/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.whitelabel.broker;

import com.strategyquant.lib.whitelabel.AbstractBroker;
import org.jdom2.Element;

public class RoboForexBroker
extends AbstractBroker {
    private String[] platforms = new String[]{"MT4", "MT5"};

    @Override
    public String getCode() {
        return "SQRBFX";
    }

    @Override
    public String getName() {
        return "Roboforex";
    }

    @Override
    public String getCheckString() {
        return "Robo";
    }

    @Override
    public boolean usesEAEncryption() {
        return true;
    }

    @Override
    public String getPassword() {
        return "rbfx#19-SQwl";
    }

    @Override
    public String[] getSupportedPlatforms() {
        return this.platforms;
    }

    @Override
    public void modifyOldStrategyXML(Element element) {
        Element element2 = element.getChild("StrategyRoboforex");
        if (element2 != null) {
            element2.setName("Strategy");
        }
    }

    @Override
    public int getCheckType() {
        return 1;
    }

    @Override
    public String getBannerFileName() {
        return "RoboMarkets-Logo.gif";
    }

    @Override
    public String getWelcomeSubtitle() {
        return " for Roboforex";
    }
}

