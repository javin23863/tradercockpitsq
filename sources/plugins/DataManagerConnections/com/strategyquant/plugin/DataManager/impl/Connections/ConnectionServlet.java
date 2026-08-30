package com.strategyquant.plugin.DataManager.impl.Connections;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.connection.ConnectionPluginManager;
import com.strategyquant.tradinglib.plugindef.connection.IConnectionPlugin;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.WebAppManager;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(ConnectionServlet.class);
   private ConnectionManager connManager;
   private static ConnectionServlet instance;

   public ConnectionServlet() {
      instance = this;
   }

   public static ConnectionServlet getInstance() {
      return instance;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
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
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onAdd(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "ConnectionPlugin")[0];
         String var4 = this.tryGetParam(var1, "ConnectionName")[0];
         ValuesMap var5 = ValuesMap.fromJSON(new JSONObject(this.tryGetParam(var1, "ConnectionParams")[0]));
         this.connManager.createConnection(var4, ConnectionPluginManager.getConnectionPlugin(var3), var5);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getConnections(), new String[0]);
         var2.put("success", L.t("Connection '%d' added.", new Object[]{var4}));
         return var2.toString();
      } catch (Exception var6) {
         return apiErrorJSON(null, var6);
      }
   }

   public String onList() {
      JSONObject var1 = new JSONObject();
      SQWebSocketManager.addToDataQueue(WSDataObjects.getConnections(), new String[0]);
      var1.put("success", L.t("Connections listed.", new Object[0]));
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

   private String onRemove(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "ConnectionName")[0];
         if (!this.isConnectionActive(var3)) {
            this.connManager.removeConnection(var3);
            var2.put("success", L.t("Connection '%s' removed.", new Object[]{var3}));
            SQWebSocketManager.addToDataQueue(WSDataObjects.getConnections(), new String[0]);
         } else {
            var2.put("error", L.t("Connection '%s' is active and thus cannot be removed.", new Object[]{var3}));
         }

         return var2.toString();
      } catch (Exception var4) {
         return apiErrorJSON(null, var4);
      }
   }

   private String onEdit(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "ConnectionName")[0];
         ValuesMap var4 = ValuesMap.fromJSON(new JSONObject(this.tryGetParam(var1, "ConnectionParams")[0]));
         if (!this.isConnectionActive(var3)) {
            this.connManager.editConnection(var3, var4);
            var2.put("success", L.t("Connection '%s' modified.", new Object[]{var3}));
            SQWebSocketManager.addToDataQueue(WSDataObjects.getConnections(), new String[0]);
         } else {
            var2.put("error", L.t("Connection '%s' is active and thus cannot be edited.", new Object[]{var3}));
         }

         return var2.toString();
      } catch (Exception var5) {
         return apiErrorJSON(null, var5);
      }
   }

   public boolean isConnectionActive(String var1) {
      String var2 = "SQTRADER";
      Map var3 = WebAppManager.getAppPorts();
      if (var3.containsKey(var2)) {
         int var4 = (Integer)var3.get(var2);
         String var5 = WebAppManager.getBaseURL(var4) + "/connections/isactive?args={'ConnectionName':'" + var1 + "'}";
         String var6 = null;

         try {
            var6 = SQUtils.httpGet(var5);
         } catch (Exception var9) {
         }

         if (var6 != null) {
            try {
               JSONObject var7 = new JSONObject(var6);
               String var8 = var7.get("active").toString();
               return var8.equals("true");
            } catch (Exception var10) {
               Log.error("Error reading response.", var10);
            }
         }
      }

      return false;
   }
}
