/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.inMemory;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.compile.CompilationMessage;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.CompilationUtils;
import com.strategyquant.lib.snippets.compile.jar.CustomClassloaderJavaFileManagerJ10;
import com.strategyquant.lib.snippets.compile.jar.SecuredClassLoder;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryJavaCompiler {
    public static final Logger Log = LoggerFactory.getLogger(InMemoryJavaCompiler.class);

    public CompilationResult compile(List<File> list, List<String> list2) {
        CompilationResult compilationResult = new CompilationResult();
        try {
            Iterable<? extends JavaFileObject> iterable;
            DiagnosticCollector diagnosticCollector;
            StandardJavaFileManager standardJavaFileManager;
            JavaCompiler.CompilationTask compilationTask;
            JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
            if (javaCompiler == null) {
                throw new Exception("Java SE Development Kit (JDK) was not found on this computer. To download the JDK visit http://docs.oracle.com/javase");
            }
            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add("-d");
            arrayList.add(MainApp.getDataPath() + "/internal/tmp/compiled");
            arrayList.add("-Xdiags:verbose");
            String string = CompilationUtils.dependenciesToString(list2);
            if (string != null && !string.equals("")) {
                arrayList.addAll(Arrays.asList("-classpath", string));
            }
            if (Boolean.TRUE.equals((compilationTask = javaCompiler.getTask(null, new CustomClassloaderJavaFileManagerJ10(new SecuredClassLoder(), standardJavaFileManager = javaCompiler.getStandardFileManager(null, null, null)), diagnosticCollector = new DiagnosticCollector(), arrayList, null, iterable = standardJavaFileManager.getJavaFileObjectsFromFiles(list))).call())) {
                compilationResult.success = true;
                compilationResult.addCompilationMessage(0, "<html><font color=\"green\"><b>" + L.t("Compilation successful, changes will be visible after application restart", new Object[0]) + "</b></font>, 0 error(s)");
            } else {
                compilationResult.success = false;
                ArrayList<CompilationMessage> arrayList2 = new ArrayList<CompilationMessage>();
                for (Diagnostic diagnostic : diagnosticCollector.getDiagnostics()) {
                    CompilationMessage compilationMessage = new CompilationMessage();
                    if (diagnostic.getSource() != null) {
                        String string2 = ((JavaFileObject)diagnostic.getSource()).getName();
                        File file = new File(string2);
                        compilationMessage.sourceCodePath = string2;
                        compilationMessage.clazz = file.getName();
                    }
                    compilationMessage.type = 20;
                    compilationMessage.line = diagnostic.getLineNumber();
                    compilationMessage.column = diagnostic.getColumnNumber();
                    compilationMessage.message = diagnostic.getMessage(null);
                    arrayList2.add(compilationMessage);
                }
                compilationResult.messageList.addAll(arrayList2);
                compilationResult.addCompilationMessage(0, "<html><font color=\"red\"><b>Compilation failed</b></font>, " + arrayList2.size() + " error(s)</html>");
            }
            standardJavaFileManager.close();
        }
        catch (Error | Exception throwable) {
            Log.error("Exc.", throwable);
            compilationResult.success = false;
            compilationResult.addCompilationMessage(20, "<html><font color=\"red\"><b>Compilation failed</b></font> " + throwable.getMessage());
        }
        return compilationResult;
    }
}

