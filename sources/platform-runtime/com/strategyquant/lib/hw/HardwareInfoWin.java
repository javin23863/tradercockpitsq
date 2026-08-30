/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.hw;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardwareInfoWin {
    public static final Logger Log = LoggerFactory.getLogger(HardwareInfoWin.class);

    public long getMachineGuid() {
        try {
            String string = Advapi32Util.registryGetStringValue((WinReg.HKEY)WinReg.HKEY_LOCAL_MACHINE, (String)"SOFTWARE\\Microsoft\\Cryptography", (String)"MachineGuid");
            return string.hashCode();
        }
        catch (Error error) {
            Log.error("Failed to get date id. Exc.", (Throwable)error);
            return -1L;
        }
        catch (Exception exception) {
            Log.error("Failed to get date id. Exc.", (Throwable)exception);
            return -1L;
        }
    }

    public String getDiskSN() {
        try {
            String string;
            String string2 = "";
            File file = File.createTempFile("realhowto", ".vbs");
            file.deleteOnExit();
            FileWriter fileWriter = new FileWriter(file);
            String string3 = "Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\nSet colDrives = objFSO.Drives\nSet objDrive = colDrives.item(\"C\")\nWscript.Echo objDrive.SerialNumber";
            fileWriter.write(string3);
            fileWriter.close();
            Process process = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((string = bufferedReader.readLine()) != null) {
                string2 = string2 + string;
            }
            bufferedReader.close();
            if (string2.length() > 30) {
                string2 = string2.substring(0, 30);
            }
            return (string2 = string2.replaceAll("[^-0-9]+", "").trim()).isEmpty() ? "-1" : string2;
        }
        catch (Exception exception) {
            Log.error("Failed to get the Disk SN. Exc.", (Throwable)exception);
            return "";
        }
    }

    public String getMacAddress() {
        String string = this.findMacByScript();
        if (string.equals("") || string.equals("N/A") || string.length() < 6) {
            string = this.findMacByJava();
        }
        return string;
    }

    private String findMacByJava() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            if (inetAddress == null) {
                return "N/A (1)";
            }
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
            if (networkInterface == null) {
                return "N/A (2)";
            }
            byte[] byArray = networkInterface.getHardwareAddress();
            if (byArray == null) {
                return "N/A (3)";
            }
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < byArray.length; ++i) {
                stringBuilder.append(String.format("%02X%s", byArray[i], i < byArray.length - 1 ? "-" : ""));
            }
            return stringBuilder.toString();
        }
        catch (Exception exception) {
            return "N/A (4)";
        }
    }

    private String findMacByScript() {
        try {
            String string;
            String string2 = "";
            Process process = Runtime.getRuntime().exec("getmac /FO csv /V /nh");
            DataInputStream dataInputStream = new DataInputStream(process.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)dataInputStream, StandardCharsets.UTF_8));
            while ((string = bufferedReader.readLine()) != null) {
                String[] stringArray = string.split(",");
                if (stringArray.length != 4) continue;
                String string3 = stringArray[1].replace('\"', ' ').trim();
                String string4 = stringArray[2].replace('\"', ' ').trim();
                if (string4.contains("00-00-00-00")) continue;
                string2 = string4;
                if (string3.contains("luetoot")) continue;
                break;
            }
            process.destroy();
            process.waitFor();
            return string2.trim();
        }
        catch (Exception exception) {
            Log.error("Failed to get the Mac Address. Exc.", (Throwable)exception);
            return "N/A (0)";
        }
    }
}

