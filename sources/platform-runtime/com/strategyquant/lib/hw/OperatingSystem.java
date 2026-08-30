/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.hw;

import java.util.Locale;

public class OperatingSystem {
    private static String OS = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

    public boolean isWindows() {
        return OS.contains("win");
    }

    public boolean isMac() {
        return OS.contains("mac");
    }

    public boolean isUnix() {
        return OS.contains("nux");
    }

    public String toString() {
        return OS;
    }

    public String getKey() {
        if (this.isWindows()) {
            return "win";
        }
        if (this.isMac()) {
            return "mac";
        }
        if (this.isUnix()) {
            return "lin";
        }
        return OS;
    }
}

