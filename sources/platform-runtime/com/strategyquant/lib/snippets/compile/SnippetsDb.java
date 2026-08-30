/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.db.DbBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Deprecated
public class SnippetsDb
extends DbBase {
    private static SnippetsDb instance = null;

    public static void init(String string) throws Exception {
        if (instance != null) {
            return;
        }
        instance = new SnippetsDb(string);
    }

    private SnippetsDb(String string) {
        super("SnippetsDb", "snippets.db", string);
        this.initDatabase();
    }

    public static SnippetsDb get() {
        return instance;
    }

    @Override
    protected void initDatabase() {
        if (this.isDbExist()) {
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            String string = "CREATE TABLE SNIPPETS (SOURCE VARCHAR(1024) PRIMARY KEY NOT NULL, HASH VARCHAR(255) NOT NULL)";
            this.sqlCommand(string);
            Log.debug("Database created successfully");
        }
        catch (Exception exception) {
            Log.error("DB error:", (Throwable)exception);
        }
    }

    public static void setHash(String string, String string2) {
        SnippetsDb.get().iSetHash(string, string2);
    }

    public static String getHash(String string) {
        return SnippetsDb.get().iGetHash(string);
    }

    private void iSetHash(String string, String string2) {
        try {
            if (this.iGetHash(string) == null) {
                String string3 = "INSERT INTO SNIPPETS (SOURCE, HASH) VALUES ('" + string + "','" + string2 + "')";
                this.sqlCommand(string3);
            } else {
                String string4 = "UPDATE SNIPPETS SET hash='" + string2 + "' WHERE SOURCE='" + string + "'";
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
    private String iGetHash(String string) {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM SNIPPETS WHERE SOURCE='" + string + "'");
            return resultSet.next() ? resultSet.getString("HASH") : null;
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
