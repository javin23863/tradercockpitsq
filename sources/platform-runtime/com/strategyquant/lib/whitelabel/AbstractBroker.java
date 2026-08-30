/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.whitelabel;

import org.jdom2.Element;

public abstract class AbstractBroker {
    public static final String PLATFORM_MT4 = "MT4";
    public static final String PLATFORM_MT5 = "MT5";
    public static final String PLATFORM_TS = "TS";
    public static final int CHECK_NONE = -1;
    public static final int CHECK_COMPANY = 1;
    public static final int CHECK_SERVER = 2;

    public abstract String getCode();

    public abstract String getName();

    public abstract int getCheckType();

    public abstract String getCheckString();

    public abstract String[] getSupportedPlatforms();

    public abstract boolean usesEAEncryption();

    public abstract String getPassword();

    public abstract void modifyOldStrategyXML(Element var1);

    public abstract String getBannerFileName();

    public String getWelcomeTitle() {
        return "Welcome to StrategyQuant X";
    }

    public String getWelcomeSubtitle() {
        return "";
    }

    public String getDescription() {
        return "Jumpstart your journey to becoming a successful algo trader.<br><br>With our powerful strategy development and research platform that uses machine learning techniques and genetic programming, you can now easily generate your own automated trading systems for any market or timeframe.<br><br>Build new trading strategies, test for quality and take your trading to new heights today!";
    }

    public String getFirstTimeLabel() {
        return "<b>New to StrategyQuant X?</b>";
    }

    public String getStartButtonTitle() {
        return "Generate your own strategies";
    }

    public String getStartButtonSubtitle() {
        return "with the default settings";
    }
}

