package com.strategyquant.plugin.Results.impl.WalkForward.views;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WFViews {
   private static final Logger Log = LoggerFactory.getLogger(WFViews.class);
   private WFViewsManager manager;

   public WFViews(String var1) {
      this.manager = new WFViewsManager(var1);
   }

   public String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "viewsGetColumns":
            return this.onGetColumns();
         case "viewsGetViews":
            return this.onGetViews();
         case "viewsAddView":
            return this.onAddView(var2);
         case "viewsUpdateView":
            return this.onUpdateView(var2);
         case "viewsRemoveView":
            return this.onRemoveView(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onGetColumns() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (DatabankColumn var5 : DatabankColumns.get().getAvailableClasses()) {
         JSONObject var6 = new JSONObject();
         var6.put("name", var5.getName());
         var6.put("class", var5.getClass().getSimpleName());
         var2.put(var6);
      }

      var1.put("columns", var2);
      var1.put("success", L.t("Columns listed.", new Object[0]));
      return var1.toString();
   }

   private String onGetViews() throws Exception {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      try {
         for (String var4 : this.manager.getViews().keySet()) {
            JSONObject var5 = new JSONObject();
            JSONArray var6 = new JSONArray();
            DatabankTableView var7 = this.manager.getView(var4);
            var5.put("name", var4);

            for (DatabankTableColumnEntry var9 : var7.columns) {
               if (var9.tableColumn != null) {
                  try {
                     JSONObject var10 = new JSONObject();
                     var10.put("entryName", var9.tableColumn.getName());
                     var10.put("columnName", var9.tableColumn.getName());
                     var10.put("sampleType", var9.sampleType);
                     var10.put("class", var9.tableColumn.getClass().getSimpleName());
                     var10.put("width", var9.tableColumn.getWidth());
                     var10.put("type", var9.tableColumn.getType());
                     var6.put(var10);
                  } catch (Exception var11) {
                     Log.error("Cannot load view", var11);
                  }
               }
            }

            var5.put("columns", var6);
            var2.put(var5);
         }

         var1.put("views", var2);
      } catch (Exception var12) {
         throw new Exception("Cannot get list of views.", var12);
      }

      var1.put("success", L.t("Views listed.", new Object[0]));
      return var1.toString();
   }

   private String onAddView(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "viewXML")[0];
         this.manager.addView(var3);
      } catch (Exception var4) {
         throw new Exception("Cannot add view.", var4);
      }

      var2.put("success", L.t("View added.", new Object[0]));
      return var2.toString();
   }

   private String onUpdateView(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "viewXML")[0];
         this.manager.updateView(var3);
      } catch (Exception var4) {
         throw new Exception(L.t("Cannot update the view.", new Object[0]), var4);
      }

      var2.put("success", L.t("View updated.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveView(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         this.manager.removeView(var3);
      } catch (Exception var4) {
         throw new Exception("Error while removing view.", var4);
      }

      var2.put("success", L.t("View removed.", new Object[0]));
      return var2.toString();
   }

   protected String[] tryGetParam(Map<String, String[]> var1, String var2) throws Exception {
      if (!var1.containsKey(var2)) {
         throw new Exception("Parameter '" + var2 + "' is missing.");
      } else {
         return (String[])var1.get(var2);
      }
   }

   public DatabankTableView getViewByName(String var1) throws Exception {
      return this.manager.getView(var1);
   }
}
