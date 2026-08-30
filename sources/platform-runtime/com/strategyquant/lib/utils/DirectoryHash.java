/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import com.strategyquant.lib.L;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import org.apache.commons.codec.digest.DigestUtils;

public class DirectoryHash {
    public static int calculate(File file) {
        assert (file.isDirectory());
        Vector<FileInputStream> vector = new Vector<FileInputStream>();
        DirectoryHash.collectInputStreams(file, vector);
        SequenceInputStream sequenceInputStream = new SequenceInputStream(vector.elements());
        try {
            String string = DigestUtils.md5Hex((InputStream)sequenceInputStream);
            sequenceInputStream.close();
            return string.hashCode();
        }
        catch (IOException iOException) {
            throw new RuntimeException(L.t("Error reading files to hash in %s", file.getAbsolutePath()), iOException);
        }
    }

    private static void collectInputStreams(File file, List<FileInputStream> list) {
        File[] fileArray = file.listFiles();
        if (fileArray == null) {
            return;
        }
        Arrays.sort(fileArray, new Comparator<File>(){

            @Override
            public int compare(File file, File file2) {
                return file.getName().compareTo(file2.getName());
            }
        });
        for (File file2 : fileArray) {
            if (file2.isDirectory()) {
                if (file2.getName().equals("build")) continue;
                DirectoryHash.collectInputStreams(file2, list);
                continue;
            }
            try {
                if (file2.getName().equals("filesMD5.txt")) continue;
                list.add(new FileInputStream(file2));
            }
            catch (FileNotFoundException fileNotFoundException) {
                throw new AssertionError((Object)(fileNotFoundException.getMessage() + ": file should never not be found!"));
            }
        }
    }
}

