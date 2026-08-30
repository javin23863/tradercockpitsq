/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.allTimeStats;

import com.strategyquant.lib.app.MainApp;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllTimeStats {
    public static final Logger Log = LoggerFactory.getLogger(AllTimeStats.class);
    private static final int MaxStats = 10;
    private ArrayList<Long> allTimeStats = new ArrayList();
    private static AllTimeStats instance;
    private static String StatsPath;
    private long strategiesGenerated = 0L;
    private long strategiesAccepted = 0L;
    private int buildTasks = 0;
    private long buildTaskStartTime = -1L;
    private long runningTime = 0L;

    private AllTimeStats() {
        for (int i = 0; i < 10; ++i) {
            this.allTimeStats.add(0L);
        }
    }

    public static AllTimeStats getInstance() {
        if (instance == null) {
            instance = new AllTimeStats();
        }
        return instance;
    }

    public long getAllTimeStat(int n) {
        return this.allTimeStats.get(n);
    }

    public void increaseStrategiesGenerated() {
        ++this.strategiesGenerated;
    }

    public void increaseStrategiesAccepted() {
        ++this.strategiesAccepted;
    }

    public long getStrategiesGenerated() {
        return this.strategiesGenerated;
    }

    public long getStrategiesAccepted() {
        return this.strategiesAccepted;
    }

    public long getRunningTime() {
        long l = this.runningTime;
        if (this.buildTaskStartTime > 0L) {
            l += (System.currentTimeMillis() - this.buildTaskStartTime) / 1000L;
        }
        return l;
    }

    public void buildTaskStart() {
        if (this.buildTasks == 0) {
            this.buildTaskStartTime = System.currentTimeMillis();
        }
        ++this.buildTasks;
    }

    public void buildTaskEnd() {
        --this.buildTasks;
        if (this.buildTasks == 0) {
            this.runningTime += (System.currentTimeMillis() - this.buildTaskStartTime) / 1000L;
            this.buildTaskStartTime = -1L;
        }
    }

    public void start() {
        this.loadStats();
    }

    public void end() {
        this.allTimeStats.set(0, this.allTimeStats.get(0) + this.strategiesGenerated);
        this.allTimeStats.set(1, this.allTimeStats.get(1) + this.strategiesAccepted);
        this.allTimeStats.set(2, this.allTimeStats.get(2) + this.runningTime);
        this.saveStats();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void saveStats() {
        FilterOutputStream filterOutputStream = null;
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(StatsPath);
            filterOutputStream = new DataOutputStream(fileOutputStream);
            for (int i = 0; i < 10; ++i) {
                ((DataOutputStream)filterOutputStream).writeLong(this.allTimeStats.get(i));
            }
            ((DataOutputStream)filterOutputStream).flush();
        }
        catch (Exception exception) {
            Log.error("Cannot save dat. Exc.", (Throwable)exception);
        }
        finally {
            try {
                if (filterOutputStream != null) {
                    filterOutputStream.close();
                }
            }
            catch (Exception exception) {}
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void loadStats() {
        FilterInputStream filterInputStream = null;
        try {
            File file = new File(StatsPath);
            if (!file.exists()) {
                return;
            }
            FileInputStream fileInputStream = new FileInputStream(StatsPath);
            filterInputStream = new DataInputStream(fileInputStream);
            for (int i = 0; i < 10; ++i) {
                this.allTimeStats.set(i, ((DataInputStream)filterInputStream).readLong());
            }
        }
        catch (Exception exception) {
            Log.error("Cannot load dat. Exc.", (Throwable)exception);
        }
        finally {
            try {
                if (filterInputStream != null) {
                    filterInputStream.close();
                }
            }
            catch (Exception exception) {}
        }
    }

    static {
        StatsPath = MainApp.getDataPath() + "/user/settings/allTimeStats.dat";
    }
}

