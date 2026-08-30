/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TimePeriod;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimePeriods
extends Long2ObjectAVLTreeMap<TimePeriod> {
    public static final Logger Log = LoggerFactory.getLogger((String)"TimePeriods");

    public void print() {
        Log.info("Timeperiod - --------------------");
        for (Long2ObjectMap.Entry entry : this.long2ObjectEntrySet()) {
            TimePeriod timePeriod = (TimePeriod)entry.getValue();
            Log.info("From :" + SQTime.toDateMinuteString(timePeriod.from) + "    " + SQTime.toDateMinuteString(timePeriod.to));
        }
    }

    public TimePeriods clone() {
        TimePeriods timePeriods = new TimePeriods();
        for (Long2ObjectMap.Entry entry : this.long2ObjectEntrySet()) {
            timePeriods.put(entry.getLongKey(), ((TimePeriod)entry.getValue()).clone());
        }
        return timePeriods;
    }
}

