/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.SnippetsUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.snippets.compile.SnippetsHashException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnippetsHash
extends HashMap<String, String> {
    public static final Logger Log = LoggerFactory.getLogger(SnippetsHash.class);

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private static String getSnippetsHashesFilePath() {
        return SQStructure.SNIPPETS_HASH_PATH;
    }

    private boolean loadFromFile() throws SnippetsHashException {
        this.clear();
        File file = new File(SnippetsHash.getSnippetsHashesFilePath());
        if (!file.exists()) {
            Log.debug("Missing snippet hashes file");
            return false;
        }
        Log.debug("Parsing snippets hashes file '{}'", (Object)file.getAbsolutePath());
        boolean bl = true;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(file), StandardCharsets.UTF_8));){
            String string;
            while ((string = bufferedReader.readLine()) != null) {
                String[] stringArray = string.split("\\|");
                if (stringArray.length != 2) {
                    bl = false;
                    break;
                }
                String string2 = stringArray[0].trim();
                String string3 = stringArray[1].trim();
                if (string2.length() == 0 || string3.length() == 0) {
                    bl = false;
                    break;
                }
                this.put(stringArray[0], stringArray[1]);
            }
        }
        catch (IOException iOException) {
            this.clear();
            throw new SnippetsHashException("Error reading snippets hashes file", iOException);
        }
        if (!bl) {
            this.clear();
            Log.debug("Invalid snippet hashes file");
        }
        return bl;
    }

    private boolean saveToFile(boolean bl) {
        try {
            File file = new File(SnippetsHash.getSnippetsHashesFilePath());
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(file, false), StandardCharsets.UTF_8));){
                TreeMap<String, String> treeMap = new TreeMap<String, String>(this);
                for (Map.Entry<String, String> entry : treeMap.entrySet()) {
                    bufferedWriter.write(entry.getKey() + "|" + entry.getValue() + "\n");
                }
            }
            return true;
        }
        catch (Exception exception) {
            if (bl) {
                Log.error("Error saving file hashes. Exc.", (Throwable)exception);
            }
            return false;
        }
    }

    public boolean checkFilesChanged(List<File> list) throws SnippetsHashException {
        if (!MainApp.runInConsole()) {
            Log.info("Checking changes in Snippets ...");
        }
        try {
            if (!this.loadFromFile()) {
                return true;
            }
            for (File object : list) {
                if (!this.checkFileChanged(object)) continue;
                return true;
            }
            ArrayList arrayList = new ArrayList();
            for (File file : list) {
                String string = SnippetsUtils.normalizePath(file.getAbsolutePath());
                arrayList.add(string);
            }
            for (String string : this.keySet()) {
                if (arrayList.contains(string)) continue;
                return true;
            }
            return false;
        }
        catch (SnippetsHashException snippetsHashException) {
            try {
                SnippetsHash.deleteFilesHashFile();
            }
            catch (SnippetsHashException snippetsHashException2) {
                snippetsHashException.addSuppressed(snippetsHashException2);
            }
            throw snippetsHashException;
        }
    }

    public static void deleteFilesHashFile() throws SnippetsHashException {
        try {
            Path path = Paths.get(SnippetsHash.getSnippetsHashesFilePath(), new String[0]);
            if (!Files.exists(path, new LinkOption[0])) {
                return;
            }
            Files.delete(path);
        }
        catch (IOException | InvalidPathException exception) {
            throw new SnippetsHashException("Error deleting hashes file", exception);
        }
    }

    public boolean saveFilesHash(List<File> list, boolean bl) {
        for (File file : list) {
            String string = SnippetsUtils.normalizePath(file.getAbsolutePath());
            String string2 = SQUtils.file2md5Hash(file);
            this.put(string, string2);
        }
        return this.saveToFile(bl);
    }

    private boolean checkFileChanged(File file) throws SnippetsHashException {
        String string;
        if (!file.exists()) {
            return true;
        }
        try {
            string = SQUtils.file2md5Hash(file);
        }
        catch (RuntimeException runtimeException) {
            throw new SnippetsHashException("Error calculating file hash", runtimeException);
        }
        String string2 = SnippetsUtils.normalizePath(file.getAbsolutePath());
        String string3 = (String)this.get(string2);
        return !string.equals(string3);
    }
}

