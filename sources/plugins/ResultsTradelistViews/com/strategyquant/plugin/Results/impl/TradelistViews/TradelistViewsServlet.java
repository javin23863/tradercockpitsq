package com.strategyquant.plugin.Results.impl.TradelistViews;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.TradelistColumn;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.tradelist.TradelistColumnsList;
import com.strategyquant.tradinglib.tradelist.TradelistTableView;
import com.strategyquant.tradinglib.tradelist.TradelistViewsManager;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradelistViewsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(TradelistViewsServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "getColumns":
            return this.onGetColumns();
         case "getViews":
            return this.onGetViews();
         case "addView":
            return this.onAddView(var2);
         case "updateView":
            return this.onUpdateView(var2);
         case "removeView":
            return this.onRemoveView(var2);
         case "changeView":
            return this.onChangeView(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onGetColumns() {
      JSONObject var1 = new JSONObject();
      var1.put("columns", this.getColumns());
      var1.put("success", L.t("Columns listed.", new Object[0]));
      return var1.toString();
   }

   private String onGetViews() {
      JSONObject var1 = new JSONObject();
      var1.put("tradelistViews", WSDataObjects.getTradelistViews().getDataArray());
      var1.put("success", L.t("Views listed.", new Object[0]));
      return var1.toString();
   }

   private JSONArray getColumns() {
      JSONArray var1 = new JSONArray();

      for (TradelistColumn var4 : TradelistColumnsList.get().getAvailableClasses()) {
         JSONObject var5 = new JSONObject();
         var5.put("name", var4.getName());
         var5.put("class", var4.getClass().getSimpleName());
         var5.put("type", var4.getType());
         var5.put("fontBold", var4.isFontBold());
         var1.put(var5);
      }

      return var1;
   }

   private void sendWSUpdate() {
      SQWebSocketManager.addToDataQueue(WSDataObjects.getTradelistViews(), new String[]{"SQUANT", "BUILDER", "OPTIMIZER", "RETESTER", "TASKMANAGER", "RESULTS"});
   }

   private String onAddView(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "viewXML")[0];
         TradelistTableView var4 = TradelistViewsManager.getInstance().addView(var3, true);
         if (var4 != null && var4.name.equals(var4.name)) {
            MainApp.settings().set("tradelistView", var4.name);
         }

         this.sendWSUpdate();
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot add view.", new Object[0]), var5);
      }

      var2.put("success", L.t("View added.", new Object[0]));
      return var2.toString();
   }

   private String onUpdateView(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "viewXML")[0];
         TradelistTableView var4 = TradelistViewsManager.getInstance().updateView(var3);
         TradelistTableView var5 = TradelistViewsManager.getInstance().getSelectedTradelistView();
         if (var5 != null && var4.name.equals(var5.name)) {
            MainApp.settings().set("tradelistView", var4.name);
         }

         this.sendWSUpdate();
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot update tradelist view.", new Object[0]), var6);
      }

      var2.put("success", L.t("Tradelist view updated.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveView(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         TradelistViewsManager.getInstance().removeView(var3);
         TradelistTableView var4 = TradelistViewsManager.getInstance().getSelectedTradelistView();
         String var5 = var4.name;
         if (var5.equals(var3)) {
            TradelistTableView var6 = TradelistViewsManager.getInstance().getView("Default");
            MainApp.settings().set("tradelistView", var6.name);
         }

         this.sendWSUpdate();
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Error while removing view.", new Object[0]), var7);
      }

      var2.put("success", L.t("View removed.", new Object[0]));
      return var2.toString();
   }

   private String onChangeView(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         var3 = this.tryGetParam(var1, "viewName")[0];
         TradelistTableView var4 = TradelistViewsManager.getInstance().getView(var3);
         MainApp.settings().set("tradelistView", var4.name);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Error while changing view.", new Object[0]), var5);
      }

      var2.put("success", L.t("Tradelist view changed to '%s'.", new Object[]{var3}));
      return var2.toString();
   }
}
