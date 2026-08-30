/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.crypting.codec.binary;

import com.strategyquant.lib.crypting.codec.binary.Base64;
import com.strategyquant.lib.crypting.codec.binary.BaseNCodecInputStream;
import java.io.InputStream;

public class Base64InputStream
extends BaseNCodecInputStream {
    public Base64InputStream(InputStream inputStream) {
        this(inputStream, false);
    }

    public Base64InputStream(InputStream inputStream, boolean bl) {
        super(inputStream, new Base64(false), bl);
    }

    public Base64InputStream(InputStream inputStream, boolean bl, int n, byte[] byArray) {
        super(inputStream, new Base64(n, byArray), bl);
    }
}

