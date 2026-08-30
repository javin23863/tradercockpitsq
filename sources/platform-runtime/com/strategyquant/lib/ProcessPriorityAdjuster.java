/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.hw.OperatingSystem;
import java.io.IOException;
import org.jutils.jprocesses.JProcesses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;

public class ProcessPriorityAdjuster {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPriorityAdjuster.class);

    public void adjustPriorityToHigh() {
        long l = this.getCurrentPID();
        LOGGER.info("Changing SQ process({}) to high priority", (Object)l);
        try {
            this.setProcessHighPriority((int)l);
        }
        catch (IOException iOException) {
            LOGGER.error("Error while adjusting process priority", (Throwable)iOException);
        }
    }

    private int getCurrentPID() {
        SystemInfo systemInfo = new SystemInfo();
        oshi.software.os.OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        OSProcess oSProcess = operatingSystem.getProcess(operatingSystem.getProcessId());
        return oSProcess.getProcessID();
    }

    public void setProcessHighPriority(int n) throws IOException {
        OperatingSystem operatingSystem = new OperatingSystem();
        boolean bl = false;
        if (operatingSystem.isWindows()) {
            bl = JProcesses.changePriority((int)n, (int)128).isSuccess();
        } else if (operatingSystem.isMac() || operatingSystem.isUnix()) {
            bl = JProcesses.changePriority((int)n, (int)-10).isSuccess();
        } else {
            throw new UnsupportedOperationException("Unsupported OS: " + operatingSystem.toString());
        }
        if (bl) {
            LOGGER.info("Changing the priority was successful.");
        } else {
            LOGGER.info("Changing the priority failed");
        }
    }
}

