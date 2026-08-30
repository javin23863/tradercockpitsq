/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.hw;

import com.strategyquant.lib.hw.HardwareInfoLin;
import com.strategyquant.lib.hw.HardwareInfoMac;
import com.strategyquant.lib.hw.HardwareInfoWin;
import com.strategyquant.lib.hw.OperatingSystem;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;

public class HardwareInfo {
    public static final Logger Log = LoggerFactory.getLogger((String)"SQLicenser");
    private static HardwareInfoWin hwWin = new HardwareInfoWin();
    private static HardwareInfoLin hwLin = new HardwareInfoLin();
    private static HardwareInfoMac hwMac = new HardwareInfoMac();
    private static OperatingSystem os = new OperatingSystem();
    private static SystemInfo si = new SystemInfo();

    public static String getDiskSN() {
        if (os.isWindows()) {
            return hwWin.getDiskSN();
        }
        if (os.isUnix()) {
            return hwLin.getDiskSN();
        }
        if (os.isMac()) {
            return hwMac.getDiskSN();
        }
        return "";
    }

    public static long getMachineGuid() {
        if (os.isWindows()) {
            return hwWin.getMachineGuid();
        }
        if (os.isUnix()) {
            return hwLin.getMachineGuid();
        }
        if (os.isMac()) {
            return hwMac.getMachineGuid();
        }
        return -1L;
    }

    public static String getMacAddress() {
        if (os.isWindows()) {
            return hwWin.getMacAddress();
        }
        if (os.isUnix()) {
            return hwLin.getMacAddress();
        }
        if (os.isMac()) {
            return hwMac.getMacAddress();
        }
        return "";
    }

    public static long getTotalMemoryInGb() {
        GlobalMemory globalMemory = si.getHardware().getMemory();
        return globalMemory.getTotal() / 1024L / 1024L / 1024L;
    }

    public static String getId() {
        try {
            String string = si.getHardware().getComputerSystem().getBaseboard().getSerialNumber();
            String string2 = si.getHardware().getComputerSystem().getSerialNumber();
            String string3 = "NA";
            try {
                string3 = si.getHardware().getProcessor().getProcessorIdentifier().getProcessorID();
            }
            catch (Error | Exception throwable) {
                Log.debug("HW error: " + throwable.getMessage(), throwable);
            }
            return string + "#" + string2 + "#" + string3;
        }
        catch (Error | Exception throwable) {
            Log.error("HW error: " + throwable.getMessage(), throwable);
            return "error:" + throwable.getMessage();
        }
    }

    public static String getInfo() {
        try {
            List list = si.getHardware().getDisplays();
            String string = (list.isEmpty() ? "<NA>" : ((Display)list.get(0)).toString()).replace("\n", ", ");
            String string2 = si.getHardware().getProcessor().toString().replace("\n", ", ");
            double d = Runtime.getRuntime().maxMemory() / 1024L / 1024L / 1024L;
            return "os: " + si.getOperatingSystem().toString() + " | cpu: " + string2 + " | ram: " + si.getHardware().getMemory().toString() + " | display: " + string + " | JVM max heap size: " + d + "GB";
        }
        catch (Error | Exception throwable) {
            Log.error("Cannot get HW info.", throwable);
            return os.getKey();
        }
    }

    public static String getOSKey() {
        return os.getKey();
    }
}

