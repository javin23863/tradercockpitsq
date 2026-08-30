/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.HistoryDataNotAvailableExeption;
import com.strategyquant.lib.HistoryOHLCData;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HistoryDataLoader {
    public static final Logger Log = LoggerFactory.getLogger(HistoryDataLoader.class);

    public static synchronized HistoryOHLCData get(String string, String string2, long l, long l2, String string3) throws HistoryDataNotAvailableExeption {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("com.strategyquant.datalib.data.DataManager");
            Method method = clazz.getMethod("getHistoryData", String.class, String.class, Long.TYPE, Long.TYPE, String.class);
            HistoryOHLCData historyOHLCData = (HistoryOHLCData)method.invoke(null, string, string2, l, l2, string3);
            return historyOHLCData;
        }
        catch (ClassNotFoundException classNotFoundException) {
            throw new HistoryDataNotAvailableExeption("Call not supported. Are you using older build of SQX?");
        }
        catch (Exception exception) {
            if (exception instanceof HistoryDataNotAvailableExeption) {
                throw (HistoryDataNotAvailableExeption)exception;
            }
            throw new HistoryDataNotAvailableExeption("Exception", exception);
        }
    }
}

