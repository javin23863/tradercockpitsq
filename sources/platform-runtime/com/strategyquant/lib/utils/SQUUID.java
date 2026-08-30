/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.security.SecureRandom;

public class SQUUID {
    private static ThreadLocal<SecureRandom> uuidGenerator = ThreadLocal.withInitial(() -> new SecureRandom());

    public static String randomUUID() {
        int n;
        byte[] byArray = new byte[16];
        uuidGenerator.get().nextBytes(byArray);
        byArray[6] = (byte)(byArray[6] & 0xF);
        byArray[6] = (byte)(byArray[6] | 0x40);
        byArray[8] = (byte)(byArray[8] & 0x3F);
        byArray[8] = (byte)(byArray[8] | 0x80);
        long l = 0L;
        long l2 = 0L;
        assert (byArray.length == 16) : "data must be 16 bytes in length";
        for (n = 0; n < 8; ++n) {
            l = l << 8 | (long)(byArray[n] & 0xFF);
        }
        for (n = 8; n < 16; ++n) {
            l2 = l2 << 8 | (long)(byArray[n] & 0xFF);
        }
        String string = SQUUID.digits(l >> 32, 8) + "-" + SQUUID.digits(l >> 16, 4) + "-" + SQUUID.digits(l, 4) + "-" + SQUUID.digits(l2 >> 48, 4) + "-" + SQUUID.digits(l2, 12);
        return string;
    }

    private static String digits(long l, int n) {
        long l2 = 1L << n * 4;
        return Long.toHexString(l2 | l & l2 - 1L).substring(1);
    }
}

