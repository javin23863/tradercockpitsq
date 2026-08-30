/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.tempfiles;

import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TempFilesManager {
    private static final Logger Log = LoggerFactory.getLogger((String)"TempFilesManager");
    public static final TempFilesManager instance = new TempFilesManager();
    private static final long LIMIT_DELETE_STOCK = 43200000L;
    private static final long LIMIT_DELETE = 600000L;
    private static final long SLEEP_PERIOD = 600000L;
    private static Thread deleteThread;
    private volatile boolean destroying = false;
    private volatile boolean autoDeleteEnabled = true;

    public static TempFilesManager get() {
        return instance;
    }

    private TempFilesManager() {
    }

    public synchronized void init() {
        if (deleteThread != null) {
            throw new IllegalStateException("TempFilesManager was already initialized");
        }
        deleteThread = new Thread(new Runnable(){

            @Override
            public void run() {
                while (!TempFilesManager.this.destroying) {
                    try {
                        Thread.sleep(600000L);
                        if (!TempFilesManager.this.autoDeleteEnabled) continue;
                        TempFilesManager.this.clean();
                    }
                    catch (InterruptedException interruptedException) {}
                }
            }
        }, "TempFilesManager-thread");
        deleteThread.start();
    }

    public void setAutoDeleteEnabled(boolean bl) {
        this.autoDeleteEnabled = bl;
    }

    public void destroy() {
        this.destroying = true;
        if (deleteThread == null) {
            return;
        }
        deleteThread.interrupt();
        try {
            deleteThread.join();
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
    }

    private File getTmpRootFile() {
        return new File(MainApp.getDataPath() + "/internal/tmp");
    }

    public void clean() {
        Log.debug("Cleaning temporary files...");
        this.cleanFolder(this.getTmpRootFile(), false);
    }

    public void cleanSubFolder(String string) {
        this.cleanFolder(new File(this.getTmpRootFile(), string), true);
    }

    private void cleanFolder(File file, boolean bl) {
        Object[] objectArray;
        if (!file.exists()) {
            return;
        }
        long l = System.currentTimeMillis();
        LinkedList<Object> linkedList = new LinkedList<Object>();
        linkedList.add(file);
        LinkedList<File> linkedList2 = new LinkedList<File>();
        while (linkedList.size() != 0) {
            File file2 = (File)linkedList.remove(0);
            if (!file2.isDirectory()) continue;
            boolean bl2 = file2.getName().equals("stock");
            linkedList2.add(file2);
            objectArray = file2.listFiles();
            if (objectArray == null) continue;
            for (Object object : objectArray) {
                if (((File)object).isDirectory()) {
                    linkedList.add(object);
                    continue;
                }
                if (!bl && !this.isTooOld((File)object, l, bl2)) continue;
                try {
                    Files.deleteIfExists(((File)object).toPath());
                }
                catch (IOException iOException) {
                    Log.error("Error while deleting file: " + ((File)object).getAbsolutePath(), (Throwable)iOException);
                }
            }
        }
        for (int i = linkedList2.size() - 1; i > 0; --i) {
            File file3 = (File)linkedList2.get(i);
            objectArray = file3.list();
            if (objectArray == null || objectArray.length != 0) continue;
            try {
                Files.deleteIfExists(file3.toPath());
                continue;
            }
            catch (IOException iOException) {
                Log.error("Error while deleting folder: " + file3.getAbsolutePath(), (Throwable)iOException);
            }
        }
    }

    private boolean isTooOld(File file, long l, boolean bl) {
        long l2 = l - file.lastModified();
        if (bl) {
            return l2 >= 43200000L;
        }
        return l2 >= 600000L;
    }
}

