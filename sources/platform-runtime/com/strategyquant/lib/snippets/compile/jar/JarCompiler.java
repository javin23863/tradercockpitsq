/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.jar;

import com.strategyquant.lib.L;
import com.strategyquant.lib.snippets.compile.CompilationMessage;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.CompilationUtils;
import com.strategyquant.lib.snippets.compile.jar.CustomClassloaderJavaFileManagerJ10;
import com.strategyquant.lib.snippets.compile.jar.SecuredClassLoder;
import com.strategyquant.lib.utils.Zipper;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JarCompiler {
    public static final Logger Log = LoggerFactory.getLogger(JarCompiler.class);

    protected CompilationResult compile(List<File> list, File file, File file2, List<String> list2, boolean bl, boolean bl2) {
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
            if (!file.exists()) {
                file.mkdirs();
            }
            FileUtils.cleanDirectory((File)file);
            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add("-d");
            arrayList.add(file.getPath());
            arrayList.add("-Xdiags:verbose");
            arrayList.add("-Xlint:none");
            String string = CompilationUtils.dependenciesToString(list2);
            if (string != null && !string.equals("")) {
                arrayList.addAll(Arrays.asList("-classpath", string));
            }
            if (Boolean.TRUE.equals((compilationTask = javaCompiler.getTask(null, new CustomClassloaderJavaFileManagerJ10(new SecuredClassLoder(), standardJavaFileManager = javaCompiler.getStandardFileManager(diagnosticCollector = new DiagnosticCollector(), Locale.ENGLISH, StandardCharsets.UTF_8)), diagnosticCollector, arrayList, null, iterable = standardJavaFileManager.getJavaFileObjectsFromFiles(list))).call())) {
                compilationResult.success = true;
                if (bl2) {
                    compilationResult.addCompilationMessage(0, "<html><font color=\"green\"><b>" + L.t("Plugin compilation successful, changes will be visible after application restart", new Object[0]) + "</b></font>, 0 error(s)");
                } else {
                    compilationResult.addCompilationMessage(0, "<html><font color=\"green\"><b>" + L.t("Snippet(s) compilation successful", new Object[0]) + "</b></font>, 0 error(s)");
                }
                try {
                    if (bl || bl2) {
                        Zipper.zipDir(file, file2);
                    }
                    Zipper.addFilesToExistingZip(file, file2);
                }
                catch (Exception exception) {
                    Log.error("Exc.", (Throwable)exception);
                    throw new Exception("Failed to create JAR file.");
                }
            } else {
                compilationResult.success = false;
                compilationResult.addCompilationMessage(20, String.format("<html><font color=\"red\" size=\"2\"><b>%s</b></font></html>", L.t("Note - Compile all failed - NO snippets were updated. Please fix the errors or compile the files you want to update individually.", new Object[0])));
                ArrayList<CompilationMessage> arrayList2 = new ArrayList<CompilationMessage>();
                int n = 0;
                for (Diagnostic diagnostic : diagnosticCollector.getDiagnostics()) {
                    CompilationMessage compilationMessage = new CompilationMessage();
                    if (diagnostic.getSource() != null) {
                        String string2 = ((JavaFileObject)diagnostic.getSource()).getName();
                        File file3 = new File(string2);
                        compilationMessage.sourceCodePath = string2;
                        compilationMessage.clazz = file3.getName();
                    }
                    compilationMessage.type = 20;
                    compilationMessage.line = diagnostic.getLineNumber();
                    compilationMessage.column = diagnostic.getColumnNumber();
                    compilationMessage.message = StringEscapeUtils.escapeHtml4((String)diagnostic.getMessage(null));
                    compilationMessage.kind = this.getType(diagnostic.getKind());
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        ++n;
                    }
                    arrayList2.add(compilationMessage);
                }
                compilationResult.messageList.addAll(arrayList2);
                compilationResult.addCompilationMessage(0, String.format("<html><font color=\"red\"><b>%s</b></font>, " + n + " %s</html>", L.t("Compilation failed", new Object[0]), L.t("error(s)", new Object[0])));
            }
            standardJavaFileManager.close();
        }
        catch (Error | Exception throwable) {
            Log.error("Exc.", throwable);
            compilationResult.success = false;
            compilationResult.addCompilationMessage(20, String.format("<html><font color=\"red\"><b>%s</b></font> %s", L.t("Compilation failed", new Object[0]), throwable.getMessage()));
        }
        return compilationResult;
    }

    private String getType(Diagnostic.Kind kind) {
        switch (kind) {
            case ERROR: {
                return L.t("Error", new Object[0]);
            }
            case WARNING: {
                return L.t("Warning", new Object[0]);
            }
            case MANDATORY_WARNING: {
                return L.t("Mandatory Warning", new Object[0]);
            }
            case NOTE: {
                return L.t("Note", new Object[0]);
            }
            case OTHER: {
                return L.t("Other", new Object[0]);
            }
        }
        return null;
    }
}

