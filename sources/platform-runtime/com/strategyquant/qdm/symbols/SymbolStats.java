/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.qdm.symbols;

public class SymbolStats {
    public String symbol;
    public int source;
    public int dataType;
    public boolean cdnUsed = false;
    public int downloadedYears = 0;

    public SymbolStats() {
    }

    public SymbolStats(String string, int n, int n2, boolean bl, int n3) {
        this.symbol = string;
        this.source = n;
        this.dataType = n2;
        this.cdnUsed = bl;
        this.downloadedYears = n3;
    }
}

