/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.whitelabel.broker;

import com.strategyquant.lib.whitelabel.AbstractBroker;
import org.jdom2.Element;

public class BlackwellBroker
extends AbstractBroker {
    private String[] platforms = new String[]{"MT4", "MT5"};

    @Override
    public String getCode() {
        return "SQBLWL";
    }

    @Override
    public String getName() {
        return "Blackwell";
    }

    @Override
    public String getCheckString() {
        return "BlackwellGlobal";
    }

    @Override
    public boolean usesEAEncryption() {
        return true;
    }

    @Override
    public String getPassword() {
        return "blwl#19-SQwl";
    }

    @Override
    public String[] getSupportedPlatforms() {
        return this.platforms;
    }

    @Override
    public void modifyOldStrategyXML(Element element) {
        Element element2 = element.getChild("StrategyBlackwell");
        if (element2 != null) {
            element2.setName("Strategy");
        }
    }

    @Override
    public int getCheckType() {
        return 2;
    }

    @Override
    public String getBannerFileName() {
        return "Blackwell-Logo.png";
    }
}

