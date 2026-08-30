/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.updates;

import com.strategyquant.lib.app.MainApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQUpdaterRunner {
    public static final Logger Log = LoggerFactory.getLogger((String)"UpdaterRunner");

    public static void run(String string, String string2) {
        try {
            if (MainApp.isRelease()) {
                String string3 = "";
                if (MainApp.is32BitVersion()) {
                    string3 = "_32";
                }
                String string4 = "Updater" + string3 + ".exe -p " + string + " -u " + string2;
                Log.info("Starting Updater (1) [" + string4 + "]");
                Runtime.getRuntime().exec(string4);
            } else {
                String string5 = "java -splash:internal/web/Updater/updater_splash.png -jar Updater.jar -p " + string + " -u " + string2;
                Log.info("Starting Updater (2) [" + string5 + "]");
                Runtime.getRuntime().exec(string5);
            }
            Log.info("Updater started");
        }
        catch (Exception exception) {
            Log.error("Error while launching Updater. Exc.", (Throwable)exception);
        }
    }
}

