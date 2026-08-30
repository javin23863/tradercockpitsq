/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.L88OaFjjon;

import com.strategyquant.lib.L88OaFjjon.CwkMCS2pCb;

public class WbwPVQGc2h
extends CwkMCS2pCb {
    public String getProduct() {
        return this.getString("product");
    }

    public boolean isItTrial() {
        String string = this.getString("type");
        if (string == null) {
            return false;
        }
        return string.equalsIgnoreCase("trial");
    }

    public boolean isItPartnerTrial() {
        String string = this.getString("type");
        if (string == null) {
            return false;
        }
        return string.equalsIgnoreCase("partner_trial");
    }

    public boolean isItFreeVersion() {
        String string = this.getString("type");
        if (string == null) {
            return true;
        }
        return string.equalsIgnoreCase("free");
    }

    public boolean isItFullMbg() {
        String string = this.getString("type");
        if (string == null) {
            return false;
        }
        return string.equalsIgnoreCase("full_mbg");
    }

    public boolean isPersonalized() {
        String string = this.getString("type");
        if (string == null) {
            return false;
        }
        return string.equalsIgnoreCase("full_personalized");
    }

    public boolean isSpecialTrial() {
        String string = this.getString("special_trial");
        if (string == null) {
            return false;
        }
        return string.equalsIgnoreCase("true");
    }

    public void reset() {
        this.clearAll();
    }
}

