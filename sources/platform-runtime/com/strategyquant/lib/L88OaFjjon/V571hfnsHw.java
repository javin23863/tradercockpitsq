/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.L88OaFjjon;

import com.strategyquant.lib.L88OaFjjon.DcOjahK4ng;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import java.util.concurrent.TimeUnit;

public class V571hfnsHw {
    private String product;
    private String type;
    private int edition;
    private String ownerName;
    private String ownerEmail;
    private String code;
    private String validUntil;
    private int validUntilSec;
    private String hardwareID;
    private boolean verified;
    private String dataTrialExpires;
    private boolean specialTrial;
    private AbstractBroker broker = null;
    public static String reasonVerifFailed = null;
    public static String codeVerifFailed = null;

    public V571hfnsHw(String string, String string2, int n, String string3, String string4, String string5, String string6, int n2, String string7, AbstractBroker abstractBroker, boolean bl, String string8, boolean bl2) {
        this.product = string;
        this.type = string2;
        this.edition = n;
        this.ownerName = string3;
        this.ownerEmail = string4;
        this.code = string5;
        this.validUntil = string6;
        this.validUntilSec = n2;
        this.hardwareID = string7;
        this.broker = abstractBroker;
        this.verified = bl;
        this.dataTrialExpires = string8;
        this.specialTrial = bl2;
    }

    public String wLtxAZeo8B() {
        return this.product;
    }

    public int c858EfPQC3() {
        return this.edition;
    }

    public String ljYePnIhcR() {
        return this.type;
    }

    public String oOkoIeUsMG() {
        return this.ownerName;
    }

    public String fSECzwVwpK() {
        return this.ownerEmail;
    }

    public String eHvc2JguAd() {
        return this.code;
    }

    public String cGFrCLK8fN() {
        return this.validUntil;
    }

    public int aIpB57erT2() {
        return this.validUntilSec;
    }

    public String dsarewvFF() {
        return this.dataTrialExpires;
    }

    public boolean dataTrialExpiresSoon() {
        String string = this.dsarewvFF();
        if (string != null) {
            long l = System.currentTimeMillis();
            long l2 = SQTime.parseToMilis(string, "yyyy-MM-dd HH:mm:ss");
            if (l2 > l && l2 - l < TimeUnit.DAYS.toMillis(7L)) {
                return true;
            }
        }
        return false;
    }

    public boolean bDm88fRJA3() {
        return this.type != null && (this.type.equals(DcOjahK4ng.TYPE_TRIAL) || this.type.equals(DcOjahK4ng.TYPE_PARTNER_TRIAL)) && this.isSpecialTrial();
    }

    public String nmFllxIfvN() {
        return this.hardwareID;
    }

    public boolean yIifbIrWD3() {
        return this.type == null || this.type.equals(DcOjahK4ng.TYPE_TRIAL) || this.type.equals(DcOjahK4ng.TYPE_PARTNER_TRIAL);
    }

    public boolean cJMHUhmpvk() {
        return this.type == null || this.type.equals(DcOjahK4ng.TYPE_PARTNER_TRIAL);
    }

    public boolean n2mCQaU80Z() {
        return this.type == null || this.type.equals(DcOjahK4ng.TYPE_FREE);
    }

    public boolean tsPZHq7yG3() {
        return this.type != null && this.verified && (this.type.equals(DcOjahK4ng.TYPE_FULL_MBG) || this.type.equals(DcOjahK4ng.TYPE_FULL_SUBSCRIPTION) || this.type.equals(DcOjahK4ng.TYPE_FULL) || this.type.equals(DcOjahK4ng.TYPE_FULL_PERSONALIZED));
    }

    public boolean w49uJpREZO() {
        return this.type != null && this.verified && (this.type.equals(DcOjahK4ng.TYPE_FULL_MBG) || this.type.equals(DcOjahK4ng.TYPE_FULL_SUBSCRIPTION));
    }

    public boolean rbaiBK0Thx() {
        return this.type != null && this.verified && this.type.equals(DcOjahK4ng.TYPE_FULL_PERSONALIZED);
    }

    public boolean aVwpAOwrF1() {
        return this.type.equals(DcOjahK4ng.TYPE_FULL_PERSONALIZED);
    }

    public boolean gDmtOfRJBr() {
        return this.edition == DcOjahK4ng.EDITION_PROFESIONAL || this.edition == DcOjahK4ng.EDITION_ULTIMATE;
    }

    public boolean aDm88fRJB2() {
        return this.edition == DcOjahK4ng.EDITION_ULTIMATE;
    }

    public boolean a1wUchdumV() {
        return this.code.startsWith(DcOjahK4ng.FutlabLicense);
    }

    public boolean xpoHYYsX() {
        return !this.yIifbIrWD3();
    }

    public String printType(String string) {
        String string2 = "";
        string2 = this.type.equals(DcOjahK4ng.TYPE_FREE) ? string2 + "(Free license)" : (this.type.equals(DcOjahK4ng.TYPE_TRIAL) ? (this.isSpecialTrial() ? string2 + "(Special Trial license) - valid until " + this.validUntil : (string.startsWith(DcOjahK4ng.FutlabLicense) ? string2 + "(Futlab license) - valid until " + this.validUntil : string2 + "(Trial license) - valid until " + this.validUntil)) : (this.type.equals(DcOjahK4ng.TYPE_PARTNER_TRIAL) ? string2 + "(Partner Evaluation license) - valid until " + this.validUntil : string2 + "(Full license)"));
        return string2;
    }

    public String printEdition() {
        if (this.edition == DcOjahK4ng.EDITION_STARTER) {
            return "Starter";
        }
        if (this.edition == DcOjahK4ng.EDITION_PROFESIONAL) {
            return "Pro";
        }
        if (this.edition == DcOjahK4ng.EDITION_ULTIMATE) {
            return "Ultimate";
        }
        return "NA";
    }

    public boolean gWfGtoRYJG() {
        return this.verified;
    }

    public boolean aJg1L21A0t() {
        return this.broker != null;
    }

    public AbstractBroker inygRmSMx7() {
        return this.broker;
    }

    public boolean isSpecialTrial() {
        return this.specialTrial;
    }
}

