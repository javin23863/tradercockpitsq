/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.hw;

import com.strategyquant.lib.SQUtils;
import java.io.File;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.OSFileStore;

public class HardwareInfoLin {
    public static final Logger Log = LoggerFactory.getLogger(HardwareInfoLin.class);

    public String getDiskSN() {
        try {
            String string = "";
            SystemInfo systemInfo = new SystemInfo();
            List list = systemInfo.getOperatingSystem().getFileSystem().getFileStores();
            for (OSFileStore oSFileStore : list) {
                String string2 = oSFileStore.getMount();
                if (string2 == null || !string2.equals("/")) continue;
                string = oSFileStore.getUUID();
                break;
            }
            return string;
        }
        catch (Error | Exception throwable) {
            Log.error("Failed to get the Disk SN. Exc.", throwable);
            return "";
        }
    }

    public String getMacAddress() {
        try {
            String string = null;
            HashMap<String, String> hashMap = new HashMap<String, String>();
            Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
            while (enumeration.hasMoreElements()) {
                NetworkInterface networkInterface = enumeration.nextElement();
                byte[] byArray = networkInterface.getHardwareAddress();
                if (byArray == null) continue;
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < byArray.length; ++i) {
                    stringBuilder.append(String.format("%02X%s", byArray[i], i < byArray.length - 1 ? "-" : ""));
                }
                if (!stringBuilder.toString().isEmpty()) {
                    hashMap.put(networkInterface.getName(), stringBuilder.toString());
                }
                if (stringBuilder.toString().isEmpty() || string != null) continue;
                string = networkInterface.getName();
            }
            if (string != null) {
                return (String)hashMap.get(string);
            }
        }
        catch (Exception exception) {
            Log.error("Failed to get the Mac Address. Exc.", (Throwable)exception);
        }
        return "N/A (0)";
    }

    public long getMachineGuid() {
        try {
            String string = SQUtils.fileToString(new File("/var/lib/dbus/machine-id")).trim();
            return string.hashCode();
        }
        catch (Exception exception) {
            Log.error("Failed to get date id. Exc.", (Throwable)exception);
            return -1L;
        }
    }
}

