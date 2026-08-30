package com.strategyquant.plugin.Servlet.impl.Connection;

import com.strategyquant.lib.L;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.connection.ConnectionPluginManager;
import com.strategyquant.tradinglib.engine.TradingEngine;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(ConnectionServlet.class);
   private ConnectionManager connManager = TradingEngine.getLiveEngine().getConnectionManager();
   private static ConnectionServlet instance;

   public ConnectionServlet() {
      instance = this;
   }

   public static ConnectionServlet getInstance() {
      return instance;
   }

   protected String execute(String var1, JSONObject var2) {
      switch (var1) {
         case "add":
            return this.onAdd(var2);
         case "edit":
            return this.onEdit(var2);
         case "remove":
            return this.onRemove(var2);
         case "list":
            return this.onList();
         case "listplugins":
            return this.onListPlugins();
         case "connect":
            return this.onConnect(var2);
         case "disconnect":
            return this.onDisconnect(var2);
         case "isactive":
            return this.onIsActive(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onAdd(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = var1.get("ConnectionPlugin").toString();
         String var4 = var1.get("ConnectionName").toString();
         ValuesMap var5 = ValuesMap.fromJSON(var1.getJSONObject("ConnectionParams"));
         this.connManager.createConnection(var4, ConnectionPluginManager.getConnectionPlugin(var3), var5);
         var2.put("success", L.t("Connection '%s' added.", new Object[]{var4}));
         return var2.toString();
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot add connection.", new Object[0]), var6);
      }
   }

   public String onList() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (Connection var4 : this.connManager.getAllConnections()) {
         JSONObject var5 = new JSONObject();
         var5.put("ConnectionPlugin", var4.getPluginName());
         var5.put("ConnectionName", var4.getConnectionName());
         var5.put("ConnectionState", var4.getStatus());
         var5.put("ConnectionParams", var4.getConnectionParams().toJSON());
         var5.put("ConnectionType", var4.getType());
         if (var4.isExecutionEngine()) {
            var5.put("OpenPositions", var4.getExecutionEngine().getOpenOrdersCount(true));
            var5.put("AccountBalance", var4.getExecutionEngine().getAccountBalance());
            var5.put("AccountPL", var4.getExecutionEngine().getTotalPL());
         } else {
            var5.put("OpenPositions", "N/A");
            var5.put("AccountBalance", "N/A");
            var5.put("AccountPL", "N/A");
         }

         var2.put(var5);
      }

      var1.put("connections", var2);
      var1.put("success", L.t("Found %d connections.", new Object[]{this.connManager.getAllConnections().size()}));
      return var1.toString();
   }

   private String onListPlugins() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (IConnectionPlugin var4 : SQPluginManager.getPlugins(IConnectionPlugin.class)) {
         JSONObject var5 = new JSONObject();
         var5.put("PluginName", var4.getPluginName());
         var2.put(var5);
      }

      var1.put("plugins", var2);
      var1.put("success", L.t("Found %d plugins.", new Object[]{SQPluginManager.getPlugins(IConnectionPlugin.class).size()}));
      return var1.toString();
   }

   private String onRemove(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = var1.get("ConnectionName").toString();
         this.connManager.removeConnection(var3);
         var2.put("success", L.t("Connection '%s' removed.", new Object[]{var3}));
         return var2.toString();
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot remove connection.", new Object[0]), var4);
      }
   }

   private String onEdit(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = var1.get("ConnectionName").toString();
         ValuesMap var4 = ValuesMap.fromJSON(var1.getJSONObject("ConnectionParams"));
         this.connManager.editConnection(var3, var4);
         var2.put("success", L.t("Connection '%s' modified.", new Object[]{var3}));
         return var2.toString();
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot edit connection.", new Object[0]), var5);
      }
   }

   private String onConnect(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = var1.get("ConnectionName").toString();
         this.connManager.getConnection(var3).connect();
         var2.put("success", L.t("Connection '%s' connected.", new Object[]{var3}));
         return var2.toString();
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Connection failed.", new Object[0]), var4);
      }
   }

   private String onDisconnect(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = var1.get("ConnectionName").toString();
         this.connManager.getConnection(var3).disconnect();
         var2.put("success", L.t("Connection '%s' disconnected.", new Object[]{var3}));
         return var2.toString();
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot disconnect.", new Object[0]), var4);
      }
   }

   private String onIsActive(JSONObject var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = var1.get("ConnectionName").toString();
         if (this.connManager.getConnection(var3).isConnected()) {
            var2.put("active", "true");
         } else {
            var2.put("active", "false");
         }

         return var2.toString();
      } catch (Exception var4) {
         return apiErrorJSON(null, var4);
      }
   }
}
