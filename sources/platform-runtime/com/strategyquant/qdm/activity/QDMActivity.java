/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.qdm.activity;

import com.strategyquant.lib.SQTime;
import com.strategyquant.qdm.QDMDb;
import com.strategyquant.qdm.QDMStats;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QDMActivity
extends QDMStats {
    public static final Logger Log = LoggerFactory.getLogger(QDMActivity.class);
    private static final int MaxStats = 40;
    private ArrayList<Integer> stats = new ArrayList();
    private long date = -1L;

    public QDMActivity(QDMDb qDMDb) {
        super(qDMDb);
        this.initStats();
    }

    private void initStats() {
        this.stats.clear();
        for (int i = 0; i < 40; ++i) {
            this.stats.add(0);
        }
    }

    public void increase(int n) {
        this.increase(n, 1);
    }

    public void increase(int n, int n2) {
        if (n < 0 || n >= 40) {
            Log.error("Cannot increase dat. Index '" + n + "'out of range.");
            return;
        }
        this.stats.set(n, this.stats.get(n) + n2);
    }

    public long getDateOfLastStats() {
        if (this.date == -1L) {
            return this.date;
        }
        return SQTime.getDateInMs(this.date);
    }

    @Override
    public void init() throws Exception {
        String string = "CREATE TABLE IF NOT EXISTS activity(`date_inserted` INTEGER NOT NULL,`v0` int(10) DEFAULT '0',`v1` int(10) DEFAULT '0',`v2` int(10) DEFAULT '0',`v3` int(10) DEFAULT '0',`v4` int(10) DEFAULT '0',`v5` int(10) DEFAULT '0',`v6` int(10) DEFAULT '0',`v7` int(10) DEFAULT '0',`v8` int(10) DEFAULT '0',`v9` int(10) DEFAULT '0',`v10` int(10) DEFAULT '0',`v11` int(10) DEFAULT '0',`v12` int(10) DEFAULT '0',`v13` int(10) DEFAULT '0',`v14` int(10) DEFAULT '0',`v15` int(10) DEFAULT '0',`v16` int(10) DEFAULT '0',`v17` int(10) DEFAULT '0',`v18` int(10) DEFAULT '0',`v19` int(10) DEFAULT '0',`v20` int(10) DEFAULT '0',`v21` int(10) DEFAULT '0',`v22` int(10) DEFAULT '0',`v23` int(10) DEFAULT '0',`v24` int(10) DEFAULT '0',`v25` int(10) DEFAULT '0',`v26` int(10) DEFAULT '0',`v27` int(10) DEFAULT '0',`v28` int(10) DEFAULT '0',`v29` int(10) DEFAULT '0',`v30` int(10) DEFAULT '0',`v31` int(10) DEFAULT '0',`v32` int(10) DEFAULT '0',`v33` int(10) DEFAULT '0',`v34` int(10) DEFAULT '0',`v35` int(10) DEFAULT '0',`v36` int(10) DEFAULT '0',`v37` int(10) DEFAULT '0',`v38` int(10) DEFAULT '0',`v39` int(10) DEFAULT '0')";
        this.db.sqlCommand(string);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void saveStats() {
        this.cleanDb();
        Connection connection = null;
        try {
            connection = this.db.getConnection();
            String string = "INSERT INTO activity (date_inserted, ";
            for (int i = 0; i < 40; ++i) {
                string = string + (i == 39 ? "v" + i : "v" + i + ",");
            }
            long l = SQTime.getDateInMs(System.currentTimeMillis());
            string = string + ") VALUES (" + l + ", ";
            for (int i = 0; i < 40; ++i) {
                string = string + (i == 39 ? (Serializable)this.stats.get(i) : this.stats.get(i) + ",");
            }
            string = string + ")";
            this.db.sqlCommand(connection, string);
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
        }
        finally {
            this.db.close(connection);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void loadStats() {
        block7: {
            Connection connection = null;
            Statement statement = null;
            ResultSet resultSet = null;
            try {
                connection = this.db.getConnection();
                String string = "SELECT * FROM activity";
                statement = connection.createStatement();
                resultSet = statement.executeQuery(string);
                if (resultSet.next()) {
                    this.date = resultSet.getLong("date_inserted");
                    for (int i = 0; i < 40; ++i) {
                        this.stats.add(i, resultSet.getInt("v" + i));
                    }
                }
                this.db.close(resultSet);
            }
            catch (Exception exception) {
                Log.error("DB Exception", (Throwable)exception);
                break block7;
            }
            finally {
                this.db.close(resultSet);
                this.db.close(statement);
                this.db.close(connection);
            }
            this.db.close(statement);
            this.db.close(connection);
        }
    }

    @Override
    public void resetStats() {
        this.initStats();
        this.cleanDb();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void cleanDb() {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.db.getConnection();
            String string = "DELETE FROM activity;";
            statement = connection.createStatement();
            statement.execute(string);
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
        }
        finally {
            this.db.close(resultSet);
            this.db.close(statement);
            this.db.close(connection);
        }
    }

    public String toString() {
        JSONArray jSONArray = new JSONArray();
        for (int i = 0; i < 40; ++i) {
            jSONArray.put((Object)this.stats.get(i));
        }
        return jSONArray.toString();
    }
}

