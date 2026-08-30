/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets;

import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomClassesLoaderCache {
    public static final Logger Log = LoggerFactory.getLogger(CustomClassesLoaderCache.class);
    private static CustomClassesLoaderCache instance = null;
    private static final Object lock = new Object();
    private final ArrayList<String> allClasses = new ArrayList();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static CustomClassesLoaderCache getInstance() {
        Object object = lock;
        synchronized (object) {
            if (instance == null) {
                instance = new CustomClassesLoaderCache();
            }
            return instance;
        }
    }

    private CustomClassesLoaderCache() {
        this.reload();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<String> getClasses(String string) {
        Object object = lock;
        synchronized (object) {
            ArrayList<String> arrayList = new ArrayList<String>();
            for (String string2 : this.allClasses) {
                if (!string2.startsWith(string)) continue;
                arrayList.add(string2);
            }
            return arrayList;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void reload() {
        Object object = lock;
        synchronized (object) {
            try {
                this.allClasses.clear();
                File file = new File(SQStructure.SNIPPETS_JAR_PATH);
                if (!file.exists()) {
                    Log.debug(String.format("File '%s' doesn't exist.", file.getAbsolutePath()));
                    return;
                }
                try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(file));){
                    ZipEntry zipEntry;
                    while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                        String string = zipEntry.getName();
                        String string2 = string.replace("/", ".").replace(".class", "");
                        if (string2.contains("$") || string2.endsWith("Abstract")) continue;
                        this.allClasses.add(string2);
                    }
                }
                Collections.sort(this.allClasses);
            }
            catch (Exception exception) {
                Log.error("Exc.", (Throwable)exception);
            }
        }
    }
}

