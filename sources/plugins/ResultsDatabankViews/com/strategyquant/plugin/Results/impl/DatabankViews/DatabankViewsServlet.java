package com.strategyquant.plugin.Results.impl.DatabankViews;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.databank.DatabankViewsManager;
import com.strategyquant.tradinglib.databank.RecentColumns;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankViewsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DatabankViewsServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "getColumns":
            return this.onGetColumns();
         case "updateRecentColumns":
            return this.onUpdateRecentColumns(var2);
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
      JSONArray var2 = new JSONArray();
      JSONArray var3 = new JSONArray();

      for (DatabankColumn var6 : DatabankColumns.get().getAvailableClasses()) {
         JSONObject var7 = new JSONObject();
         var7.put("name", var6.getName());
         var7.put("class", var6.getClass().getSimpleName());
         var7.put("type", var6.getType());
         var2.put(var7);
      }

      for (String var9 : RecentColumns.getRecentCols()) {
         var3.put(var9);
      }

      var1.put("columns", var2);
      var1.put("recentColumns", var3);
      var1.put("success", L.t("Columns listed.", new Object[0]));
      return var1.toString();
   }

   private String onUpdateRecentColumns(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();

      try {
         String var4 = this.tryGetParam(var1, "lastUsedCols")[0];
         RecentColumns.updateRecentCols(var4.split(";"));

         for (String var6 : RecentColumns.getRecentCols()) {
            var3.put(var6);
         }
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot update recent columns.", new Object[0]), var7);
      }

      var2.put("recentColumns", var3);
      var2.put("success", L.t("Recent columns updated.", new Object[0]));
      return var2.toString();
   }

   private String onGetViews() {
      JSONObject var1 = new JSONObject();
      var1.put("databankViews", DatabankViewsManager.getInstance().toJSON());
      var1.put("success", L.t("Databank views broadcasted", new Object[0]));
      return var1.toString();
   }

   private String onAddView(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "viewXML")[0];
         DatabankTableView var4 = DatabankViewsManager.getInstance().addView(var3);

         try {
            String var5 = this.tryGetParam(var1, "project")[0];
            String var6 = this.tryGetParam(var1, "databank")[0];
            Databank var7 = (Databank)ProjectEngine.get(var5).getDatabanks().get(var6);
            var7.setView(var4);
         } catch (Exception var8) {
         }

         this.sendDataUpdate();
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot add view.", new Object[0]), var9);
      }

      var2.put("success", L.t("View added.", new Object[0]));
      return var2.toString();
   }

   private String onUpdateView(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "viewXML")[0];
         DatabankTableView var4 = DatabankViewsManager.getInstance().updateView(var3);

         for (SQProject var6 : ProjectEngine.list()) {
            for (Databank var8 : var6.getDatabanks().list()) {
               if (var8.getView().name.equals(var4.name)) {
                  var8.setView(var4);
               }
            }
         }

         this.sendDataUpdate();
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot update view.", new Object[0]), var9);
      }

      var2.put("success", L.t("View updated.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveView(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];

         for (SQProject var5 : ProjectEngine.list()) {
            for (Databank var7 : var5.getDatabanks().list()) {
               if (var7.getView().name.equals(var3)) {
                  return apiErrorJSON(L.t("Cannot remove view '%s', because it is still used by some databanks.", new Object[]{var3}), null);
               }
            }
         }

         DatabankViewsManager.getInstance().removeView(var3);
         this.sendDataUpdate();
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Error while removing view.", new Object[0]), var8);
      }

      var2.put("success", L.t("View removed.", new Object[0]));
      return var2.toString();
   }

   private String onChangeView(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      Object var3 = null;
      String var4 = null;

      try {
         String var5 = this.tryGetParam(var1, "projectName")[0];
         var3 = this.tryGetParam(var1, "databankName")[0];
         var4 = this.tryGetParam(var1, "viewName")[0];
         SQProject var6 = ProjectEngine.get(var5);
         Databank var7 = (Databank)var6.getDatabanks().get(var3);
         DatabankTableView var8 = DatabankViewsManager.getInstance().getView(var4);
         var7.setView(var8);
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Error while changing view.", new Object[0]), var9);
      }

      var2.put("success", L.t("View for databank '%s' changed to '%s'.", new Object[]{var3, var4}));
      return var2.toString();
   }

   private void sendDataUpdate() {
      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getDatabankViews(), new String[]{"SQUANT"});
      } catch (Exception var2) {
         Log.error("Cannot send websocket data update. ", var2);
      }
   }
}
