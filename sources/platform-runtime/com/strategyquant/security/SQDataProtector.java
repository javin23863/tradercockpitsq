/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.security;

import com.strategyquant.security.IDataProtector;

public class SQDataProtector
implements IDataProtector {
    @Override
    public String encryptBase64(String string, String string2) throws Exception {
        return string;
    }

    @Override
    public byte[] encryptBase64(byte[] byArray, String string) throws Exception {
        return byArray;
    }

    @Override
    public String decryptBase64(String string, String string2) throws Exception {
        return string;
    }

    @Override
    public byte[] decryptBase64(byte[] byArray, String string) throws Exception {
        return byArray;
    }

    @Override
    public boolean isEncrypted(String string) {
        return false;
    }

    @Override
    public boolean isEncrypted(byte[] byArray) {
        return false;
    }
}

