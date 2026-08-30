package com.strategyquant.plugin.DataSource.impl.Yahoo;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceYahooServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DataSourceYahooServlet.class);
   private boolean canceled = false;

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "add":
            return this.onAdd(var2);
         case "addCancel":
            return this.onAddCancel();
         case "importData":
            return this.onImportData(var2);
         case "importDataAction":
            return this.onImportDataAction(var2);
         case "updateAll":
            return this.onUpdateAll();
         case "updateSelected":
            return this.onUpdateSelected(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onAdd(final Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbols"});
      JSONObject var2 = new JSONObject();
      this.canceled = false;
      String var5 = this.tryGetParam(var1, "symbols")[0];
      ArrayList var6 = this.parseSymbols(var5);
      final ArrayList var7 = new ArrayList();

      for (int var8 = 0; var8 < var6.size(); var8++) {
         String var3 = (String)var6.get(var8);
         YahooSymbol var4 = YahooDataManager.get().downloadInfo(var3);
         var7.add(var4);
      }

      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onAdd(var1, var7);
      } else {
         (new Thread() {
            @Override
            public void run() {
               DataSourceYahooServlet.this._onAdd(var1, var7);
            }
         }).start();
      }

      var2.put("success", L.t("Add symbols.", new Object[0]));
      return var2.toString();
   }

   private void _onAdd(Map<String, String[]> var1, ArrayList<YahooSymbol> var2) {
      JSONObject var3 = new JSONObject();
      String var4 = null;

      try {
         String var6 = var1.containsKey("postfix") ? this.tryGetParamValue(var1, "postfix") : "";
         int var7 = 0;

         for (int var8 = 0; var8 < var2.size(); var8++) {
            YahooSymbol var5 = (YahooSymbol)var2.get(var8);
            String var9 = YahooDataManager.get().addData(var5.symbol, var6, (byte)1, var5.name, var5.exchange);
            DataManager.updateTimeframe("History", var9, "D1");
            Log.info(String.format("Symbol '%s' added.", var9));
            var7 = SQUtils.round(100.0 * var8 / var2.size());
            var3.put("percent", var7);
            var3.put("info", L.t("Added symbol '%s'", new Object[]{var9}));
            DataManagerAddProgressSender.getInstance().sendData(var3);
            SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var5.symbol, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            if (this.canceled) {
               break;
            }
         }
      } catch (Exception var13) {
         Log.error("Cannot add symbols. Exc.", var13);
         var4 = var13.getMessage();
      } finally {
         var3.put("percent", 100);
         var3.put("info", L.t("Completed", new Object[0]));
         if (var4 != null) {
            var3.put("error", var4);
         }

         DataManagerAddProgressSender.getInstance().sendData(var3);
      }
   }

   private String onAddCancel() throws Exception {
      JSONObject var1 = new JSONObject();
      this.canceled = true;
      var1.put("success", L.t("Adding symbols canceled.", new Object[0]));
      return var1.toString();
   }

   private ArrayList<String> parseSymbols(String var1) {
      String[] var2 = var1.split("[\\n,;]+");
      ArrayList var3 = new ArrayList();

      for (String var7 : var2) {
         var7 = var7.trim();
         if (!var7.isEmpty()) {
            var3.add(var7);
         }
      }

      return var3;
   }

   private String onImportData(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "symbols")[0].split(",");
         String[] var4 = this.tryGetParam(var1, "uSymbols")[0].split(",");
         String var5 = this.tryGetParam(var1, "dateType")[0];
         long var6 = 0L;

         try {
            var6 = SQTime.parseToMilis(this.tryGetParam(var1, "dateFrom")[0], "yyyy.MM.dd");
         } catch (Exception var20) {
         }

         long var8 = System.currentTimeMillis();

         try {
            var8 = SQTime.parseToMilis(this.tryGetParam(var1, "dateTo")[0], "yyyy.MM.dd");
         } catch (Exception var19) {
         }

         boolean var10 = this.tryGetParam(var1, "overwrite")[0].equals("true");
         String[] var11 = null;

         try {
            var11 = this.tryGetParam(var1, "datesFrom")[0].split(",");
         } catch (Exception var18) {
         }

         String[] var12 = null;

         try {
            var12 = this.tryGetParam(var1, "datesTo")[0].split(",");
         } catch (Exception var17) {
         }

         for (int var14 = 0; var14 < var3.length; var14++) {
            String var15 = var3[var14];
            if (var11 != null && var12 != null) {
               var6 = SQTime.parseToMilis(var11[var14], "yyyy.MM.dd");
               var8 = SQTime.parseToMilis(var12[var14], "yyyy.MM.dd");
            }

            ImportInfo var16 = new ImportInfo();
            var16.origSymbol = var4[var14];
            var16.symbol = var15;
            var16.overwrite = var10;
            DataInfo var13 = DataManager.getDataInfo("History", var16.symbol);
            if (var13 == null) {
               throw new Exception(L.t("Symbol with name '%s' doesn't exist.", new Object[]{var15}));
            }

            var16.instrumentInfo = var13.symbolInfo;
            var16.rows = var13.rows;
            var16.oldDateFrom = var13.dateFrom;
            var16.oldDateTo = var13.dateTo;
            var16.dateFrom = var6;
            var16.dateTo = var8;
            if (var5.equals("allTime")) {
               var16.dateFrom = 0L;
            } else if (var5.equals("sinceLast")) {
               var16.dateFrom = var13.dateTo > 0L ? SQTime.correctDayStart(var13.dateTo) : 0L;
            } else {
               var16.dateFrom = SQTime.correctDayStart(var16.dateFrom);
               var16.dateTo = SQTime.correctDayEnd(var16.dateTo);
            }

            if (SQTime.getDateInMs(var16.dateTo) == SQTime.getDateInMs(System.currentTimeMillis())) {
               var16.dateTo = SQTime.minus(var16.dateTo, 5, 1);
            }

            if (var16.dateFrom > var16.dateTo) {
               return apiErrorJSON(L.t("Date from must be before Date to.", new Object[0]), null);
            }

            DownloadDispatcher.get().update(var13, var16);
         }
      } catch (Exception var21) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var21);
      }

      var2.put("success", L.t("Data import started", new Object[0]));
      return var2.toString();
   }

   private String onImportDataAction(Map<String, String[]> var1) throws Exception {
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
            if (var4.source == 3 && var4.sourceDataId == 0) {
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
         return this.onImportData(var9);
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
            if (var5.source == 3 && var5.sourceDataId == 0) {
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
         if (var3 != null && var4 != null) {
            var17.put("datesFrom", new String[]{String.join(",", var9)});
            var17.put("datesTo", new String[]{String.join(",", var10)});
         }

         return this.onImportData(var17);
      } catch (Exception var14) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var14);
      }
   }
}
