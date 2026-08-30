package com.strategyquant.plugin.DataSource.impl.Crypto;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.dukascopy.ImportInfo;
import com.strategyquant.tradinglib.exchange.IExchange;
import com.strategyquant.tradinglib.exchange.SymbolInfo;
import com.strategyquant.tradinglib.project.console.CLILogger;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceCryptoServlet extends HttpJSONServlet {
   public static final String DOWNLOAD_JOB = "DownloadCryptoSymbolJob_";
   private static final Logger Log = LoggerFactory.getLogger(DataSourceCryptoServlet.class);
   private boolean canceled = false;

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getExchanges":
            return this.onGetExchanges(var2);
         case "getSymbols":
            return this.onGetSymbols(var2);
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

   private String onGetExchanges(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();

      for (IExchange var5 : SQPluginManager.getPlugins(IExchange.class)) {
         JSONObject var6 = new JSONObject();
         var6.put("name", var5.getName());
         var6.put("value", SQPluginManager.getPluginName(var5));
         var3.put(var6);
      }

      var2.put("exchanges", var3);
      var2.put("success", L.t("Exchanges listed", new Object[0]));
      return var2.toString();
   }

   private String onGetSymbols(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "exchange")[0];
      IExchange var3 = (IExchange)SQPluginManager.findPluginByName(var2);
      if (var3 == null) {
         throw new Exception(L.t("Cannot find plugin for '%s'.", new Object[]{var2}));
      }

      JSONObject var4 = new JSONObject();
      var4.put("symbols", var3.getSymbols());
      var4.put("timeframes", var3.availableTimeframes());
      var4.put("success", L.t("Symbols listed", new Object[0]));
      return var4.toString();
   }

   private String onAdd(final Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbols"});
      JSONObject var2 = new JSONObject();
      this.canceled = false;
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onAdd(var1);
      } else {
         (new Thread() {
            @Override
            public void run() {
               DataSourceCryptoServlet.this._onAdd(var1);
            }
         }).start();
         var2.put("success", L.t("Add symbols.", new Object[0]));
      }

      return var2.toString();
   }

   private String onAddCancel() throws Exception {
      JSONObject var1 = new JSONObject();
      this.canceled = true;
      var1.put("success", L.t("Adding symbols canceled.", new Object[0]));
      return var1.toString();
   }

   private void _onAdd(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;
      LinkedList var5 = new LinkedList();
      int var6 = 0;

      try {
         String var7 = this.tryGetParam(var1, "exchange")[0];
         String[] var8 = this.tryGetParam(var1, "symbols")[0].split(",");
         String var9 = this.tryGetParam(var1, "timeframe")[0];
         String var10 = var1.containsKey("postfix") ? this.tryGetParamValue(var1, "postfix") : "";
         IExchange var11 = (IExchange)SQPluginManager.findPluginByName(var7);
         if (var11 == null) {
            throw new Exception(L.t("Cannot find plugin for '%s'.", new Object[]{var7}));
         }

         Log.info(String.format("Checking timeframe '%s'.", var9));
         boolean var12 = false;
         JSONArray var13 = var11.availableTimeframes();

         for (int var14 = 0; var14 < var13.length(); var14++) {
            if (var9.equals(var13.getString(var14))) {
               var12 = true;
               break;
            }
         }

         if (!var12) {
            throw new Exception(L.t("Timeframe '%s' not supported.", new Object[]{var9}));
         }

         int var24 = 0;

         for (int var15 = 0; var15 < var8.length; var15++) {
            String var4 = var8[var15];
            Log.info(String.format("Checking symbol '%s'.", var4));
            if (var15 > 0) {
               Thread.sleep(500L);
            }

            if (!var11.checkSymbolExists(var4)) {
               Log.error("Unknown symbol '" + var4 + "'. It wasn't possible to check selected symbol on crypto exchange.");
               var5.add(var4);
            } else {
               try {
                  String var16 = this.addData(var4, var10, var11);
                  DataManager.updateTimeframe("History", var16, var9);
                  Log.info(L.t("Symbol '%s' added.", new Object[]{var16}));
                  var24 = SQUtils.round(100.0 * var15 / var8.length);
                  var2.put("percent", var24);
                  var2.put("info", L.t("Added symbol '%s'", new Object[]{var16}));
                  DataManagerAddProgressSender.getInstance().sendData(var2);
                  SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                  SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var4, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
                  var6++;
               } catch (Exception var21) {
                  Log.error("Error while adding symbol " + var4, var21);
                  var5.add(var4);
               }

               if (this.canceled) {
                  break;
               }
            }
         }
      } catch (Exception var22) {
         Log.error("Cannot add symbols. Exc.", var22);
         var3 = var22.getMessage();
         if (MainApp.runInConsoleWithoutActiveWebserver()) {
            CLILogger.log("Cannot add symbols. " + var3);
         }
      } finally {
         var2.put("percent", 100);
         var2.put("info", L.t("Completed", new Object[0]));
         if (!var5.isEmpty()) {
            var2.put("error", "Error while adding symbols: " + String.join(", ", var5));
         }

         if (var3 != null) {
            var2.put("error", var3);
         }

         DataManagerAddProgressSender.getInstance().sendData(var2);
      }
   }

   private String addData(String var1, String var2, IExchange var3) throws Exception {
      String var4 = var3.getName();
      SymbolInfo var5 = var3.getSymbolInfo(var1);
      double var6 = var5.getPointValue();
      double var8 = var5.getTickSize();
      double var10 = var5.getTickStep();
      double var12 = var5.getDefaultSpread();
      double var14 = var5.getDefaultSlippage();
      String var16 = var1 + var2;
      String var17 = var1;

      while (DataManager.checkDataExists("History", var16)) {
         var16 = DataManager.generateName(var16);
      }

      if (!InstrumentManager.checkInstrumentExists(var17)) {
         InstrumentManager.addInstrument(
            var17,
            "Crypto instrument",
            var6,
            var8,
            var10,
            var12,
            var14,
            "<Method type=\"None\" use=\"true\"><Params/></Method>",
            (byte)7,
            null,
            null,
            null,
            null,
            1.0,
            0.0
         );
      } else {
         InstrumentInfo var18 = InstrumentManager.getInstrumentInfo(var17);
         if (var18.tickSize != var8 || var18.tickStep != var10) {
            while (InstrumentManager.checkInstrumentExists(var17)) {
               var17 = DataManager.generateName(var17);
            }

            InstrumentManager.addInstrument(
               var17,
               "Crypto instrument",
               var6,
               var8,
               var10,
               var12,
               var14,
               "<Method type=\"None\" use=\"true\"><Params/></Method>",
               (byte)7,
               null,
               null,
               null,
               null,
               1.0,
               0.0
            );
         }
      }

      DataManager.addData("History", var16, var17, 1, 7, var1, var4);
      return var16;
   }

   private String onImportData(Map<String, String[]> var1) {
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

   private String getJobId(String var1) {
      return "DownloadCryptoSymbolJob_" + var1;
   }

   private String onUpdateAll() {
      try {
         ArrayList var1 = DataManager.list();
         ArrayList var2 = new ArrayList();

         for (int var3 = 0; var3 < var1.size(); var3++) {
            DataInfo var4 = (DataInfo)var1.get(var3);
            if (var4.source == 7 && var4.sourceDataId == 0) {
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
         var9.put("useCDN", new String[]{"true"});
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
            if (var5.source == 7 && var5.sourceDataId == 0) {
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
         var17.put("useCDN", new String[]{"true"});
         if (var3 != null && var4 != null) {
            var17.put("datesFrom", new String[]{String.join(",", var9)});
            var17.put("datesTo", new String[]{String.join(",", var10)});
         }

         return this.onImportData(var17);
      } catch (Exception var14) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var14);
      }
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
}
