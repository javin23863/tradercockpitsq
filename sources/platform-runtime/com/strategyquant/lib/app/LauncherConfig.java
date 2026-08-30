/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LauncherConfig {
    private static final Logger Log = LoggerFactory.getLogger(LauncherConfig.class);
    private static LauncherConfig instance = null;
    private static final String MemoryOption = "-Xmx";
    private static final String GCOption = "-XX:+Use";
    public static final int MemoryUsageAuto = 0;
    public static final int MemoryUsageFixedMaximum = 1;
    public int memoryUsage = 0;
    public int memoryInGB = 6;
    public String gcType;
    private File configFile;
    private String configText = null;

    private LauncherConfig() {
        try {
            this.configFile = this.getConfigFile();
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
        }
    }

    public static LauncherConfig getInstance() {
        if (instance == null) {
            instance = new LauncherConfig();
            if (!MainApp.checkProduct("QDM")) {
                instance.load();
            }
        }
        return instance;
    }

    public void load() {
        try {
            if (!this.configFile.exists()) {
                Log.debug("Launcher config file doesn't exist.");
                return;
            }
            this.configText = SQUtils.fileToString(this.configFile);
            String[] stringArray = this.configText.split("\n");
            for (int i = 0; i < stringArray.length; ++i) {
                try {
                    String string = stringArray[i];
                    if (string.contains(MemoryOption)) {
                        this.memoryInGB = Integer.parseInt(string.replaceAll("\\D+", ""));
                        this.memoryUsage = 1;
                    }
                    if (!string.contains(GCOption)) continue;
                    String string2 = "option -XX:+Use";
                    this.gcType = string.substring(string2.length());
                    continue;
                }
                catch (Exception exception) {
                    Log.error("Exc.", (Throwable)exception);
                }
            }
        }
        catch (Exception exception) {
            Log.error("Error while loading Launcher config. Exc.", (Throwable)exception);
        }
    }

    public void save() throws Exception {
        try {
            boolean bl = this.gcType == null || this.gcType.equals("auto");
            String string = "";
            try {
                String[] stringArray = this.configText.split("\n");
                for (int i = 0; i < stringArray.length; ++i) {
                    String string2 = stringArray[i].trim();
                    if (string2.contains(MemoryOption) || string2.contains(GCOption) || string2.trim().isEmpty()) continue;
                    string = string + string2 + "\n";
                }
            }
            catch (Exception exception) {
                Log.error("Exc.", (Throwable)exception);
            }
            if (this.memoryUsage == 1) {
                string = string + "option -Xmx" + this.memoryInGB + "g\n";
            }
            if (!bl) {
                string = string + "option -XX:+Use" + this.gcType + "\n";
            }
            SQUtils.stringToFile(this.configFile, string);
        }
        catch (Exception exception) {
            Log.error("Error while saving Launcher config. Exc.", (Throwable)exception);
            throw new Exception("Error while saving Launcher config. Exc.", exception);
        }
    }

    public File getConfigFile() throws Exception {
        return new File(MainApp.getDataPath() + "StrategyQuantX.config");
    }
}

