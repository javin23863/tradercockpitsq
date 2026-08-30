package com.strategyquant.datalib.session;

import com.strategyquant.datalib.data.DataDb;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class SessionManager extends DataDb {
   private static SessionManager instance = null;
   private HashMap<String, Session> sessionCache = new HashMap<>();
   private ArrayList<Session> sessionList = new ArrayList<>();
   private boolean cacheEmpty = true;
   private Session sessionNoSession = new SessionNoSession();
   private SessionComparator sessionComparator = new SessionComparator();

   public static void init(String var0) throws Exception {
      if (instance != null) {
         throw new Exception("SessionManager.init() called more than once!");
      }

      instance = new SessionManager(var0);
   }

   private SessionManager(String var1) {
      super(var1);
      this.addInitialRecords();
   }

   private static SessionManager get() {
      return instance;
   }

   @Override
   public void initDatabase() {
      try {
         if (!this.tableExists("SESSIONS")) {
            String var1 = "CREATE TABLE SESSIONS (SESSION \tVARCHAR(50) PRIMARY KEY NOT NULL)";
            this.sqlCommand(var1);
            Log.debug("Sessions table created successfully");
         }

         if (!this.tableExists("ELEMENTS")) {
            String var3 = "CREATE TABLE ELEMENTS (ID\tINTEGER PRIMARY KEY AUTOINCREMENT,SESSION VARCHAR(50) NOT NULL,DAYFROM INTEGER NOT NULL,TIMEFROM INTEGER NOT NULL,DAYTO INTEGER NOT NULL,TIMETO INTEGER NOT NULL,EOD BOOLEAN NOT NULL)";
            this.sqlCommand(var3);
            Log.info("Elements table created successfully");
         }

         if (!this.columnExists("SESSIONS", "BROKER_ID")) {
            String var4 = "ALTER TABLE SESSIONS ADD BROKER_ID INT DEFAULT -1";
            this.sqlCommand(var4);
         }
      } catch (Exception var2) {
         Log.error("DB error: Cannot create Sessions table.", var2);
      }
   }

   private void addInitialRecords() {
      try {
         ArrayList var1 = new ArrayList();
         var1.add(new SessionElement(1, 0, 1, 0, true));
         var1.add(new SessionElement(2, 0, 2, 0, true));
         var1.add(new SessionElement(3, 0, 3, 0, true));
         var1.add(new SessionElement(4, 0, 4, 0, true));
         var1.add(new SessionElement(5, 0, 5, 0, true));
         Session var2 = new Session("Forex_245", var1);
         if (!this._checkSessionExists("Forex_245")) {
            this._addSession(var2);
         }

         var1.add(new SessionElement(6, 0, 6, 0, true));
         var1.add(new SessionElement(7, 0, 7, 0, true));
         var2 = new Session("Forex_247", var1);
         if (!this._checkSessionExists("Forex_247")) {
            this._addSession(var2);
         }

         var1 = new ArrayList();
         var1.add(new SessionElement(1, SessionElement.getTimeInt("08:30"), 1, SessionElement.getTimeInt("15:00"), true));
         var1.add(new SessionElement(2, SessionElement.getTimeInt("08:30"), 2, SessionElement.getTimeInt("15:00"), true));
         var1.add(new SessionElement(3, SessionElement.getTimeInt("08:30"), 3, SessionElement.getTimeInt("15:00"), true));
         var1.add(new SessionElement(4, SessionElement.getTimeInt("08:30"), 4, SessionElement.getTimeInt("15:00"), true));
         var1.add(new SessionElement(5, SessionElement.getTimeInt("08:30"), 5, SessionElement.getTimeInt("15:00"), true));
         var2 = new Session("US_Index_Futures", var1);
         if (!this._checkSessionExists("US_Index_Futures")) {
            this._addSession(var2);
         }

         var1 = new ArrayList();
         var1.add(new SessionElement(1, SessionElement.getTimeInt("09:30"), 1, SessionElement.getTimeInt("16:00"), true));
         var1.add(new SessionElement(2, SessionElement.getTimeInt("09:30"), 2, SessionElement.getTimeInt("16:00"), true));
         var1.add(new SessionElement(3, SessionElement.getTimeInt("09:30"), 3, SessionElement.getTimeInt("16:00"), true));
         var1.add(new SessionElement(4, SessionElement.getTimeInt("09:30"), 4, SessionElement.getTimeInt("16:00"), true));
         var1.add(new SessionElement(5, SessionElement.getTimeInt("09:30"), 5, SessionElement.getTimeInt("16:00"), true));
         var2 = new Session("US_Stocks", var1);
         if (!this._checkSessionExists("US_Stocks")) {
            this._addSession(var2);
         }
      } catch (Exception var3) {
         Log.error("Cannot add sessions.", var3);
      }
   }

   public static synchronized ArrayList<Session> getSessions() {
      return get()._getSessions();
   }

   public static void addSession(Session var0) throws SessionException {
      get()._addSession(var0);
      get()._clearCache();
   }

   public static Session getSession(String var0) {
      return get()._getSession(var0);
   }

   public static boolean checkSessionExists(String var0) {
      return get()._checkSessionExists(var0);
   }

   public static void removeSession(String var0) throws SessionException {
      get()._removeSession(var0);
      deleteOldDataFiles(var0, new File(SQPaths.dataDirPath));
   }

   public static void updateSession(Session var0) throws SessionException {
      updateSession(var0.getSessionName(), var0.getElements());
   }

   public static void updateSession(String var0, ArrayList<SessionElement> var1) throws SessionException {
      get()._updateSession(var0, var1);
      deleteOldDataFiles(var0, new File(SQPaths.dataDirPath));
   }

   private static void deleteOldDataFiles(String var0, File var1) {
      if (var1.exists()) {
         if (var1.isDirectory()) {
            File[] var2 = var1.listFiles();

            for (int var3 = 0; var3 < var2.length; var3++) {
               deleteOldDataFiles(var0, var2[var3]);
            }
         } else {
            tryDeleteFile(var1, var0);
         }
      }
   }

   private static void tryDeleteFile(File var0, String var1) {
      try {
         if (var0.getName().endsWith("_" + var1 + ".dat") && !var0.delete()) {
            Log.error("File '" + var0.getAbsolutePath() + "' was not deleted");
         }
      } catch (Exception var3) {
         Log.error("Cannot delete file '" + var0.getAbsolutePath() + "'", var3);
      }
   }

   private ArrayList<Session> _getSessions() {
      if (!this.cacheEmpty) {
         return this.sessionList;
      }

      this._clearCache();
      Connection var1 = null;
      Statement var2 = null;
      Statement var3 = null;
      ResultSet var4 = null;
      ResultSet var5 = null;

      try {
         var1 = this.getConnection();
         var3 = var1.createStatement();
         var5 = var3.executeQuery("SELECT * FROM ELEMENTS");
         HashMap var6 = new HashMap();

         while (var5.next()) {
            String var7 = var5.getString(2);
            int var8 = var5.getInt(3);
            int var9 = var5.getInt(4);
            int var10 = var5.getInt(5);
            int var11 = var5.getInt(6);
            boolean var12 = var5.getBoolean(7);
            if (!var6.containsKey(var7)) {
               var6.put(var7, new ArrayList());
            }

            ((ArrayList)var6.get(var7)).add(new SessionElement(var8, var9, var10, var11, var12));
         }

         var2 = var1.createStatement();
         var4 = var2.executeQuery("SELECT SESSION,BROKER_ID FROM SESSIONS");

         while (var4.next()) {
            String var20 = var4.getString(1);
            int var21 = var4.getInt(2);
            ArrayList var22 = var6.containsKey(var20) ? (ArrayList)var6.get(var20) : new ArrayList();
            Session var23 = new Session(var20, var22);
            var23.setBroker(var21);
            this.sessionCache.put(var20, var23);
            this.sessionList.add(var23);
         }

         this.cacheEmpty = false;
      } catch (Exception var16) {
         Log.error("DB Exception", var16);
      } finally {
         this.close(var4);
         this.close(var2);
         this.close(var1);
      }

      this.sessionList.sort(this.sessionComparator);
      return this.sessionList;
   }

   private void _addSession(Session var1) throws SessionException {
      Connection var2 = null;
      Statement var3 = null;
      Object var4 = null;

      try {
         var2 = this.getConnection();
         if (this._checkSessionExists(var2, var1.getSessionName())) {
            throw new SessionException("Session '" + var1.getSessionName() + "' already exists!");
         }

         var3 = var2.createStatement();
         String var5 = "INSERT INTO SESSIONS (SESSION, BROKER_ID) VALUES ('" + var1.getSessionName() + "'," + var1.getBroker() + ")";
         var3.executeUpdate(var5);
         this.createElements(var1.getSessionName(), var1.getElements(), var3);
      } catch (Exception var9) {
         throw new SessionException(var9.getMessage());
      } finally {
         this.close((ResultSet)var4);
         this.close(var3);
         this.close(var2);
      }
   }

   private void createElements(String var1, ArrayList<SessionElement> var2, Statement var3) throws SQLException {
      for (SessionElement var5 : var2) {
         String var6 = "INSERT INTO ELEMENTS(SESSION, DAYFROM, TIMEFROM, DAYTO, TIMETO, EOD) VALUES ('"
            + var1
            + "',"
            + var5.getDayFrom()
            + ","
            + var5.getTimeFrom()
            + ","
            + var5.getDayTo()
            + ","
            + var5.getTimeTo()
            + ","
            + (var5.isEOD() ? "1" : "0")
            + ")";
         Log.debug(var6);
         var3.executeUpdate(var6);
      }

      this._clearCache();
   }

   private boolean _checkSessionExists(String var1) {
      if (var1.equals("No Session")) {
         return true;
      }

      Connection var2 = null;

      try {
         var2 = this.getConnection();
         return this._checkSessionExists(var2, var1);
      } catch (Exception var8) {
         Log.error("DB Exception", var8);
         return false;
      } finally {
         this.close(var2);
      }
   }

   private boolean _checkSessionExists(Connection var1, String var2) {
      if (var2.equals("No Session")) {
         return true;
      } else {
         return this.sessionCache.containsKey(var2) ? true : this.sqlCheckRecordExists(var1, "SELECT * FROM SESSIONS WHERE SESSION='" + var2 + "'");
      }
   }

   private Session _getSession(String var1) {
      if (var1.equals("No Session")) {
         return this.sessionNoSession;
      }

      if (this.sessionCache.containsKey(var1)) {
         return this.sessionCache.get(var1);
      }

      Connection var2 = null;

      try {
         var2 = this.getConnection();
         if (!this._checkSessionExists(var2, var1)) {
            return null;
         }

         ArrayList var3 = this._getSessionElements(var2, var1);
         return new Session(var1, var3);
      } catch (Exception var8) {
         Log.error("DB Exception", var8);
      } finally {
         this.close(var2);
      }

      return null;
   }

   private ArrayList<SessionElement> _getSessionElements(Connection var1, String var2) throws SessionException {
      Statement var3 = null;
      ResultSet var4 = null;

      try {
         var3 = var1.createStatement();
         var4 = var3.executeQuery("SELECT * FROM ELEMENTS WHERE SESSION='" + var2 + "'");
         ArrayList var5 = new ArrayList();

         while (var4.next()) {
            var5.add(new SessionElement(var4.getInt("DAYFROM"), var4.getInt("TIMEFROM"), var4.getInt("DAYTO"), var4.getInt("TIMETO"), var4.getBoolean("EOD")));
         }

         return var5;
      } catch (Exception var10) {
         Log.error("DB Exception", var10);
         throw new SessionException("Error getting elements for session '" + var2 + "' !");
      } finally {
         this.close(var4);
         this.close(var3);
      }
   }

   private void _clearCache() {
      this.sessionCache.clear();
      this.sessionList.clear();
      this.sessionList.add(this.sessionNoSession);
      this.cacheEmpty = true;
   }

   private void _removeSession(String var1) throws SessionException {
      Statement var2 = null;

      try {
         Connection var3 = this.getConnection();
         var2 = var3.createStatement();
         var2.executeUpdate("DELETE FROM SESSIONS WHERE SESSION='" + var1 + "'");
         var2.executeUpdate("DELETE FROM ELEMENTS WHERE SESSION='" + var1 + "'");
         this._clearCache();
      } catch (Exception var7) {
         Log.error("DB Exception", var7);
         throw new SessionException("Error while removing session '" + var1 + "'.");
      } finally {
         this.close(var2);
      }
   }

   private void _updateSession(String var1, ArrayList<SessionElement> var2) throws SessionException {
      Statement var3 = null;

      try {
         Connection var4 = this.getConnection();
         var3 = var4.createStatement();
         var3.executeUpdate("DELETE FROM ELEMENTS WHERE SESSION='" + var1 + "'");
         this.createElements(var1, var2, var3);
      } catch (Exception var8) {
         Log.error("DB Exception", var8);
         throw new SessionException("Error while removing session '" + var1 + "'.");
      } finally {
         this.close(var3);
      }
   }
}
