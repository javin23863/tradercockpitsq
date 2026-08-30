package com.strategyquant.plugin.DataSource.impl.Mt5Api;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.mt5api.Mt5ApiImportInfo;
import com.strategyquant.tradinglib.mt5api.Mt5ApiManager;
import com.strategyquant.tradinglib.mt5api.Mt5ApiManager.Source;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceMt5ApiServlet extends HttpJSONServlet {
   private static final long serialVersionUID = 1L;
   DateTimeFormatter formaterDate = DateTimeFormat.forPattern("yyyyMMdd");
   private static final Logger Log = LoggerFactory.getLogger(DataSourceMt5ApiServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "importData":
            return this.onImportData(var2);
         case "downloadDataAction":
            return this.onImportAction(var2);
         case "loadAvailableSymbols":
            return this.onLoadAvailableSymbols(var2);
         case "updateSelected":
            return this.updateSelected(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Incorrect url arguments '%s'.", new Object[]{var1}), null);
      }
   }

   private String updateSelected(Map<String, String[]> var1) throws Exception {
      try {
         String[] var2 = this.tryGetParam(var1, "symbols")[0].split(",");

         for (int var3 = 0; var3 < var2.length; var3++) {
            String var4 = var2[var3];
            DataInfo var5 = DataManager.getDataInfo("History", var4);
            if (var5 == null) {
               Log.error(String.format("Ticker '%s' not found. Failed to update data.", var4));
            } else {
               Mt5ApiImportInfo var6 = new Mt5ApiImportInfo();
               var6.origSymbol = var5.uSymbol;
               var6.symbol = var5.symbol;
               DownloadDispatcher.get().update(var5, var6);
            }
         }
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var7);
      }

      JSONObject var8 = new JSONObject();
      var8.put("success", L.t("Update started.", new Object[0]));
      return var8.toString();
   }

   private String onLoadAvailableSymbols(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = "portable";
      String var4 = this.getParam(var1, "path", null);
      List var5 = Mt5ApiManager.get().getSymbolListJson(Source.valueOf(var3), var4);
      if (var5.isEmpty()) {
         apiErrorJSON("Error while fetching symbols list", null);
      }

      var5.sort(new Comparator<JSONObject>() {
         public int compare(JSONObject var1, JSONObject var2x) {
            int var3x = var1.getString("path").compareTo(var2x.getString("path"));
            return var3x != 0 ? var3x : var1.getString("name").compareTo(var2x.getString("name"));
         }
      });
      var2.put("symbols", var5);
      var2.put("success", L.t("Symbols loaded.", new Object[0]));
      return var2.toString();
   }

   private String onImportData(final Map<String, String[]> var1) throws Exception {
      if (!var1.containsKey("symbols")) {
         return apiErrorJSON(L.t("No symbols were specified", new Object[0]), null);
      }

      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onImportData(var1);
      } else {
         (new Thread() {
            @Override
            public void run() {
               DataSourceMt5ApiServlet.this._onImportData(var1);
            }
         }).start();
      }

      JSONObject var2 = new JSONObject();
      var2.put("success", L.t("Data import started", new Object[0]));
      return var2.toString();
   }

   private void _onImportData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "symbols")[0];
         String var4 = var1.containsKey("postfix") ? this.tryGetParam(var1, "postfix")[0] : "";
         String var5 = "M1";
         int var6 = Integer.valueOf(this.tryGetParamValue(var1, "broker"));
         String[] var7 = var3.split(",");
         String var8 = "portable";
         String var9 = this.getParam(var1, "path", null);
         LinkedList var10 = new LinkedList();

         for (int var11 = 0; var11 < var7.length; var11++) {
            String var12 = var7[var11];
            String var13 = this.addData(Source.valueOf(var8), var9, var12, var4, var6, var5);
            Integer var14 = SQUtils.round(100.0 * var11 / var7.length);
            var2.put("percent", var14);
            var2.put("info", L.t("Adding symbol '%s'", new Object[]{var12}));
            DataManagerAddProgressSender.getInstance().sendData(var2);
            Mt5ApiImportInfo var15 = new Mt5ApiImportInfo();
            var15.origSymbol = var12;
            var15.symbol = var13;
            var15.path = var9;
            if (var1.containsKey("dateTo")) {
               var15.dateTo = SQTime.parseToMilis(this.tryGetParam(var1, "dateTo")[0], "yyyy.MM.dd");
            } else {
               var15.dateTo = System.currentTimeMillis();
            }

            if (var1.containsKey("dateFrom")) {
               var15.dateFrom = SQTime.parseToMilis(this.tryGetParam(var1, "dateFrom")[0], "yyyy.MM.dd");
            } else {
               var15.dateFrom = SQTime.addYears(var15.dateTo, -1);
            }

            var10.add(var15);
            var2.put("percent", var14);
            var2.put("info", L.t("Added symbol '%s'", new Object[]{var12}));
            DataManagerAddProgressSender.getInstance().sendData(var2);
            SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var12, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var2.put("percent", 100);
         var2.put("info", L.t("Starting to download", new Object[0]));
         DataManagerAddProgressSender.getInstance().sendData(var2);
         Log.info("Starting to download {} symbols", var10.size());

         for (Mt5ApiImportInfo var18 : var10) {
            DataInfo var19 = DataManager.getDataInfo("History", var18.symbol);
            DownloadDispatcher.get().update(var19, var18);
         }
      } catch (Exception var16) {
         Log.error("Error while adding symbol", var16);
         var2.put("percent", 100);
         var2.put("info", L.t("Completed", new Object[0]));
         var2.put("error", var16.getMessage());
         DataManagerAddProgressSender.getInstance().sendData(var2);
      }
   }

   private String addData(Source var1, String var2, String var3, String var4, int var5, String var6) throws Exception {
      BrokerDto var7 = BrokerManager.getInstance().getBroker(var5);
      if (var7 == null) {
         throw new RuntimeException("Specified broker was not found");
      }

      String var8 = var3 + var4;
      String var9 = var3 + var4;

      while (DataManager.checkDataExists("History", var8)) {
         var8 = DataManager.generateName(var8);
      }

      Map var10 = Mt5ApiManager.get().getSymbolInfoJson(var1, var2, var3);
      double var11 = 0.0;
      byte var13 = 3;
      String var14 = Mt5ApiManager.get().getDescription(var10);
      if (var14.length() > 200) {
         var14 = var14.substring(0, 200);
      }

      var10 = Mt5ApiManager.get().getPriceSymbolInfoJson(var1, var2, var3);
      double var15 = Mt5ApiManager.get().getTickSize(var10);
      double var17 = Mt5ApiManager.get().getTickStep(var10);
      double var19 = Mt5ApiManager.get().getPointValue(var10);
      double var21 = Mt5ApiManager.get().getDefaultSpread(var10);
      if (!InstrumentManager.checkInstrumentExists(var9)) {
         InstrumentManager.addInstrument(
            var9,
            "Mt5 Api instrument - " + var14,
            var19,
            var15,
            var17,
            var21,
            var11,
            "<Method type=\"None\" use=\"true\"><Params/></Method>",
            var13,
            null,
            null,
            null,
            null,
            1.0,
            0.0
         );
      } else {
         InstrumentInfo var23 = InstrumentManager.getInstrumentInfo(var9);
         if (var23.tickSize != var15 || var23.tickStep != var17) {
            while (InstrumentManager.checkInstrumentExists(var9)) {
               var9 = DataManager.generateName(var9);
            }

            InstrumentManager.addInstrument(
               var9,
               var5,
               "MT5 Api instrument",
               var19,
               var15,
               var17,
               var21,
               var11,
               0.0,
               "<Method type=\"None\" use=\"true\"><Params/></Method>",
               var13,
               null,
               null,
               null,
               null,
               1.0,
               0.0
            );
         }
      }

      DataManager.addData("History", var8, var9, 1, 8, var3, var3, -1, var5);
      DataManager.updateTimeframe("History", var8, var6);
      return var8;
   }

   private String onImportAction(Map<String, String[]> var1) throws Exception {
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
