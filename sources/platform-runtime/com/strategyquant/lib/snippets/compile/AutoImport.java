/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.compile.CompilationMessage;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.MissingImport;
import com.strategyquant.lib.snippets.compile.SQStructure;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoImport {
    public static final Logger Log = LoggerFactory.getLogger(AutoImport.class);
    private Int2ObjectOpenHashMap<ObjectArrayList<String>> availableClasses = null;

    public boolean fixImports(CompilationResult compilationResult, File file) {
        Map<String, List<MissingImport>> map;
        if (this.availableClasses == null) {
            this.availableClasses = new Int2ObjectOpenHashMap();
            this.loadAvailableClasses();
        }
        if ((map = this.searchForMissingImports(compilationResult)).isEmpty()) {
            Log.info("No missing imports found.");
        }
        for (Map.Entry<String, List<MissingImport>> entry : map.entrySet()) {
            String string = entry.getKey();
            List<MissingImport> list = entry.getValue();
            if (!string.equals(file.getAbsolutePath())) continue;
            try {
                Log.info(String.format("Fixing imports in '%s'", string));
                this.addMissingImports(string, list);
                return true;
            }
            catch (Exception exception) {
                Log.error(String.format("Error while fixing imports in '%s'. Exc.", string), (Throwable)exception);
            }
        }
        return false;
    }

    private void loadAvailableClasses() {
        String string = System.getProperty("java.class.path");
        String[] stringArray = string.split(System.getProperty("path.separator"));
        for (String string2 : stringArray) {
            if (string2.toLowerCase().endsWith(".jar")) {
                this.loadClassesFromJar(string2);
                continue;
            }
            this.loadClassesFromDirectory(new File(string2), string2);
        }
        ArrayList arrayList = new ArrayList();
        this.loadJarFilesFromDirectory(new File(SQStructure.JAVA_J32_LIB_DIR), arrayList);
        this.loadJarFilesFromDirectory(new File(SQStructure.JAVA_J64_LIB_DIR), arrayList);
        this.loadJarFilesFromDirectory(new File(SQStructure.JAVA_J32_LIB_DIR + "ext"), arrayList);
        this.loadJarFilesFromDirectory(new File(SQStructure.JAVA_J64_LIB_DIR + "ext"), arrayList);
        this.loadJarFilesFromDirectory(new File(SQStructure.PLUGINS_DIR), arrayList);
        this.loadJarFilesFromDirectory(new File(SQStructure.LIBS_DIR), arrayList);
        Iterator iterator = arrayList.iterator();
        while (iterator.hasNext()) {
            String string3 = (String)iterator.next();
            this.loadClassesFromJar(string3);
        }
    }

    private void loadClassesFromDirectory(File file, String string) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] fileArray = file.listFiles();
            if (fileArray == null) {
                return;
            }
            for (File file2 : fileArray) {
                this.loadClassesFromDirectory(file2, string);
            }
        } else {
            ObjectArrayList objectArrayList;
            if (!file.getAbsolutePath().toLowerCase().endsWith(".class")) {
                return;
            }
            String string2 = file.getAbsolutePath().replace(string, "").replace("/", ".").replace("/", ".").replace(".class", "");
            String string3 = SQUtils.getExtension(string2 = string2.replaceFirst(Pattern.quote("."), ""));
            if (this.availableClasses.containsKey(string3.hashCode())) {
                objectArrayList = (ObjectArrayList)this.availableClasses.get(string3.hashCode());
            } else {
                objectArrayList = new ObjectArrayList();
                this.availableClasses.put(string3.hashCode(), (Object)objectArrayList);
            }
            if (!objectArrayList.contains((Object)string2)) {
                objectArrayList.add((Object)string2);
            }
        }
    }

    private void loadJarFilesFromDirectory(File file, ArrayList<String> arrayList) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] fileArray = file.listFiles();
            if (fileArray == null) {
                return;
            }
            for (File file2 : fileArray) {
                this.loadJarFilesFromDirectory(file2, arrayList);
            }
        } else {
            if (!file.getAbsolutePath().toLowerCase().endsWith(".jar")) {
                return;
            }
            arrayList.add(file.getAbsolutePath());
        }
    }

    private void loadClassesFromJar(String string) {
        try {
            if (string.endsWith("SQLib.jar")) {
                return;
            }
            try (JarFile jarFile = new JarFile(string);){
                for (JarEntry jarEntry : Collections.list(jarFile.entries())) {
                    ObjectArrayList objectArrayList;
                    if (!jarEntry.getName().toLowerCase().endsWith(".class")) continue;
                    String string2 = jarEntry.getName().replace("/", ".").replace(".class", "");
                    String string3 = SQUtils.getExtension(string2);
                    if (this.availableClasses.containsKey(string3.hashCode())) {
                        objectArrayList = (ObjectArrayList)this.availableClasses.get(string3.hashCode());
                    } else {
                        objectArrayList = new ObjectArrayList();
                        this.availableClasses.put(string3.hashCode(), (Object)objectArrayList);
                    }
                    if (objectArrayList.contains((Object)string2)) continue;
                    objectArrayList.add((Object)string2);
                }
            }
        }
        catch (Exception exception) {
            Log.error(String.format("Error while loading classes from jar '%s'. Exc.", string), (Throwable)exception);
        }
    }

    private Map<String, List<MissingImport>> searchForMissingImports(CompilationResult compilationResult) {
        HashMap<String, List<MissingImport>> hashMap = new HashMap<String, List<MissingImport>>();
        try {
            for (CompilationMessage compilationMessage : compilationResult.messageList) {
                String string;
                String string2 = compilationMessage.message;
                int n = string2.indexOf("cannot find symbol");
                if (n == -1) continue;
                int n2 = string2.indexOf("location:");
                if (n2 != -1) {
                    string2 = string2.substring("cannot find symbol".length(), n2).trim();
                }
                for (int i = string2.length() - 1; i > 0; --i) {
                    if (string2.charAt(i) != ' ') continue;
                    string2 = string2.substring(i + 1);
                    break;
                }
                if (Character.isLowerCase(string2.charAt(0)) || string2.contains("(") || (string = this.findClassPathByName(string2)) == null) continue;
                if (!hashMap.containsKey(compilationMessage.sourceCodePath)) {
                    hashMap.put(compilationMessage.sourceCodePath, new ArrayList());
                } else if (!this.isNewImport(string2, (List)hashMap.get(compilationMessage.sourceCodePath))) continue;
                ((List)hashMap.get(compilationMessage.sourceCodePath)).add(new MissingImport(string2, string));
            }
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
        }
        return hashMap;
    }

    private boolean isNewImport(String string, List<MissingImport> list) {
        for (MissingImport missingImport : list) {
            if (!missingImport.getClassName().equals(string)) continue;
            return false;
        }
        return true;
    }

    private String findClassPathByName(String string) {
        ObjectArrayList objectArrayList = (ObjectArrayList)this.availableClasses.get(string.hashCode());
        if (objectArrayList == null || objectArrayList.isEmpty()) {
            return null;
        }
        for (String string2 : objectArrayList) {
            if (!string2.contains("strategyquant")) continue;
            return string2;
        }
        return (String)objectArrayList.get(0);
    }

    private void addMissingImports(String string, List<MissingImport> list) throws Exception {
        String string2 = SQUtils.fileToString(string);
        StringBuilder stringBuilder = new StringBuilder();
        if (!string2.contains("com.strategyquant.lib.*")) {
            stringBuilder.append("import com.strategyquant.lib.*;\n");
        }
        if (!string2.contains("com.strategyquant.datalib.*")) {
            stringBuilder.append("import com.strategyquant.datalib.*;\n");
        }
        if (!string2.contains("com.strategyquant.tradinglib.*")) {
            stringBuilder.append("import com.strategyquant.tradinglib.*;\n");
        }
        for (MissingImport object : list) {
            if (object.getClassPath().contains("com.strategyquant.lib") || object.getClassPath().contains("com.strategyquant.datalib") || object.getClassPath().contains("com.strategyquant.tradinglib")) continue;
            stringBuilder.append("import ").append(object.getClassPath()).append(";\n");
        }
        int n = string2.lastIndexOf("import ");
        if (n == -1) {
            n = string2.indexOf("package ");
        }
        if (n == -1) {
            string2 = stringBuilder + "\n" + string2;
        } else {
            if ((n = string2.indexOf("\n", n)) == -1) {
                n = 0;
            }
            String string3 = string2.substring(0, n).trim();
            String string4 = string2.substring(n).trim();
            string2 = string3 + "\n\n" + stringBuilder + "\n" + string4;
        }
        SQUtils.stringToFile(string, string2);
        Log.info(String.format("Added new imports to file: '%s'\n-------------------------------------------\n%s-------------------------------------------", string, stringBuilder.toString()));
    }
}

