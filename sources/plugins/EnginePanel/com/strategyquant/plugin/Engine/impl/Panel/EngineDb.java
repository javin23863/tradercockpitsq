package com.strategyquant.plugin.Engine.impl.Panel;

import com.strategyquant.lib.db.DbBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class EngineDb extends DbBase {
   private static EngineDb instance = null;

   public static void init(String var0) {
      if (instance == null) {
         instance = new EngineDb(var0);
      }
   }

   private EngineDb(String var1) {
      super("EngineDb", "EnginePanel.db", var1);
      this.initDatabase();
   }

   private static EngineDb get() {
      return instance;
   }

   protected void initDatabase() {
      if (!this.isDbExist()) {
         try {
            String var1 = "CREATE TABLE SETTINGS (KEY VARCHAR(256) PRIMARY KEY, VALUE VARCHAR(256) DEFAULT '')";
            this.sqlCommand(var1);
            Log.debug("Engine settings table created successfully");
         } catch (Exception var2) {
            Log.error("DB error:", var2);
         }
      }
   }

   public static void setSetting(String var0, String var1) {
      get()._setSetting(var0, var1);
   }

   public static String getSetting(String var0, String var1) {
      String var2 = get()._getSetting(var0);
      return var2 == null ? var1 : var2;
   }

   private void _setSetting(String var1, String var2) {
      try {
         if (this._getSetting(var1) == null) {
            String var3 = "INSERT INTO SETTINGS (KEY, VALUE) VALUES ('" + var1 + "','" + var2 + "')";
            this.sqlCommand(var3);
         } else {
            String var5 = "UPDATE SETTINGS SET VALUE='" + var2 + "' WHERE KEY='" + var1 + "'";
            this.sqlCommand(var5);
         }
      } catch (Exception var4) {
         Log.error("DB Exception", var4);
      }
   }

   private String _getSetting(String var1) {
      Connection var2 = null;
      Statement var3 = null;
      ResultSet var4 = null;

      try {
         var2 = this.getConnection();
         var3 = var2.createStatement();
         var4 = var3.executeQuery("SELECT * FROM SETTINGS WHERE KEY='" + var1 + "'");
         if (var4.next()) {
            return var4.getString("VALUE");
         }
      } catch (Exception var9) {
         Log.error("DB Exception", var9);
      } finally {
         this.close(var4);
         this.close(var3);
         this.close(var2);
      }

      return null;
   }
}
