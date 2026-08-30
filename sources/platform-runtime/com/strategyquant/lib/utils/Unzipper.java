/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Unzipper {
    public static final Logger Log = LoggerFactory.getLogger(Unzipper.class);
    private static final int BUFFER_SIZE = 2048;
    private static final String ZIP_EXTENSION = ".zip";

    public static void unzip(String string, String string2, boolean bl) throws Exception {
        try {
            BufferedInputStream bufferedInputStream = null;
            File file = new File(string2);
            file.mkdirs();
            File file2 = new File(string);
            ZipFile zipFile = new ZipFile(file2, 1);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                int n;
                ZipEntry zipEntry = enumeration.nextElement();
                File file3 = new File(file, zipEntry.getName());
                File file4 = file3.getParentFile();
                file4.mkdirs();
                if (zipEntry.isDirectory()) continue;
                bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
                byte[] byArray = new byte[2048];
                FileOutputStream fileOutputStream = new FileOutputStream(file3);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 2048);
                while ((n = bufferedInputStream.read(byArray, 0, 2048)) != -1) {
                    bufferedOutputStream.write(byArray, 0, n);
                }
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
                if (!bl || !zipEntry.getName().toLowerCase().endsWith(ZIP_EXTENSION)) continue;
                String string3 = file.getPath() + File.separatorChar + zipEntry.getName();
                Unzipper.unzip(string3, string3.substring(0, string3.length() - ZIP_EXTENSION.length()), bl);
            }
            bufferedInputStream.close();
            zipFile.close();
        }
        catch (Exception exception) {
            throw new Exception("Unzipping file failed. Exc.", exception);
        }
    }

    public static void unzip(byte[] byArray, File file) throws Exception {
        byte[] byArray2 = new byte[1024];
        file.mkdirs();
        String string = file.getAbsolutePath();
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(byArray));
        try {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                int n;
                String string2 = zipEntry.getName();
                File file2 = new File(string + File.separator + string2);
                FileOutputStream fileOutputStream = new FileOutputStream(file2);
                while ((n = zipInputStream.read(byArray2)) > 0) {
                    fileOutputStream.write(byArray2, 0, n);
                }
                fileOutputStream.close();
            }
        }
        catch (Exception exception) {
            Log.error("Cannot unzip data. Exc.", (Throwable)exception);
            throw new Exception("Cannot unzip data. Exc.", exception);
        }
        finally {
            zipInputStream.closeEntry();
            zipInputStream.close();
        }
    }
}

