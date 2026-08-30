/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.historyData;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;

public class CdnUrlGenerator {
    private static final String PATTERN = "%token%";

    public static String generate(String string, String string2, long l) throws NoSuchAlgorithmException {
        int n = string2.indexOf(PATTERN);
        if (n == -1) {
            return string2;
        }
        String string3 = string2.substring(0, n);
        String string4 = "/" + string2.substring(n + PATTERN.length());
        String string5 = string4.substring(0, string4.lastIndexOf(47));
        String string6 = l + string5 + string;
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.update(string6.getBytes());
        String string7 = Base64.encodeBase64String((byte[])messageDigest.digest());
        String string8 = string7.replace("+", "-").replaceAll("/", "_");
        String string9 = string3 + string8 + "," + l + string4;
        return string9;
    }
}

