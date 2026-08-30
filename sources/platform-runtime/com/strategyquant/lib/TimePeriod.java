/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

public class TimePeriod {
    public long from;
    public long to;
    public double value = 0.0;
    public String name;
    public String note;
    public byte type;

    public TimePeriod() {
    }

    public TimePeriod(long l, long l2) {
        this.from = l;
        this.to = l2;
    }

    public TimePeriod(long l, long l2, String string) {
        this.from = l;
        this.to = l2;
        this.name = string;
    }

    public TimePeriod(TimePeriod timePeriod) {
        this.from = timePeriod.from;
        this.to = timePeriod.to;
    }

    public TimePeriod clone() {
        TimePeriod timePeriod = new TimePeriod();
        timePeriod.from = this.from;
        timePeriod.to = this.to;
        timePeriod.value = this.value;
        timePeriod.name = this.name;
        timePeriod.note = this.note;
        return timePeriod;
    }
}

