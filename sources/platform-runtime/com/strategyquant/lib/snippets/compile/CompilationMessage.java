/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import java.io.Serializable;

public class CompilationMessage
implements Serializable {
    public static final int MESSAGE_TYPE_NORMAL = 0;
    public static final int MESSAGE_TYPE_INFO = 10;
    public static final int MESSAGE_TYPE_ERROR = 20;
    public String sourceCodePath = null;
    public int type = 0;
    public String message = null;
    public Long line = null;
    public Long column = null;
    public String clazz = null;
    public String kind = null;

    public CompilationMessage() {
    }

    public CompilationMessage(String string, int n, String string2) {
        this.sourceCodePath = string;
        this.type = n;
        this.message = string2;
    }
}

