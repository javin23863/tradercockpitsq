/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GCChecker {
    private static final Logger Log = LoggerFactory.getLogger((String)"GCChecker");

    public static boolean checkG1GCUsed() {
        if (GCChecker.getJavaVersion() >= 10) {
            List<GarbageCollectorMXBean> list = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean garbageCollectorMXBean : list) {
                if (!garbageCollectorMXBean.getName().contains("G1")) continue;
                return true;
            }
            return false;
        }
        return true;
    }

    private static int getJavaVersion() {
        String string = System.getProperty("java.version");
        try {
            return Integer.parseInt(string.split("\\.")[string.startsWith("1.") ? 1 : 0]);
        }
        catch (Exception exception) {
            Log.error("Cannot recognize Java version. Assuming Java 8...", (Throwable)exception);
            return 8;
        }
    }
}

