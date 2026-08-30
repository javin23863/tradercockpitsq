/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app;

import com.strategyquant.lib.app.AppSplash;
import com.strategyquant.lib.app.MainApp;
import java.awt.Color;
import java.awt.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQSplashScreen {
    public static final Logger Log = LoggerFactory.getLogger((String)"SQSplashScreen");
    public static final String SQL_RECENT_MAX_VALUE = "splashScreen";
    private AppSplash splash;
    private static final int X = 20;
    private static final int W = 420;
    private int TEXT_H = 20;
    private int BAR_W = 4;
    private int BAR_H = 8;
    private int MAX_BARS = 103;
    private int textY;
    private int barY;
    private int barPos = 0;
    private int value = 0;
    private int maxValue = -1;
    private int step = -1;
    private Color fontColor = new Color(130, 130, 130);
    private Color progressBarColor = new Color(33, 152, 246);
    private Font progressFont;

    public SQSplashScreen() {
        this.splash = new AppSplash();
        try {
            Thread.sleep(100L);
        }
        catch (InterruptedException interruptedException) {
            Log.debug("Interrupted during sleep");
            Thread.currentThread().interrupt();
        }
    }

    public void hide() {
        this.splash.dispose();
    }

    public int getValue() {
        return this.value;
    }

    public int getMaxValueFromDB() {
        try {
            String string = MainApp.settings().get(MainApp.getProduct() + "_" + SQL_RECENT_MAX_VALUE);
            return Integer.parseInt(string);
        }
        catch (Exception exception) {
            return -1;
        }
    }

    public static void setMaxValueToDB() {
        try {
            SQSplashScreen sQSplashScreen = MainApp.getSplashScreen();
            if (sQSplashScreen != null) {
                int n = sQSplashScreen.getValue();
                MainApp.settings().set(MainApp.getProduct() + "_" + SQL_RECENT_MAX_VALUE, n + "");
            }
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
        }
    }

    public void drawSplashProgress(String string) {
    }
}

