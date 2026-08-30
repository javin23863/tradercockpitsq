/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.qdm.symbols;

import com.strategyquant.qdm.QDMDb;
import com.strategyquant.qdm.QDMStats;
import com.strategyquant.qdm.symbols.SymbolStats;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SymbolsStats
extends QDMStats {
    public static final int SourceDukas = 1;
    public static final int DataTypeTick = 1;
    public static final int DataTypeM1 = 1;
    public static final Logger Log = LoggerFactory.getLogger(SymbolsStats.class);
    private ArrayList<SymbolStats> symbolsStats = new ArrayList();

    public SymbolsStats(QDMDb qDMDb) {
        super(qDMDb);
    }

    public void add(String string, int n, int n2, boolean bl, int n3) {
        SymbolStats symbolStats = new SymbolStats(string, n, n2, bl, n3);
        this.symbolsStats.add(symbolStats);
    }

    @Override
    public void init() throws Exception {
        String string = "CREATE TABLE IF NOT EXISTS symbol_stats (id INTEGER PRIMARY KEY AUTOINCREMENT,`symbol` varchar(256) NOT NULL,`source` tinyint(1) NOT NULL,`data_type` tinyint(1) NOT NULL,`cdn_used` tinyint(1) NOT NULL,`downloaded_years` int(3) NOT NULL)";
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
            for (SymbolStats symbolStats : this.symbolsStats) {
                String string = "INSERT INTO symbol_stats (symbol, source, data_type, cdn_used, downloaded_years) VALUES ('" + symbolStats.symbol + "', " + symbolStats.source + ", " + symbolStats.dataType + "," + (symbolStats.cdnUsed ? 1 : 0) + "," + symbolStats.downloadedYears + ")";
                this.db.sqlCommand(connection, string);
            }
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
        block6: {
            Connection connection = null;
            Statement statement = null;
            ResultSet resultSet = null;
            try {
                connection = this.db.getConnection();
                String string = "SELECT * FROM symbol_stats";
                statement = connection.createStatement();
                resultSet = statement.executeQuery(string);
                while (resultSet.next()) {
                    SymbolStats symbolStats = new SymbolStats();
                    symbolStats.symbol = resultSet.getString("symbol");
                    symbolStats.source = resultSet.getInt("source");
                    symbolStats.dataType = resultSet.getInt("data_type");
                    symbolStats.cdnUsed = resultSet.getBoolean("cdn_used");
                    symbolStats.downloadedYears = resultSet.getInt("downloaded_years");
                    this.symbolsStats.add(symbolStats);
                }
                this.db.close(resultSet);
            }
            catch (Exception exception) {
                Log.error("DB Exception", (Throwable)exception);
                break block6;
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
        this.symbolsStats.clear();
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
            String string = "DELETE FROM symbol_stats;";
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
        for (SymbolStats symbolStats : this.symbolsStats) {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("symbol", (Object)symbolStats.symbol);
            jSONObject.put("source", symbolStats.source);
            jSONObject.put("dataType", symbolStats.dataType);
            jSONObject.put("cdnUsed", symbolStats.cdnUsed);
            jSONObject.put("downloadedYears", symbolStats.downloadedYears);
            jSONArray.put((Object)jSONObject);
        }
        return jSONArray.toString();
    }
}

