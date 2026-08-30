/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.memory;

import com.strategyquant.lib.memory.SQMemoryProtectionError;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryUsageChecker {
    public static final Logger Log = LoggerFactory.getLogger(MemoryUsageChecker.class);
    private static final MemoryMXBean MemoryMXBean = ManagementFactory.getMemoryMXBean();
    private static final int oneTimeMemoryUsageLimit = 90;
    private static final int memoryUsagePctLimit = 85;
    private static final int memoryOverLimitCount = 15;
    private static int overLimitCount = 0;
    private static double curUsagePct = 0.0;
    private static Thread measuringThread = null;

    public static void init() {
        if (measuringThread == null) {
            measuringThread = new Thread(){

                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(1000L);
                        }
                        catch (Exception exception) {
                            // empty catch block
                        }
                        if (!MemoryUsageChecker.checkMemoryUsage()) {
                            overLimitCount++;
                            Log.debug("Exceeded max memory usage " + overLimitCount + " times");
                            continue;
                        }
                        overLimitCount = 0;
                    }
                }
            };
            measuringThread.start();
        }
    }

    public static void checkAvailableMemory() {
        if (overLimitCount > 15 || curUsagePct > 90.0) {
            String string = String.format("Memory usage is too high (%d%%). Please increase available memory or reduce memory usage", (int)curUsagePct);
            Log.error("Memory usage limit reached. Current usage: " + curUsagePct + "%");
            throw new SQMemoryProtectionError(string);
        }
    }

    public static double getCurrentUsage() {
        return curUsagePct;
    }

    private static boolean checkMemoryUsage() {
        MemoryUsage memoryUsage = MemoryMXBean.getHeapMemoryUsage();
        curUsagePct = 100.0 * (double)memoryUsage.getUsed() / (double)memoryUsage.getMax();
        return curUsagePct < 85.0;
    }
}

