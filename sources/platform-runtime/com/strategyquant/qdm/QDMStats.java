/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.qdm;

import com.strategyquant.qdm.QDMDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class QDMStats {
    public static final Logger Log = LoggerFactory.getLogger(QDMStats.class);
    protected QDMDb db;

    public QDMStats(QDMDb qDMDb) {
        this.db = qDMDb;
        try {
            this.init();
        }
        catch (Exception exception) {
            Log.error("Err.", (Throwable)exception);
        }
    }

    public abstract void init() throws Exception;

    public abstract void saveStats();

    public abstract void loadStats();

    public abstract void resetStats();
}

