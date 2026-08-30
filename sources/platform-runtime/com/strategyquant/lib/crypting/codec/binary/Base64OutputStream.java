/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.crypting.codec.binary;

import com.strategyquant.lib.crypting.codec.binary.Base64;
import com.strategyquant.lib.crypting.codec.binary.BaseNCodecOutputStream;
import java.io.OutputStream;

public class Base64OutputStream
extends BaseNCodecOutputStream {
    public Base64OutputStream(OutputStream outputStream) {
        this(outputStream, true);
    }

    public Base64OutputStream(OutputStream outputStream, boolean bl) {
        super(outputStream, new Base64(false), bl);
    }

    public Base64OutputStream(OutputStream outputStream, boolean bl, int n, byte[] byArray) {
        super(outputStream, new Base64(n, byArray), bl);
    }
}

