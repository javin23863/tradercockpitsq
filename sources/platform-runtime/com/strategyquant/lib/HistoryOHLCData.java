/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

public class HistoryOHLCData {
    public final String Symbol;
    public final String Timeframe;
    public final long DateFrom;
    public final long DateTo;
    public final String Session;
    public long[] Time;
    public float[] Open;
    public float[] High;
    public float[] Low;
    public float[] Close;
    public float[] Volume;

    public HistoryOHLCData(String string, String string2, long l, long l2, String string3) {
        this.Symbol = string;
        this.Timeframe = string2;
        this.DateFrom = l;
        this.DateTo = l2;
        this.Session = string3;
    }
}

