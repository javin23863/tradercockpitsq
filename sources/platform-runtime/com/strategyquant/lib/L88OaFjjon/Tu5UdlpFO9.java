/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.L88OaFjjon;

import com.strategyquant.lib.db.DbBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Tu5UdlpFO9
extends DbBase {
    private static Tu5UdlpFO9 instance = null;

    public static void init(String string) {
        if (instance != null) {
            return;
        }
        instance = new Tu5UdlpFO9(string);
    }

    private Tu5UdlpFO9(String string) {
        super("LicenseDb", "license.db", string);
        this.initDatabase();
    }

    private static Tu5UdlpFO9 get() {
        return instance;
    }

    @Override
    protected void initDatabase() {
        if (this.isDbExist()) {
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            String string = "CREATE TABLE IF NOT EXISTS SETTINGS(KEY VARCHAR(255) PRIMARY KEY, VALUE VARCHAR(255) DEFAULT '')";
            this.sqlCommand(string);
            Log.debug("Database created successfully");
        }
        catch (Exception exception) {
            Log.error("DB error:", (Throwable)exception);
        }
    }

    public static void pzi7BBRxNg(String string) {
        Tu5UdlpFO9.get()._setSetting("license", string);
    }

    public static String odz0Gyiqhr() {
        return Tu5UdlpFO9.get()._getSetting("license");
    }

    public static String pBfFbrabhJ(String string, String string2) {
        String string3 = Tu5UdlpFO9.get()._getSetting(string);
        return string3 == null ? string2 : string3;
    }

    private void _setSetting(String string, String string2) {
        try {
            if (this._getSetting(string) == null) {
                String string3 = "INSERT INTO SETTINGS (KEY, VALUE) VALUES ('" + string + "','" + string2 + "')";
                this.sqlCommand(string3);
            } else {
                String string4 = "UPDATE SETTINGS SET VALUE='" + string2 + "' WHERE KEY='" + string + "'";
                this.sqlCommand(string4);
            }
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private String _getSetting(String string) {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM SETTINGS WHERE KEY='" + string + "'");
            return resultSet.next() ? resultSet.getString("VALUE") : null;
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
            return null;
        }
        finally {
            this.close(resultSet);
            this.close(statement);
            this.close(connection);
        }
    }
}
