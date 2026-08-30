/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.ProtectedSnippetsException;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectedSnippets {
    public static final Logger Log = LoggerFactory.getLogger(ProtectedSnippets.class);
    private static final Object lock = new Object();
    private static final String JAR_PATH = MainApp.getDataPath() + "internal/internal.dat";
    private final ArrayList<String> list = new ArrayList();
    private static ProtectedSnippets instance;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static ProtectedSnippets getInstance() {
        Object object = lock;
        synchronized (object) {
            if (instance == null) {
                instance = new ProtectedSnippets();
            }
            return instance;
        }
    }

    private ProtectedSnippets() {
        try {
            this.load();
        }
        catch (Exception exception) {
            Log.error("Could not load list of protected snippets. Exc.", (Throwable)exception);
        }
        try {
            ProtectedSnippets.recover();
            Log.debug("Separating user files from builtin...");
            String string = SQStructure.getExtendUserDirPath();
            String string2 = SQStructure.getExtendBuiltinDirPath();
            if (!this.list.isEmpty()) {
                this.separateUserFilesFromBuiltin(string, string2, string2);
            } else {
                Log.debug("No protected files found.");
            }
        }
        catch (Exception exception) {
            Log.error("Failed to separate user files from builtin. Exc.", (Throwable)exception);
        }
    }

    private void separateUserFilesFromBuiltin(String string, String string2, String string3) throws IOException {
        File[] fileArray;
        File file = new File(string3);
        File file2 = null;
        for (File file3 : fileArray = file.listFiles()) {
            try {
                if (file3.isFile()) {
                    if (this.isProtected(file3) || (file2 = new File(string, SQUtils.trimFilePath(file3.getAbsolutePath(), SQStructure.EXTEND_DIR_PATH))).exists()) continue;
                    if (!file2.getParentFile().exists()) {
                        file2.getParentFile().mkdirs();
                    }
                    FileUtils.moveFile((File)file3, (File)file2);
                    Log.info(String.format("File '%s' moved from Builtin to User folder", file3.getAbsolutePath()));
                    continue;
                }
                if (!file3.isDirectory()) continue;
                this.separateUserFilesFromBuiltin(string, string2, file3.getAbsolutePath());
            }
            catch (Exception exception) {
                Log.error(String.format("Failed to move file from Builtin to User folder '%s' -> '%s'. Exc.", file3.getAbsolutePath(), file2 == null ? "NULL" : file2.getAbsolutePath()), (Throwable)exception);
            }
        }
    }

    public void load() throws ProtectedSnippetsException {
        block9: {
            Log.debug("Loading list of protected snippets...");
            File file = new File(JAR_PATH);
            if (file.exists()) {
                try (ZipFile zipFile = new ZipFile(file, 1);){
                    Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                    while (enumeration.hasMoreElements()) {
                        ZipEntry zipEntry = enumeration.nextElement();
                        if (zipEntry.isDirectory()) continue;
                        this.list.add(zipEntry.getName());
                    }
                    break block9;
                }
                catch (IOException iOException) {
                    throw new ProtectedSnippetsException("Error reading protected snippets", iOException);
                }
            }
            Log.debug("No protected snippets found.");
        }
    }

    public boolean isProtected(File file) {
        String string = SQUtils.trimFilePath(file.getAbsolutePath(), SQStructure.EXTEND_DIR_PATH);
        if (string.equals("Snippets/SQ/Internal/Indicators.java")) {
            return true;
        }
        Log.debug("Protected file: " + string);
        return this.list.contains(string);
    }

    public static void recover() {
        Log.debug("Recovering protected snippets...");
        try {
            File file = new File(JAR_PATH);
            if (file.exists()) {
                ProtectedSnippets.unzipOnlySnippetsDir(JAR_PATH, SQStructure.EXTEND_DIR_PATH, false);
                Log.debug("Protected snippets reloaded successfully.");
            } else {
                Log.debug("No protected snippets found.");
            }
        }
        catch (Exception exception) {
            Log.error("Cannot reload protected snippets. Exc.", (Throwable)exception);
        }
    }

    public static void unzipOnlySnippetsDir(String string, String string2, boolean bl) throws Exception {
        try {
            int n = 2048;
            String string3 = ".zip";
            BufferedInputStream bufferedInputStream = null;
            File file = new File(string2);
            file.mkdirs();
            File file2 = new File(string);
            ZipFile zipFile = new ZipFile(file2, 1);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                int n2;
                ZipEntry zipEntry = enumeration.nextElement();
                if (!zipEntry.getName().startsWith("Snippets")) continue;
                File file3 = new File(file, zipEntry.getName());
                File file4 = file3.getParentFile();
                file4.mkdirs();
                if (zipEntry.isDirectory()) continue;
                bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                byte[] byArray = new byte[n];
                FileOutputStream fileOutputStream = new FileOutputStream(file3);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, n);
                while ((n2 = bufferedInputStream.read(byArray, 0, n)) != -1) {
                    bufferedOutputStream.write(byArray, 0, n2);
                }
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
                if (!bl || !zipEntry.getName().toLowerCase().endsWith(string3)) continue;
                String string4 = file.getPath() + File.separatorChar + zipEntry.getName();
                ProtectedSnippets.unzipOnlySnippetsDir(string4, string4.substring(0, string4.length() - string3.length()), bl);
            }
            bufferedInputStream.close();
            zipFile.close();
        }
        catch (Exception exception) {
            throw new Exception("Unzipping file failed. Exc.", exception);
        }
    }
}

