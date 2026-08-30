/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import com.strategyquant.lib.SQUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class FileLoader {
    protected String dirPath;
    protected ArrayList<File> fileList;
    protected Iterator<File> iterator;
    protected String extensions = null;

    public FileLoader(String string, String string2) {
        this.dirPath = string;
        this.extensions = string2;
        this.fileList = new ArrayList();
        File file = new File(string);
        if (file.exists()) {
            File[] fileArray;
            for (File file2 : fileArray = file.listFiles()) {
                if (file2.isDirectory() || !this.checkExtension(SQUtils.getExtension(file2.getName()))) continue;
                this.fileList.add(file2);
            }
        }
        this.iterator = this.fileList.listIterator();
    }

    private boolean checkExtension(String string) {
        if (this.extensions == null) {
            return true;
        }
        string = string.toLowerCase();
        String string2 = "," + this.extensions.replace(" ", "") + ",";
        return string2.contains("," + string + ",");
    }

    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    public File getNext() {
        return this.iterator.next();
    }
}

