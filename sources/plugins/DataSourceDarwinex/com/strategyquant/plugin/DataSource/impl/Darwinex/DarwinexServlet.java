package com.strategyquant.plugin.DataSource.impl.Darwinex;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.SymbolData;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.plugin.DataSource.impl.Darwinex.importdata.DarwinexImport;
import com.strategyquant.qdm.QDM;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.DataManagerProgressListener;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinexServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DarwinexServlet.class);
   private boolean canceled = false;

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "importData":
            return this.onImportData(var2);
         case "importDataCancel":
            return this.onImportDataCancel();
         case "addData":
            return this.onAddData(var2);
         case "importAction":
            return this.onImportAction(var2);
         case "importLoadAvailableSymbols":
            return this.onImportLoadAvailableSymbols(var2);
         case "downloadGetDataList":
            return this.onDownloadGetDataList();
         case "downloadAddData":
            return this.onDownloadAddData(var2);
         case "downloadData":
            return this.onDownloadData(var2);
         case "downloadDataAction":
            return this.onDownloadDataAction(var2);
         case "updateAll":
            return this.onUpdateAll();
         case "updateSelected":
            return this.onUpdateSelected(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onAddCancel() throws Exception {
      JSONObject var1 = new JSONObject();
      this.canceled = true;
      var1.put("success", L.t("Adding symbols canceled.", new Object[0]));
      return var1.toString();
   }

   private String onAddData(final Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbols[]"});
      JSONObject var2 = new JSONObject();
      this.canceled = false;
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onAdd(var1);
      } else {
         (new Thread() {
            @Override
            public void run() {
               DarwinexServlet.this._onAdd(var1);
            }
         }).start();
      }

      var2.put("success", L.t("Add symbols.", new Object[0]));
      return var2.toString();
   }

   private void _onAdd(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         String[] var4 = this.tryGetParam(var1, "symbols[]");
         String[] var5 = var1.containsKey("instruments[]") ? this.tryGetParam(var1, "instruments[]") : null;
         String var6 = var1.containsKey("postfix") ? this.tryGetParamValue(var1, "postfix") : "";
         String var7 = "TICK";
         int var8 = Integer.valueOf(this.tryGetParamValue(var1, "broker"));
         int var10 = 0;

         for (int var11 = 0; var11 < var4.length; var11++) {
            String var9 = var4[var11];
            String var12 = var5 == null ? null : var5[var11];
            if (var12 != null && var12.equals("-1")) {
               var12 = null;
            }

            String var13 = DarwinexDataManager.get().addData(var9, var6, var8, var12);
            DataManager.updateTimeframe("History", var13, var7);
            Log.info(String.format("Symbol '%s' added.", var13));
            var10 = SQUtils.round(100.0 * var11 / var4.length);
            var2.put("percent", var10);
            var2.put("info", L.t("Added symbol '%s'", new Object[]{var13}));
            DataManagerAddProgressSender.getInstance().sendData(var2);
            SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            SQWebSocketManager.addToDataQueue(WSDataObjects.getData(String.join(",", var4), "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            if (this.canceled) {
               break;
            }
         }
      } catch (Exception var17) {
         Log.error("Cannot add symbols. Exc.", var17);
         var3 = var17.getMessage();
      } finally {
         var2.put("percent", 100);
         var2.put("info", L.t("Completed", new Object[0]));
         if (var3 != null) {
            var2.put("error", var3);
         }

         DataManagerAddProgressSender.getInstance().sendData(var2);
      }
   }

   private String onImportLoadAvailableSymbols(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "path")[0];
      JSONObject var4 = new JSONObject();
      JSONArray var5 = new JSONArray();

      try {
         File var6 = new File(var2);
         File[] var7 = var6.listFiles();
         if (var7 != null) {
            for (int var8 = 0; var8 < var7.length; var8++) {
               File var3 = var7[var8];
               if (var3.isDirectory()) {
                  File[] var9 = var3.listFiles();
                  if (var9 != null) {
                     for (int var10 = 0; var10 < var9.length; var10++) {
                        if (var9[var10].getName().endsWith("log.gz")) {
                           var5.put(var3.getName());
                           break;
                        }
                     }
                  }
               }
            }

            if (var5.length() == 0) {
               String var13 = var6.getName();
               if (DarwinexDataManager.get().getAvailableDataInfo(var13) != null) {
                  for (int var14 = 0; var14 < var7.length; var14++) {
                     File var12 = var7[var14];
                     if (!var12.isDirectory() && var12.getName().endsWith("log.gz")) {
                        var5.put(var13);
                        break;
                     }
                  }
               }
            }
         }
      } catch (Exception var11) {
         return apiErrorJSON(L.t("Cannot load available symbols.", new Object[0]), var11);
      }

      var4.put("symbols", var5);
      var4.put("success", L.t("Symbols loaded.", new Object[0]));
      return var4.toString();
   }

   private String onImportData(final Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbols", "path"});
      JSONObject var2 = new JSONObject();
      this.canceled = false;
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onImportData(var1);
      } else {
         (new Thread() {
            @Override
            public void run() {
               DarwinexServlet.this._onImportData(var1);
            }
         }).start();
      }

      var2.put("success", L.t("Add darwinex data.", new Object[0]));
      return var2.toString();
   }

   private String onImportDataCancel() throws Exception {
      JSONObject var1 = new JSONObject();
      this.canceled = true;
      var1.put("success", L.t("Adding symbols canceled.", new Object[0]));
      return var1.toString();
   }

   private void _onImportData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         String var4 = this.tryGetParam(var1, "symbols")[0];
         String var5 = var1.containsKey("postfix") ? this.tryGetParam(var1, "postfix")[0] : "";
         String var6 = this.tryGetParam(var1, "path")[0];
         String[] var7 = var4.split(",");
         if (var7.length == 1) {
            File var8 = new File(new File(var6), var7[0]);
            if (!var8.exists()) {
               var6 = new File(var6).getParentFile().getAbsolutePath();
            }
         }

         int var9 = 0;

         for (int var10 = 0; var10 < var7.length; var10++) {
            String var22 = var7[var10];

            try {
               String var11 = DarwinexDataManager.get().addData(var22, var5, -1, null);
               Log.info(String.format("Symbol '%s' added.", var11));
               ImportInfo var12 = new ImportInfo();
               var12.origSymbol = var22;
               var12.symbol = var11;
               var12.overwrite = true;
               DataInfo var13 = DataManager.getDataInfo("History", var12.symbol);
               var12.instrumentInfo = var13.symbolInfo;
               var12.rows = var13.rows;
               var12.oldDateFrom = var13.dateFrom;
               var12.oldDateTo = var13.dateTo;
               var12.priceConstant = Math.pow(10.0, -var13.decimals);
               DataManagerProgressListener var14 = (DataManagerProgressListener)DataManagerDataProgress.get().createListener(var11, "darwinex/importAction");
               DarwinexImport.get().importData(var12, var6, var14);
               var9 = SQUtils.round(100.0 * var10 / var7.length);
               var2.put("percent", var9);
               var2.put("info", L.t("Added symbol '%s'", new Object[]{var11}));
               DataManagerAddProgressSender.getInstance().sendData(var2);
               SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
               SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var4, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
               if (this.canceled) {
                  break;
               }
            } catch (Exception var19) {
               var3 = L.t("Some symbols cannot be added.", new Object[0]) + " " + var19.getMessage();
               Log.error(L.t("Cannot add symbol %s. Reason: %s", new Object[]{var22, var19.getMessage()}), var19);
            }
         }
      } catch (Exception var20) {
         Log.error("Cannot add symbols. Exc.", var20);
         var3 = var20.getMessage();
      } finally {
         var2.put("percent", 100);
         var2.put("info", L.t("Completed", new Object[0]));
         if (var3 != null) {
            var2.put("error", var3);
         }

         DataManagerAddProgressSender.getInstance().sendData(var2);
      }
   }

   private String onImportAction(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      switch (var3) {
         case "stop":
            DarwinexImport.get().stop(var2);
            break;
         case "pause":
            DarwinexImport.get().pause(var2);
            break;
         case "continue":
            DarwinexImport.get().restart(var2);
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private String onDownloadGetDataList() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (SymbolData var4 : DarwinexDataManager.get().getAvailableDataList()) {
         if (MainApp.v571hfnsHw().a1wUchdumV() || MainApp.v571hfnsHw().xpoHYYsX() || DarwinexDataManager.get().canFreeDownloadFromCdn(var4.symbol)) {
            JSONObject var5 = new JSONObject();
            var5.put("symbol", var4.symbol);
            var5.put("dateFrom", var4.dateFrom);
            var5.put("decimals", var4.decimals);
            var5.put("tickValue", var4.tickValue);
            var5.put("defaultSpread", var4.defaultSpread);
            var5.put("tickSize", var4.tickSize);
            var5.put("tickStep", var4.tickStep);
            var5.put("instrumentType", var4.instrumentType);
            var2.put(var5);
         }
      }

      var1.put("data", var2);
      var1.put("success", L.t("Data listed", new Object[0]));
      return var1.toString();
   }

   private String onDownloadAddData(final Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onDownloadAddData(var1);
      } else {
         (new Thread() {
            @Override
            public void run() {
               DarwinexServlet.this._onDownloadAddData(var1);
            }
         }).start();
      }

      var2.put("success", L.t("Data added", new Object[0]));
      return var2.toString();
   }

   private String _onDownloadAddData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      double var3 = 0.0;

      try {
         String var5 = var1.containsKey("postfix") ? this.tryGetParam(var1, "postfix")[0] : "";
         String[] var6 = this.tryGetParam(var1, "symbols[]");
         int var7 = Integer.valueOf(this.tryGetParamValue(var1, "broker"));
         String[] var8 = var1.containsKey("instruments[]") ? this.tryGetParam(var1, "instruments[]") : null;

         for (int var9 = 0; var9 < var6.length; var9++) {
            String var10 = var6[var9];
            if (!MainApp.v571hfnsHw().a1wUchdumV() && !MainApp.v571hfnsHw().xpoHYYsX() && !DarwinexDataManager.get().canFreeDownloadFromCdn(var10)) {
               throw new RuntimeException(L.t("Darwinex CDN download is available only for users of full license.", new Object[0]));
            }

            String var11 = var8 == null ? null : var8[var9];
            if (var11 != null && var11.equals("-1")) {
               var11 = null;
            }

            String var12 = DarwinexDataManager.get().addData(var10, var5, var7, var11);
            DataManager.updateTimeframe("History", var12, "TICK");
            var3 = SQUtils.round(100.0 * var9 / var6.length).intValue();
            var2.put("percent", var3);
            var2.put("info", L.t("Added symbol '%s'", new Object[]{var10}));
            DataManagerAddProgressSender.getInstance().sendData(var2);
            SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var10, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            if (this.canceled) {
               break;
            }
         }
      } catch (Exception var16) {
         return apiErrorJSON(L.t("Cannot add data.", new Object[0]), var16);
      } finally {
         var2.put("percent", 100);
         var2.put("info", L.t("Completed", new Object[0]));
         DataManagerAddProgressSender.getInstance().sendData(var2);
      }

      var2.put("success", L.t("Data added", new Object[0]));
      return var2.toString();
   }

   private String onDownloadData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "symbols")[0].split(",");
         long var4 = SQTime.parseToMilis(this.tryGetParam(var1, "dateFrom")[0], "yyyy.MM.dd");
         long var6 = SQTime.parseToMilis(this.tryGetParam(var1, "dateTo")[0], "yyyy.MM.dd");
         String var8 = this.tryGetParam(var1, "dateType")[0];
         boolean var9 = this.tryGetParam(var1, "overwrite")[0].equals("true");
         String[] var10 = null;

         try {
            var10 = this.tryGetParam(var1, "datesFrom")[0].split(",");
         } catch (Exception var20) {
         }

         String[] var11 = null;

         try {
            var11 = this.tryGetParam(var1, "datesTo")[0].split(",");
         } catch (Exception var19) {
         }

         new HashMap();
         new HashMap();

         for (int var14 = 0; var14 < var3.length; var14++) {
            String var15 = var3[var14];
            DataInfo var16 = DataManager.getDataInfo("History", var15);
            if (var16.source == 4) {
               if (!MainApp.v571hfnsHw().a1wUchdumV() && !MainApp.v571hfnsHw().xpoHYYsX() && !DarwinexDataManager.get().canFreeDownloadFromCdn(var16.uSymbol)) {
                  throw new RuntimeException(L.t("Darwinex CDN download is available only for users of full license.", new Object[0]));
               }

               if (var10 != null && var11 != null) {
                  var4 = SQTime.parseToMilis(var10[var14], "yyyy.MM.dd");
                  var6 = SQTime.parseToMilis(var11[var14], "yyyy.MM.dd");
               }

               ImportInfo var17 = new ImportInfo();
               var17.symbol = var15;
               var17.overwrite = var9;
               var17.origSymbol = var16.uSymbol;
               var17.instrumentInfo = var16.symbolInfo;
               var17.rows = var16.rows;
               var17.oldDateFrom = var16.dateFrom;
               var17.oldDateTo = var16.dateTo;
               var17.ignoreWeekend = var16.symbolInfo.dataType == 3;
               SymbolData var18 = DarwinexDataManager.get().getAvailableDataInfo(var17.origSymbol);
               if (var18 == null) {
                  throw new Exception(L.t("No info found for symbol '%s'.", new Object[]{var17.origSymbol}));
               }

               var17.priceConstant = Math.pow(10.0, -var18.decimals);
               var17.downloadM1Data = false;
               var17.dateFrom = var4;
               var17.dateTo = var6;
               var17.useCDN = true;
               if (var8.equals("allTime")) {
                  var17.dateFrom = var18.dateFrom;
               } else if (var8.equals("sinceLast")) {
                  var17.dateFrom = var16.dateTo > 0L ? var16.dateTo : var18.dateFrom;
               }

               if (var17.dateFrom > var17.dateTo) {
                  return apiErrorJSON(L.t("Date from must be before Date to.", new Object[0]), null);
               }

               QDM.getInstance().activity.increase(3);
               DownloadDispatcher.get().update(var16, var17);
            }
         }
      } catch (Exception var21) {
         return apiErrorJSON(L.t("Cannot import data.", new Object[0]), var21);
      }

      var2.put("success", L.t("Data import started", new Object[0]));
      return var2.toString();
   }

   private String onDownloadDataAction(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbol", "action"});
      String var2 = this.tryGetParam(var1, "symbol")[0];
      String var3 = this.tryGetParam(var1, "action")[0];
      JSONObject var4 = new JSONObject();
      MultiProgressListener var5 = DataManagerDataProgress.get().createListener(var2);
      switch (var3) {
         case "stop":
            DownloadDispatcher.get().stop(var2);
            break;
         case "pause":
            var5.onPause();
            DownloadDispatcher.get().pause(var2);
            break;
         case "continue":
            var5.onContinue();
            DownloadDispatcher.get().resume(var2);
      }

      var4.put("success", "ok");
      return var4.toString();
   }

   private String onUpdateAll() {
      try {
         ArrayList var1 = DataManager.list();
         ArrayList var2 = new ArrayList();

         for (int var3 = 0; var3 < var1.size(); var3++) {
            DataInfo var4 = (DataInfo)var1.get(var3);
            if (var4.source == 4 && var4.sourceDataId == 0) {
               var2.add((DataInfo)var1.get(var3));
            }
         }

         if (var2.isEmpty()) {
            return "No symbols to update";
         }

         HashMap var9 = new HashMap();
         String[] var10 = new String[var2.size()];
         String[] var5 = new String[var2.size()];

         for (int var6 = 0; var6 < var2.size(); var6++) {
            DataInfo var7 = (DataInfo)var2.get(var6);
            var10[var6] = var7.symbol;
            var5[var6] = var7.uSymbol;
         }

         var9.put("symbols", new String[]{String.join(",", var10)});
         var9.put("uSymbols", new String[]{String.join(",", var5)});
         var9.put("dateFrom", new String[]{"2013.01.01"});
         var9.put("dateTo", new String[]{SQTime.toUIDateString(System.currentTimeMillis())});
         var9.put("dateType", new String[]{"sinceLast"});
         var9.put("overwrite", new String[]{"true"});
         var9.put("dataType", new String[]{"M1"});
         return this.onDownloadData(var9);
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var8);
      }
   }

   private String onUpdateSelected(Map<String, String[]> var1) {
      try {
         String[] var2 = this.tryGetParam(var1, "symbols")[0].split(",");
         String[] var3 = null;

         try {
            var3 = this.tryGetParam(var1, "datesFrom")[0].split(",");
         } catch (Exception var13) {
         }

         String[] var4 = null;

         try {
            var4 = this.tryGetParam(var1, "datesTo")[0].split(",");
         } catch (Exception var12) {
         }

         ArrayList var6 = new ArrayList();

         for (int var7 = 0; var7 < var2.length; var7++) {
            DataInfo var5 = DataManager.getDataInfo("History", var2[var7]);
            if (var5.source == 4 && var5.sourceDataId == 0) {
               var5.dateFromStr = var3 == null ? null : var3[var7];
               var5.dateToStr = var4 == null ? null : var4[var7];
               var6.add(var5);
            }
         }

         if (var6.isEmpty()) {
            return "No symbols to update";
         }

         HashMap var17 = new HashMap();
         var2 = new String[var6.size()];
         String[] var8 = new String[var6.size()];
         String[] var9 = new String[var6.size()];
         String[] var10 = new String[var6.size()];

         for (int var11 = 0; var11 < var6.size(); var11++) {
            DataInfo var16 = (DataInfo)var6.get(var11);
            var2[var11] = var16.symbol;
            var8[var11] = var16.uSymbol;
            var9[var11] = var16.dateFromStr;
            var10[var11] = var16.dateToStr;
         }

         var17.put("symbols", new String[]{String.join(",", var2)});
         var17.put("uSymbols", new String[]{String.join(",", var8)});
         var17.put("dateFrom", new String[]{"2013.01.01"});
         var17.put("dateTo", new String[]{SQTime.toUIDateString(System.currentTimeMillis())});
         var17.put("dateType", new String[]{"sinceLast"});
         var17.put("overwrite", new String[]{"true"});
         var17.put("dataType", new String[]{"M1"});
         if (var3 != null && var4 != null) {
            var17.put("datesFrom", new String[]{String.join(",", var9)});
            var17.put("datesTo", new String[]{String.join(",", var10)});
         }

         return this.onDownloadData(var17);
      } catch (Exception var14) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var14);
      }
   }
}
