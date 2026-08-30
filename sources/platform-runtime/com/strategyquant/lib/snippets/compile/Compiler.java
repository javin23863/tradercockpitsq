/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.L;
import com.strategyquant.lib.snippets.compile.CompilationMessage;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.CompilationUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.snippets.compile.SourceFile;
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

public class Compiler {
    public static final Logger Log = LoggerFactory.getLogger(Compiler.class);
    private final ArrayList<String> compilationOptions;
    private final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

    public Compiler(List<String> list) throws Exception {
        if (this.javaCompiler == null) {
            throw new Exception("Java SE Development Kit (JDK) was not found on this computer. To download the JDK visit http://docs.oracle.com/javase");
        }
        this.compilationOptions = new ArrayList();
        this.compilationOptions.addAll(Arrays.asList("-d", SQStructure.COMPILED_DIR_PATH_WITHOUT_PACKAGE));
        String string = CompilationUtils.dependenciesToString(list);
        if (string != null && !string.equals("")) {
            this.compilationOptions.addAll(Arrays.asList("-classpath", string));
        }
    }

    public CompilationResult compile(SourceFile sourceFile) {
        CompilationResult compilationResult = new CompilationResult();
        compilationResult.sourceFile = sourceFile;
        try {
            DiagnosticCollector diagnosticCollector;
            JavaCompiler.CompilationTask compilationTask;
            compilationResult.sourceCodePath = sourceFile.codeFile.getAbsolutePath();
            compilationResult.logTabTitle = "Log - " + sourceFile.codeFile.getName();
            compilationResult.addCompilationMessage(0, "Compiling " + sourceFile.codeFile.getName());
            ArrayList<File> arrayList = new ArrayList<File>();
            arrayList.add(new File(sourceFile.codeFile.getAbsolutePath()));
            StandardJavaFileManager standardJavaFileManager = this.javaCompiler.getStandardFileManager(null, null, null);
            Iterable<? extends JavaFileObject> iterable = standardJavaFileManager.getJavaFileObjectsFromFiles(arrayList);
            File file = new File(SQStructure.COMPILED_DIR_PATH_WITHOUT_PACKAGE);
            if (!file.exists()) {
                file.mkdirs();
            }
            if (Boolean.TRUE.equals((compilationTask = this.javaCompiler.getTask(null, standardJavaFileManager, diagnosticCollector = new DiagnosticCollector(), this.compilationOptions, null, iterable)).call())) {
                compilationResult.success = true;
                compilationResult.addCompilationMessage(0, "<html><font color=\"green\"><b>" + L.t("Compilation successful, changes will be visible after application restart", new Object[0]) + "</b></font>, 0 error(s)");
            } else {
                compilationResult.success = false;
                ArrayList<CompilationMessage> arrayList2 = new ArrayList<CompilationMessage>();
                for (Diagnostic diagnostic : diagnosticCollector.getDiagnostics()) {
                    CompilationMessage compilationMessage = new CompilationMessage();
                    compilationMessage.sourceCodePath = sourceFile.codeFile.getAbsolutePath();
                    compilationMessage.type = 20;
                    compilationMessage.message = diagnostic.getMessage(null);
                    compilationMessage.line = diagnostic.getLineNumber();
                    compilationMessage.column = diagnostic.getColumnNumber();
                    arrayList2.add(compilationMessage);
                }
                compilationResult.messageList.addAll(arrayList2);
                compilationResult.addCompilationMessage(0, "<html><font color=\"red\"><b>Compilation failed</b></font>, " + arrayList2.size() + " error(s)</html>");
            }
            standardJavaFileManager.close();
        }
        catch (Error | Exception throwable) {
            compilationResult.success = false;
            compilationResult.addCompilationMessage(0, "<html><font color=\"red\"><b>Compilation failed</b></font> " + throwable.getMessage());
        }
        return compilationResult;
    }
}

