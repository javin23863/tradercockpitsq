package com.strategyquant.tradinglib.databank;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.DatabankColumn;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankViewsManager {
   private static final Logger Log = LoggerFactory.getLogger(DatabankViewsManager.class);
   private static Map<String, DatabankTableView> views = new TreeMap<>();
   private static final String viewsFolder = MainApp.getDataPath() + "/user/settings/views/databanks";
   private static final String fileExtension = "vw";
   private static DatabankViewsManager instance;

   public static DatabankViewsManager getInstance() {
      if (instance == null) {
         instance = new DatabankViewsManager();
      }

      return instance;
   }

   private DatabankViewsManager() {
      this.loadViews();
   }

   private void loadViews() {
      File var1 = new File(viewsFolder);
      if (!var1.exists()) {
         var1.mkdirs();
      } else {
         File[] var2 = var1.listFiles();
         if (var2 != null) {
            for (File var6 : var2) {
               try {
                  String var7 = SQUtils.fileToString(var6);
                  this._addView(var7, false);
               } catch (Exception var8) {
                  Log.error("Cannot load databank view from file '" + var6.getName() + "'");
               }
            }
         }
      }

      views.put("Default - Main data", this.createDefaultView("Default - Main data", "main"));
      views.put("Default - Portfolio", this.createDefaultView("Default - Portfolio", "portfolio"));
   }

   public Map<String, DatabankTableView> getViews() {
      return views;
   }

   public JSONArray toJSON() {
      JSONArray var1 = new JSONArray();
      DatabankTableView var2 = views.get("Default - Main data");
      JSONObject var3 = this.getViewObject("Default - Main data", var2);
      var1.put(var3);

      for (String var5 : views.keySet()) {
         if (!var5.equals("Default - Main data")) {
            var2 = views.get(var5);
            var3 = this.getViewObject(var5, var2);
            var1.put(var3);
         }
      }

      return var1;
   }

   public JSONObject getViewObject(String var1, DatabankTableView var2) {
      JSONObject var3 = new JSONObject();
      JSONArray var4 = new JSONArray();
      var3.put("name", var1);

      for (DatabankTableColumnEntry var7 : var2.columns) {
         try {
            JSONObject var8 = new JSONObject();
            var8.put("columnName", var7.tableColumn.getName());
            var8.put("class", var7.tableColumn.getClass().getSimpleName());
            var8.put("resultType", var7.resultType);
            var8.put("sampleType", var7.sampleType);
            var8.put("direction", var7.direction);
            var8.put("plType", var7.plType);
            var8.put("confidenceLevel", var7.confidenceLevel);
            var8.put("market", var7.market);
            byte var5 = var7.subresult;
            var8.put("subresult", var5);
            var8.put("showMainResult", var7.showMainResult);
            var8.put("name", var7.getColumnEntryName());
            var8.put("width", var7.tableColumn.getWidth());
            String var9 = var7.tableColumn.getType();
            if (var5 == 31 || var5 == 32) {
               var9 = "Decimal2Pct";
            }

            var8.put("type", var9);
            var4.put(var8);
         } catch (Exception var10) {
            Log.error("Cannot load view", var10);
         }
      }

      var3.put("columns", var4);
      return var3;
   }

   public DatabankTableView getView(String var1) throws Exception {
      if (views.containsKey(var1)) {
         return views.get(var1);
      } else {
         throw new Exception(L.t("View with name '%s' doesn't exist.", new Object[]{var1}));
      }
   }

   public DatabankTableView addView(String var1) throws Exception {
      return this._addView(var1, true);
   }

   private DatabankTableView _addView(String var1, boolean var2) throws Exception {
      DatabankTableView var3 = this.getTableView(var1);
      if (views.containsKey(var3.name)) {
         throw new Exception(L.t("Databank view with name '%s' already exists.", new Object[]{var3.name}));
      }

      views.put(var3.name, var3);
      if (var2) {
         File var4 = new File(this.getViewFilePath(var3.name));
         SQUtils.stringToFile(var4, var1);
      }

      return var3;
   }

   public DatabankTableView updateView(String var1) throws Exception {
      DatabankTableView var2 = this.getTableView(var1);
      Element var3 = XMLUtil.stringToElement(var1);
      String var4 = var3.getAttributeValue("originalName");
      String var5 = var3.getAttributeValue("name");
      boolean var6 = var4 != null && !var4.equals(var5);
      if (var6) {
         views.remove(var4);
         File var7 = new File(this.getViewFilePath(var4));
         var7.delete();
      }

      if (!var6 && !views.containsKey(var2.name)) {
         throw new Exception(L.t("Databank view with name '%s' doesn't exist.", new Object[]{var2.name}));
      }

      views.put(var2.name, var2);
      File var8 = new File(this.getViewFilePath(var2.name));
      SQUtils.stringToFile(var8, var1);
      return var2;
   }

   public void removeView(String var1) throws Exception {
      if (views.containsKey(var1)) {
         views.remove(var1);
         File var2 = new File(this.getViewFilePath(var1));
         if (!var2.delete()) {
            throw new Exception(L.t("Cannot delete file '%s'", new Object[]{var2.getName()}));
         }
      } else {
         throw new Exception(L.t("Databank view with name '%s' doesn't exist.", new Object[]{var1}));
      }
   }

   public DatabankTableView createDefaultView(String var1, String var2) {
      DatabankTableView var3 = new DatabankTableView();
      var3.name = var1;
      String[] var4 = new String[]{
         "Fitness",
         "Symbol",
         "TimeFrame",
         "NetProfit",
         "MiniEquityChart",
         "NumberOfTrades",
         "PctWins",
         "ProfitFactor",
         "SharpeRatio",
         "RExpectancy",
         "AnnualPctReturn",
         "Stability",
         "Symmetry",
         "Drawdown",
         "WinLossRatio",
         "ReturnDDRatio",
         "AnnualPctReturnDDRatio",
         "AvgWin",
         "AvgLoss",
         "AvgBarsWin",
         "AvgBarsLoss",
         "AvgBarsInTrade",
         "Exposure"
      };
      List var5 = DatabankColumns.get().cloneAvailableClasses();

      for (String var9 : var4) {
         for (DatabankColumn var11 : var5) {
            if (var11.getClass().getSimpleName().equals(var9)) {
               var3.columns.add(new DatabankTableColumnEntry(var11, var2, (byte)0, (byte)10, (byte)10));
               break;
            }
         }
      }

      var4 = new String[]{
         "Fitness",
         "NetProfit",
         "MiniEquityChart",
         "NumberOfTrades",
         "PctWins",
         "ProfitFactor",
         "SharpeRatio",
         "RExpectancy",
         "AnnualPctReturn",
         "Stability",
         "Symmetry",
         "Drawdown",
         "WinLossRatio",
         "ReturnDDRatio",
         "AnnualPctReturnDDRatio",
         "AvgWin",
         "AvgLoss",
         "AvgBarsWin",
         "AvgBarsLoss",
         "AvgBarsInTrade",
         "Exposure"
      };

      for (String var16 : var4) {
         for (DatabankColumn var18 : var5) {
            if (var18.getClass().getSimpleName().equals(var16)) {
               var3.columns.add(new DatabankTableColumnEntry(var18, var2, (byte)0, (byte)10, (byte)20));
               break;
            }
         }
      }

      return var3;
   }

   private DatabankTableView getTableView(String var1) throws Exception {
      DatabankTableView var2 = new DatabankTableView();
      Element var3 = XMLUtil.stringToElement(SQUtils.removeUTF8BOM(var1));
      var2.setFromXML(var3);
      return var2;
   }

   private String getViewFilePath(String var1) {
      return viewsFolder + "/" + var1 + "." + "vw";
   }
}
