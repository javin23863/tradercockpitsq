/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app;

public class LicenseInfo {
    public String errorMsg = null;
    public int errorCode = -1;
    public String lc = null;
    public volatile boolean licenseChecked = false;

    public void setError(String string) {
        this.errorMsg = string;
    }

    public void setErrorCode(int n) {
        this.errorCode = n;
    }

    public void setLicense(String string) {
        this.lc = string;
    }
}

