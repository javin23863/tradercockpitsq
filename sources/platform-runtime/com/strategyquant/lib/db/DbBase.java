/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DbBase {
    protected static Logger Log;
    private final String dbFileName;
    private final String directory;
    private final String dbPath;
    private String dbName;

    public DbBase(String string, String string2, String string3) {
        Log = LoggerFactory.getLogger((String)string);
        File file = new File(string3);
        if (!file.exists() && !file.mkdirs()) {
            Log.error("Cannot create folder '" + string3 + "'");
        }
        this.dbName = string;
        this.dbFileName = string2;
        this.directory = string3;
        this.dbPath = "jdbc:sqlite:" + string3 + "/" + string2;
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (Exception exception) {
            Log.error("DbBase init error", (Throwable)exception);
        }
    }

    public synchronized void sqlCommand(String string) throws Exception {
        try {
            Connection connection = this.getConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate(string);
            statement.close();
            connection.close();
        }
        catch (Exception exception) {
            Log.error("Data DB error performing query: " + string, (Throwable)exception);
            throw new Exception("DB error. " + exception.getMessage(), exception);
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.dbPath);
    }

    public synchronized void sqlCommand(Connection connection, String string) throws Exception {
        try {
            Class.forName("org.sqlite.JDBC");
            Statement statement = connection.createStatement();
            statement.executeUpdate(string);
            statement.close();
        }
        catch (Exception exception) {
            Log.error("Data DB error:", (Throwable)exception);
            throw new Exception("DB error. " + exception.getMessage(), exception);
        }
    }

    public synchronized int sqlInsertReturnAutoId(Connection connection, String string) throws Exception {
        int n = -1;
        try {
            Class.forName("org.sqlite.JDBC");
            Statement statement = connection.createStatement();
            statement.executeUpdate(string);
            ResultSet resultSet = statement.getGeneratedKeys();
            if (resultSet.next()) {
                n = resultSet.getInt(1);
            }
            statement.close();
        }
        catch (Exception exception) {
            Log.error("Data DB error: " + string, (Throwable)exception);
            throw new Exception("DB error. " + exception.getMessage(), exception);
        }
        return n;
    }

    public synchronized int sqlInsertReturnAutoId(Connection connection, PreparedStatement preparedStatement) throws Exception {
        int n = -1;
        try {
            Class.forName("org.sqlite.JDBC");
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                n = resultSet.getInt(1);
            }
            preparedStatement.close();
        }
        catch (Exception exception) {
            Log.error("Data DB error: ", (Throwable)exception);
            throw new Exception("DB error. " + exception.getMessage(), exception);
        }
        return n;
    }

    public synchronized void sqlCommand(Connection connection, PreparedStatement preparedStatement) throws Exception {
        try {
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
        catch (Exception exception) {
            Log.error("Data DB error:", (Throwable)exception);
            throw new Exception("DB error. " + exception.getMessage(), exception);
        }
    }

    public String _getDirectory() {
        return this.directory;
    }

    public String getDbPath() {
        return this.dbPath;
    }

    public boolean isDbExist() {
        File file = new File(this.directory + "/" + this.dbFileName);
        if (file.exists() && file.isFile()) {
            return true;
        }
        try {
            if (!file.createNewFile()) {
                Log.error("Cannot create database file '" + file.getAbsolutePath() + "'");
            }
        }
        catch (Exception exception) {
            Log.error("Cannot create database file '" + file.getAbsolutePath() + "'");
        }
        return false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public synchronized boolean columnExists(String string, String string2) throws SQLException {
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            Connection connection = this.getConnection();
            String string3 = "select " + string2 + " from " + string;
            statement = connection.createStatement();
            resultSet = statement.executeQuery(string3);
            boolean bl = true;
            this.close(resultSet);
            this.close(statement);
            return bl;
        }
        catch (Exception exception) {
            return false;
        }
        finally {
            this.close(resultSet);
            this.close(statement);
        }
    }

    public boolean tableExists(String string) throws SQLException {
        Connection connection = this.getConnection();
        String string2 = "SELECT name FROM sqlite_master WHERE type='table' AND name='" + string + "';";
        return this.sqlCheckRecordExists(connection, string2);
    }

    public void close(ResultSet resultSet) {
        try {
            if (resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
        }
    }

    public void close(Statement statement) {
        try {
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
        }
    }

    public void close(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public synchronized boolean sqlCheckRecordExists(Connection connection, String string) {
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(string);
            if (resultSet.next()) {
                boolean bl = true;
                this.close(resultSet);
                this.close(statement);
                return bl;
            }
            this.close(resultSet);
            this.close(statement);
        }
        catch (Exception exception) {
            try {
                Log.error("DB Exception", (Throwable)exception);
                boolean bl = false;
                this.close(resultSet);
                this.close(statement);
                return bl;
            }
            catch (Throwable throwable) {
                this.close(resultSet);
                this.close(statement);
                throw throwable;
            }
        }
        return false;
    }

    protected abstract void initDatabase();
}

