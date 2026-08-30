/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.security;

public interface IDataProtector {
    public String encryptBase64(String var1, String var2) throws Exception;

    public byte[] encryptBase64(byte[] var1, String var2) throws Exception;

    public String decryptBase64(String var1, String var2) throws Exception;

    public byte[] decryptBase64(byte[] var1, String var2) throws Exception;

    public boolean isEncrypted(String var1);

    public boolean isEncrypted(byte[] var1);
}

