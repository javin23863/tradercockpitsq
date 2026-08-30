/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Zipper {
    public static void zipDir(File file, File file2) throws Exception {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            Zipper._zipDir(file, zipOutputStream);
            zipOutputStream.close();
            fileOutputStream.close();
        }
        catch (Exception exception) {
            throw new Exception("Failed to create a .zip file. File=" + file2.getAbsolutePath() + "\nExc.", exception);
        }
    }

    private static void _zipDir(File file, ZipOutputStream zipOutputStream) throws IOException {
        for (File file2 : file.listFiles()) {
            Zipper.addFolderToZip("", file2.getAbsolutePath(), zipOutputStream);
        }
    }

    private static void addFolderToZip(String string, String string2, ZipOutputStream zipOutputStream) throws IOException {
        File file = new File(string2);
        if (file.list().length == 0) {
            Zipper.addFileToZip(string, string2, zipOutputStream, true);
        } else {
            for (String string3 : file.list()) {
                if (string.equals("")) {
                    Zipper.addFileToZip(file.getName(), string2 + "/" + string3, zipOutputStream, false);
                    continue;
                }
                Zipper.addFileToZip(string + "/" + file.getName(), string2 + "/" + string3, zipOutputStream, false);
            }
        }
    }

    private static void addFileToZip(String string, String string2, ZipOutputStream zipOutputStream, boolean bl) throws IOException {
        File file = new File(string2);
        if (bl) {
            zipOutputStream.putNextEntry(new ZipEntry(string + "/" + file.getName() + "/"));
        } else if (file.isDirectory()) {
            Zipper.addFolderToZip(string, string2, zipOutputStream);
        } else {
            int n;
            byte[] byArray = new byte[1024];
            FileInputStream fileInputStream = new FileInputStream(string2);
            zipOutputStream.putNextEntry(new ZipEntry(string + "/" + file.getName()));
            while ((n = fileInputStream.read(byArray)) > 0) {
                zipOutputStream.write(byArray, 0, n);
            }
            fileInputStream.close();
        }
    }

    public static void addFilesToExistingZip(File file, File file2) throws Exception {
        try {
            File file3 = new File(MainApp.getDataPath() + "/internal/tmp/tmp987654321.zip");
            FileUtils.copyFile((File)file2, (File)file3);
            ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file2));
            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file3));
            List<File> list = Zipper.listFiles(file);
            File[] fileArray = list.toArray(new File[list.size()]);
            ZipEntry zipEntry = null;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                String string = zipEntry.getName();
                boolean bl = false;
                for (int i = 0; i < fileArray.length; ++i) {
                    if (!string.equalsIgnoreCase(SQUtils.trimFilePath(fileArray[i].getAbsolutePath(), file.getAbsolutePath() + "/"))) continue;
                    bl = true;
                    break;
                }
                if (bl) continue;
                zipOutputStream.putNextEntry(new ZipEntry(string));
                IOUtils.copy((InputStream)zipInputStream, (OutputStream)zipOutputStream);
            }
            for (int i = 0; i < fileArray.length; ++i) {
                int n;
                String string = SQUtils.trimFilePath(fileArray[i].getAbsolutePath(), file.getAbsolutePath() + "/");
                byte[] byArray = new byte[1024];
                FileInputStream fileInputStream = new FileInputStream(fileArray[i]);
                zipOutputStream.putNextEntry(new ZipEntry(string));
                while ((n = fileInputStream.read(byArray)) > 0) {
                    zipOutputStream.write(byArray, 0, n);
                }
                fileInputStream.close();
            }
            zipInputStream.close();
            zipOutputStream.close();
        }
        catch (Exception exception) {
            throw new Exception("Failed to add files to existing .zip file. File=" + file2.getAbsolutePath() + "\nExc.", exception);
        }
    }

    private static List<File> listFiles(File file) {
        ArrayList<File> arrayList = new ArrayList<File>();
        Zipper._listFiles(file, arrayList);
        return arrayList;
    }

    private static void _listFiles(File file, ArrayList<File> arrayList) {
        if (file.exists()) {
            File[] fileArray;
            for (File file2 : fileArray = file.listFiles()) {
                if (file2.isDirectory()) {
                    Zipper._listFiles(file2, arrayList);
                    continue;
                }
                arrayList.add(file2);
            }
        }
    }
}

