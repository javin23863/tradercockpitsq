/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.memory;

import com.strategyquant.lib.app.MainApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;

public class CpuInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(CpuInfo.class);

    public static int getAvailableProcessors() {
        Object object;
        int n = -1;
        int n2 = -1;
        try {
            object = new SystemInfo();
            HardwareAbstractionLayer hardwareAbstractionLayer = object.getHardware();
            CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
            n = centralProcessor.getLogicalProcessorCount();
        }
        catch (Error | Exception throwable) {
            LOGGER.error("Failed to get cores, using SystemInfo. Exc.", throwable);
        }
        try {
            object = Runtime.getRuntime();
            n2 = ((Runtime)object).availableProcessors();
        }
        catch (Error | Exception throwable) {
            LOGGER.error("Failed to get cores, using Java Runtime. Exc.", throwable);
        }
        int n3 = Math.max(n, n2);
        if (!MainApp.runInConsole()) {
            LOGGER.info(String.format("CPU cores detected %d, SysInfo=%d / JavaRuntime=%d", n3, n, n2));
        }
        return n3;
    }
}

