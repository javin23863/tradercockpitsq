/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.crypting;

import com.strategyquant.lib.crypting.codec.binary.Base64;
import java.nio.charset.StandardCharsets;

public class SQDecoderEncoder {
    public static byte[] decode(String string) {
        return Base64.decodeBase64(string);
    }

    public static byte[] encode(byte[] byArray) {
        return Base64.encodeBase64(byArray);
    }

    public static String encode2String(byte[] byArray) {
        return new String(Base64.encodeBase64(byArray), StandardCharsets.UTF_8);
    }
}

