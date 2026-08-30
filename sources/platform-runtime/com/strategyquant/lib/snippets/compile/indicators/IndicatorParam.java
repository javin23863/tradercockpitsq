/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile.indicators;

public class IndicatorParam {
    private final String type;
    private final String name;

    public IndicatorParam(String string, String string2) {
        this.type = string;
        this.name = string2;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }
}

