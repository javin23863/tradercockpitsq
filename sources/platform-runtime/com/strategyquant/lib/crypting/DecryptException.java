/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.crypting;

public class DecryptException
extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DecryptException() {
    }

    public DecryptException(String string, Throwable throwable, boolean bl, boolean bl2) {
        super(string, throwable, bl, bl2);
    }

    public DecryptException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public DecryptException(String string) {
        super(string);
    }

    public DecryptException(Throwable throwable) {
        super(throwable);
    }
}

