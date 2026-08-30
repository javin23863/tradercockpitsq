/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.ProtectedSnippets;
import com.strategyquant.lib.snippets.SnippetsCompilerUtils;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.snippets.compile.SnippetsHash;
import com.strategyquant.lib.snippets.compile.SnippetsHashException;
import com.strategyquant.lib.snippets.compile.jar.JarCompiler;
import com.strategyquant.lib.time.SQTimeOld;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnippetsCompiler
extends JarCompiler {
    public static final Logger Log = LoggerFactory.getLogger(SnippetsCompiler.class);
    private final SnippetsHash snippetsHash;
    private static SnippetsCompiler instance;
    private CompilationResult lastCompilationResult = null;
    private ArrayList<String> internalBlockClassNames = new ArrayList();

    public CompilationResult getLastCompilationResult() {
        return this.lastCompilationResult;
    }

    public SnippetsCompiler() {
        this.snippetsHash = new SnippetsHash();
    }

    public static synchronized SnippetsCompiler getInstance() {
        if (instance == null) {
            instance = new SnippetsCompiler();
        }
        return instance;
    }

    public CompilationResult run(boolean bl) {
        return this.run(null, bl);
    }

    public synchronized CompilationResult run(List<File> list, boolean bl) {
        long l = System.currentTimeMillis();
        if (!MainApp.runInConsole() || MainApp.isBacktestNode()) {
            Log.info("Starting to compile Snippets");
        }
        CompilationResult compilationResult = this.iRun(list, bl);
        long l2 = System.currentTimeMillis();
        long l3 = l2 - l;
        String string = SQTimeOld.formatDateTime((int)(l3 / 1000L));
        if (!MainApp.runInConsole() || MainApp.isBacktestNode()) {
            Log.info("Compiling Snippets done in {}", (Object)string);
        }
        this.lastCompilationResult = compilationResult;
        return compilationResult;
    }

    public boolean isInternalSnippet(String string) {
        return this.internalBlockClassNames.contains(string);
    }

    private CompilationResult iRun(List<File> list, boolean bl) {
        File file;
        ProtectedSnippets.recover();
        boolean bl2 = false;
        if (list == null) {
            bl2 = true;
            list = this.getSources();
            if (!bl) {
                boolean bl3;
                file = new File(SQStructure.SNIPPETS_JAR_PATH);
                try {
                    bl3 = !file.exists() || this.snippetsHash.checkFilesChanged(list);
                }
                catch (SnippetsHashException snippetsHashException) {
                    Log.error("Error calculating compilation file hashes", (Throwable)snippetsHashException);
                    bl3 = true;
                }
                if (!bl3) {
                    if (!MainApp.runInConsole() || MainApp.isBacktestNode()) {
                        Log.info("Files not changed. Skipping compiling Snippets...");
                    }
                    CompilationResult compilationResult = new CompilationResult();
                    compilationResult.success = true;
                    compilationResult.addCompilationMessage(0, "<html><font color=\"green\"><b>" + L.t("Files not changed. Skipping compiling Snippets...", new Object[0]) + "</b></font>");
                    compilationResult.setRecompiled(false);
                    return compilationResult;
                }
            }
        }
        file = new File(SQStructure.getProgramDirPath() + "internal/tmp/compiled");
        File file2 = new File(SQStructure.SNIPPETS_JAR_PATH);
        List<String> list2 = SnippetsCompilerUtils.loadDependencies();
        CompilationResult compilationResult = this.compile(list, file, file2, list2, bl2, false);
        if (compilationResult.success) {
            compilationResult.setRecompiled(true);
            Log.debug("Snippets compilation SUCCESS");
            Log.debug("Saving Snippets hash.");
            this.snippetsHash.saveFilesHash(list, true);
        } else {
            Log.error("Compilation FAILED. Reason:\n{}", (Object)compilationResult.getAsSimpleString());
        }
        return compilationResult;
    }

    private List<File> getSources() {
        String[] stringArray;
        ArrayList<File> arrayList = new ArrayList<File>();
        for (String string : stringArray = SQStructure.getSnippetsSourceDirs()) {
            File file = new File(string);
            boolean bl = string.equals(SQStructure.getSnippetsBuiltinDirPath());
            if (!file.exists() || !file.isDirectory()) continue;
            this.searchForSources(file, arrayList, bl);
        }
        return arrayList;
    }

    private void searchForSources(File file, List<File> list, boolean bl) {
        File[] fileArray = file.listFiles();
        if (fileArray == null) {
            return;
        }
        for (File file2 : fileArray) {
            String string = file2.getAbsolutePath();
            if (file2.isDirectory()) {
                this.searchForSources(file2, list, bl);
                continue;
            }
            if (!string.toLowerCase().endsWith(".java")) continue;
            list.add(file2);
            if (!bl) continue;
            String string2 = file2.getAbsolutePath().replace("\\", "/");
            int n = string2.indexOf("/Snippets/SQ/");
            String string3 = SQUtils.stripExtension(string2.substring(n + 10)).replace("/", ".");
            this.internalBlockClassNames.add(string3);
        }
    }
}

