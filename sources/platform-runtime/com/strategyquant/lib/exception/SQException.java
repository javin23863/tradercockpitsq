/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.exception;

public class SQException
extends Exception {
    public SQException() {
    }

    public SQException(String string, Throwable throwable, boolean bl, boolean bl2) {
        super(string, throwable, bl, bl2);
    }

    public SQException(String string, Throwable throwable) {
        super(string, throwable);
    }

    public SQException(String string) {
        super(string);
    }

    public SQException(Throwable throwable) {
        super(throwable);
    }
}

