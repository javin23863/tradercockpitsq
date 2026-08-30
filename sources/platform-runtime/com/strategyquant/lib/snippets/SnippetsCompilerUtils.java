/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SnippetsCompilerUtils {
    private SnippetsCompilerUtils() {
    }

    public static List<String> loadDependencies() {
        ArrayList<String> arrayList = new ArrayList<String>();
        SnippetsCompilerUtils.loadJarDependencies(SQStructure.PLUGINS_DIR, arrayList);
        SnippetsCompilerUtils.loadJarDependencies(SQStructure.LIBS_DIR, arrayList);
        SnippetsCompilerUtils.loadJarDependencies(SQStructure.USER_LIBS, arrayList);
        if (MainApp.is32BitVersion()) {
            SnippetsCompilerUtils.loadJarDependencies(SQStructure.JAVA_J32, arrayList);
        } else {
            SnippetsCompilerUtils.loadJarDependencies(SQStructure.JAVA_J64, arrayList);
        }
        if (!MainApp.isRelease()) {
            SnippetsCompilerUtils.loadBinDirectories(SQStructure.SQ4_PROJECTS_DIR, arrayList);
            SnippetsCompilerUtils.loadBinDirectories(SQStructure.SQ4_LIBS_DIR, arrayList);
        }
        return arrayList;
    }

    public static void loadJarDependencies(String string, List<String> list) {
        File file = new File(string);
        if (file.exists()) {
            File[] fileArray = file.listFiles();
            if (fileArray == null) {
                return;
            }
            for (File file2 : fileArray) {
                String string2 = file2.getAbsolutePath();
                if (file2.isDirectory()) {
                    SnippetsCompilerUtils.loadJarDependencies(string2, list);
                    continue;
                }
                if (!string2.toLowerCase().endsWith("jar")) continue;
                list.add(string2);
            }
        }
    }

    public static void loadJarFileDependencies(String string, List<File> list) {
        File file = new File(string);
        if (file.exists()) {
            File[] fileArray = file.listFiles();
            if (fileArray == null) {
                return;
            }
            for (File file2 : fileArray) {
                String string2 = file2.getAbsolutePath();
                if (file2.isDirectory()) {
                    SnippetsCompilerUtils.loadJarFileDependencies(string2, list);
                    continue;
                }
                if (!string2.toLowerCase().endsWith("jar")) continue;
                list.add(file2);
            }
        }
    }

    public static void loadBinDirectories(String string, List<String> list) {
        if (string == null) {
            return;
        }
        File file = new File(string);
        if (file.exists()) {
            File[] fileArray = file.listFiles();
            if (fileArray == null) {
                return;
            }
            for (File file2 : fileArray) {
                if (!file2.isDirectory()) continue;
                SnippetsCompilerUtils.addBinDirDependency(file2, list);
            }
        }
    }

    private static void addBinDirDependency(File file, List<String> list) {
        String string = file.getAbsolutePath() + "/bin";
        File file2 = new File(string);
        if (file2.exists() && file2.isDirectory()) {
            list.add(file2.getAbsolutePath());
        }
    }
}

