package com.strategyquant.tradinglib.connection;

import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.engine.TradingEngine;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionsFileManager {
   public static final Logger Log = LoggerFactory.getLogger("ConnectionFileManager");
   private static final String connectionsFileName = "connections.txt";
   private static final String connectionsFilePath = MainApp.getDataPath() + "user/data/" + "connections.txt";

   public static void saveConnections() {
      Log.info("Saving connections to " + connectionsFilePath);
      ConnectionManager var0 = TradingEngine.getLiveEngine().getConnectionManager();

      try {
         PrintWriter var1 = new PrintWriter(new FileWriter(connectionsFilePath, false));

         try {
            for (Connection var3 : var0.getAllConnections()) {
               var1.write(var3.toJSON() + "\n");
            }
         } catch (Throwable var5) {
            try {
               var1.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         var1.close();
      } catch (IOException var6) {
         Log.error("Error while saving connections to the connections file.", var6);
      }
   }

   public static void loadConnections() {
      Log.info("Loading connections from " + connectionsFilePath);
      ConnectionManager var0 = TradingEngine.getLiveEngine().getConnectionManager();

      try {
         BufferedReader var1 = new BufferedReader(new FileReader(connectionsFilePath));

         String var2;
         try {
            while ((var2 = var1.readLine()) != null) {
               try {
                  JSONObject var3 = new JSONObject(var2);
                  String var4 = var3.get("ConnectionName").toString();
                  String var5 = var3.get("ConnectionPlugin").toString();
                  ValuesMap var6 = ValuesMap.fromJSON(var3.getJSONObject("ConnectionParams"));
                  Connection var7 = var0.createConnection(var4, ConnectionPluginManager.getConnectionPlugin(var5), var6);
                  if (var6.get("connectOnStart") != null && var6.get("connectOnStart").toString().equals("true")) {
                     var7.connect();
                  }
               } catch (Exception var9) {
                  Log.error("Cannot initialize the connection.", var9);
               }
            }
         } catch (Throwable var10) {
            try {
               var1.close();
            } catch (Throwable var8) {
               var10.addSuppressed(var8);
            }

            throw var10;
         }

         var1.close();
      } catch (Exception var11) {
         Log.error("Error while initializing auto-connect connections...", var11);
      }
   }
}
