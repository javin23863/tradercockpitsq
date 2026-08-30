/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.qdm;

import com.strategyquant.lib.db.DbBase;
import com.strategyquant.lib.snippets.compile.SQStructure;

public class QDMDb
extends DbBase {
    public QDMDb() {
        super("QDMDb", "act.dat", SQStructure.INTERNAL_DIR_PATH + "web/QDM");
        this.initDatabase();
    }

    @Override
    protected void initDatabase() {
        if (this.isDbExist()) {
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            Log.debug("Database created successfully");
        }
        catch (Exception exception) {
            Log.error("DB error:", (Throwable)exception);
        }
    }
}

