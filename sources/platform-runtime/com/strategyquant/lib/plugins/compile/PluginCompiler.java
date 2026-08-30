/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.plugins.compile;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.SnippetsCompilerUtils;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.jar.JarCompiler;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginCompiler
extends JarCompiler {
    public static final Logger Log = LoggerFactory.getLogger(PluginCompiler.class);

    public CompilationResult compile(String string) {
        CompilationResult compilationResult = new CompilationResult();
        try {
            File file3;
            Log.info("Compiling plugin: " + string);
            File file2 = new File(string);
            if (!file2.exists()) {
                throw new Exception(String.format("Plugin folder '%s' not found.", string));
            }
            if (!file2.isDirectory()) {
                throw new Exception(String.format("Plugin path '%s' must be a folder.", string));
            }
            ArrayList<File> arrayList = new ArrayList<File>();
            File[] fileArray = file2.listFiles();
            if (null != fileArray) {
                for (File file3 : fileArray) {
                    if (!file3.getAbsolutePath().toLowerCase().endsWith("java")) continue;
                    Log.info("File found: " + file3.getAbsolutePath());
                    arrayList.add(file3);
                }
            }
            String string2 = MainApp.getDataPath() + "user/extend/Plugins/" + file2.getName() + "/" + file2.getName() + ".jar";
            File file4 = new File(string2 + "");
            Log.info("Output jar: " + file4.getAbsolutePath());
            List<String> list = SnippetsCompilerUtils.loadDependencies();
            file3 = new File(MainApp.getDataPath() + "internal/tmp/plugin");
            file3.mkdirs();
            if (!file3.exists()) {
                throw new Exception(String.format("Output folder '%s' not found.", file3));
            }
            compilationResult = this.compile(arrayList, file3, file4, list, true, true);
            if (!compilationResult.success) {
                Log.info(String.format("Compiling plugin failed %s", compilationResult.getAsString()));
            }
            Log.info(String.format("Compilation plugin %s success.", file3.getAbsolutePath()));
        }
        catch (Error | Exception throwable) {
            Log.error("Exc.", throwable);
            compilationResult.success = false;
            compilationResult.addCompilationMessage(20, "<html><font color=\"red\"><b>Compiling plugin failed</b></font> " + throwable.getMessage());
        }
        return compilationResult;
    }
}

