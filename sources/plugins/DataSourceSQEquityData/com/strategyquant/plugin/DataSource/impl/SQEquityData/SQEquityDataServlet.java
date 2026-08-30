package com.strategyquant.plugin.DataSource.impl.SQEquityData;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.BatchProgressController;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DownloadSemaphore;
import com.strategyquant.datalib.data.DukasDataManager;
import com.strategyquant.datalib.data.InstrumentValueEvaluator;
import com.strategyquant.datalib.data.StockGroupUpdateErrorManager;
import com.strategyquant.datalib.historyData.TickerFilterDto;
import com.strategyquant.datalib.historyData.dto.TickerDto;
import com.strategyquant.datalib.historyData.dto.TickerKind;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.historyData.HistoryDataSubscription;
import com.strategyquant.model.IdName;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.historyData.HistoryDataManager;
import com.strategyquant.tradinglib.historyData.MarketDtoToJsonTransformer;
import com.strategyquant.tradinglib.historyData.TickerDtoToJsonTransformer;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.project.websocket.WebSocketNotification;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQEquityDataServlet extends HttpJSONServlet {
   private static final String JOB_PREFIX = "HistoryDownloadJob_";
   private static final Logger Log = LoggerFactory.getLogger(SQEquityDataServlet.class);
   private boolean canceled = false;
   private String exchangesResponse = null;

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getExchanges":
            return this.onGetExchanges();
         case "add":
            return this.onAdd(var2);
         case "addCancel":
            return this.onAddCancel();
         case "lookup":
            return this.onLookup(var2);
         case "update":
            return this.onUpdate();
         case "verifySubscription":
            return this.onVerifySubscription(var2);
         case "updateDataAction":
            return this.onUpdateDataAction(var2);
         case "updateAll":
            return this.onUpdateAll();
         case "updateSelected":
            return this.onUpdateSelected(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onGetExchanges() throws Exception {
      if (this.exchangesResponse != null) {
         return this.exchangesResponse;
      }

      JSONObject var1 = new JSONObject();
      List var2 = HistoryDataManager.get().getStockMarkets();
      var2.add(0, new IdName(0L, L.t("Any", new Object[0])));
      JSONArray var3 = new MarketDtoToJsonTransformer().toJson(var2);
      var1.put("exchanges", var3);
      var1.put("success", L.t("Exchanges listed.", new Object[0]));
      this.exchangesResponse = var1.toString();
      return this.exchangesResponse;
   }

   private String onAdd(final Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbols", "barType"});
      JSONObject var2 = new JSONObject();
      this.canceled = false;
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onAdd(var1);
      } else {
         (new Thread() {
            @Override
            public void run() {
               SQEquityDataServlet.this._onAdd(var1);
            }
         }).start();
      }

      var2.put("success", L.t("Add symbols.", new Object[0]));
      return var2.toString();
   }

   private void _onAdd(Map<String, String[]> var1) {
      final JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         String var4 = this.tryGetParamValue(var1, "symbols");
         final String[] var5 = var4.equals("") ? new String[0] : var4.split(",");
         String var6 = var1.containsKey("postfix") ? this.tryGetParamValue(var1, "postfix") : "";
         int var7 = Integer.parseInt(this.tryGetParam(var1, "barType")[0]);
         int var8 = Integer.parseInt(this.getParam(var1, "timezoneType", "2"));
         int var9 = Integer.parseInt(this.getParam(var1, "timezoneShift", "0"));
         String var10 = this.getParam(var1, "timezone", "EETUS");
         String var11 = this.getParam(var1, "updateSymbols", null);
         Object var12 = null;
         if (var8 == 2) {
            var12 = "Exchange";
         } else if (var8 == 1) {
            var12 = var10;
         } else {
            var12 = "Exchange " + (var9 < 0 ? var9 : "+" + var9);
         }

         TickerFilterDto var13 = new TickerFilterDto();
         var13.setSearchInTicker(true);
         var13.setExactMatch(true);
         var13.setNames(var5);
         List var14 = HistoryDataManager.get().getStockTickers(var13);
         LinkedList var15 = new LinkedList();
         LinkedList var16 = new LinkedList();

         for (TickerDto var18 : var14) {
            if (var18.isEod()) {
               var15.add(var18.getTicker());
            } else {
               var16.add(var18.getTicker());
            }
         }

         this.subscriptionCheck(var15.toArray(new String[0]), true);
         this.subscriptionCheck(var16.toArray(new String[0]), false);
         Map var31 = var14.stream().collect(Collectors.toMap(TickerDto::getTicker, var0 -> (TickerDto)var0));
         ArrayList var32 = new ArrayList(var5.length);

         for (int var20 = 0; var20 < var5.length; var20++) {
            String var19 = var5[var20];
            TickerDto var21 = (TickerDto)var31.get(var19);
            if (var21 == null) {
               Log.error(String.format("Ticker '%s' not found. Failed to add data.", var19));
               StockGroupUpdateErrorManager.getInstance().logError(var19, "Ticker '" + var19 + "' not found. Failed to add data.");
            } else {
               DataInfo var22 = new DataInfo();
               var32.add(var22);
               var22.barTimeType = var7;
               var22.timezone = (String)var12;
               var22.connection = "History";
               var22.symbol = var19 + var6;
               var22.source = 5;
               String var23 = var21.getMarketName().replaceAll("Common ", "").trim() + " " + var21.getType();
               var22.instrument = var23;
               var22.uSymbol = var21.getTicker();
            }
         }

         int var33 = var5.length;
         int var34 = var33 / 20;
         if (var34 < 10) {
            var34 = 10;
         }

         final HashSet var35 = new HashSet();
         final int var36 = var34;
         DukasDataManager.get().addBatch(var32, var31, new InstrumentValueEvaluator() {
            public double getTickStep(TickerDto var1) {
               return 1.0E-4;
            }

            public double getTickSize(TickerDto var1) {
               return 0.01;
            }

            public double getPointValue(TickerDto var1) {
               return 1.0;
            }

            public byte getInstrumentType(TickerDto var1) {
               return (byte)(var1.getType().contains("ETF") ? 5 : 1);
            }

            public String getDescriptions(TickerDto var1) {
               return "History data instrument";
            }

            public double getOrderSizeMultiplier(TickerDto var1) {
               return 1.0;
            }

            public double getOrderSizeStep(TickerDto var1) {
               return 0.0;
            }
         }, new BatchProgressController() {
            public void updateProgress(int var1, int var2x, String var3x) throws Exception {
               if (var1 % var36 == 0) {
                  Integer var4x = SQUtils.round(100.0 * var1 / var2x);
                  var2.put("percent", var4x);
                  var2.put("info", L.t("Added symbol '%s'", new Object[]{var3x}));
                  DataManagerAddProgressSender.getInstance().sendData(var2);
                  var35.clear();
                  StringBuilder var5x = new StringBuilder();

                  for (int var6x = var1 - var36; var6x <= var1 && var6x < var5.length; var6x++) {
                     var35.add(var5[var6x]);
                     if (var5x.length() != 0) {
                        var5x.append(",");
                     }

                     var5x.append(var5[var6x]);
                  }

                  SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var35, var5x.toString(), "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
               }
            }

            public boolean isCancel() {
               return SQEquityDataServlet.this.canceled;
            }

            public void finished() {
            }
         });
         if (var11 != null) {
            WebSocketNotification.sendSuccess(
               L.t("Symbols %s added to databank. Starting to download data.", new Object[]{var32.size()}), new String[]{"QDM", "SQUANT"}
            );
            HashMap var24 = new HashMap();
            var24.put("symbols", new String[]{var11});
            this.onUpdateSelected(var24);
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var4, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var28) {
         Log.error("Cannot add symbols. Exc.", var28);
         var3 = var28.getMessage();
      } finally {
         DownloadSemaphore.getInstance().downloadFinished();
         var2.put("percent", 100);
         var2.put("info", L.t("Completed", new Object[0]));
         if (var3 != null) {
            var2.put("error", var3);
         }

         DataManagerAddProgressSender.getInstance().sendData(var2);
      }
   }

   private String onAddCancel() throws Exception {
      JSONObject var1 = new JSONObject();
      this.canceled = true;
      var1.put("success", L.t("Adding symbols canceled.", new Object[0]));
      return var1.toString();
   }

   private void subscriptionCheck(String[] var1, boolean var2) throws Exception {
      for (String var6 : var1) {
         if (var2) {
            if (!HistoryDataSubscription.getInstance().isAllowedEquity(var6, true)) {
               throw new Exception(L.t("Cannot add ticker '%s'. EOD data subscription not active.", new Object[]{var6}));
            }
         } else if (!HistoryDataSubscription.getInstance().isAllowedEquity(var6, false)) {
            throw new Exception(L.t("Cannot add ticker '%s'. Intraday data subscription not active.", new Object[]{var6}));
         }
      }
   }

   private String onLookup(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      boolean var3 = Boolean.parseBoolean(((String[])var1.get("searchInName"))[0]);
      boolean var4 = Boolean.parseBoolean(((String[])var1.get("searchInTicker"))[0]);
      boolean var5 = Boolean.parseBoolean(((String[])var1.get("exact"))[0]);
      if (!var3 && !var4) {
         throw new Exception(L.t("Search method not set.", new Object[0]));
      }

      TickerFilterDto var6 = new TickerFilterDto();
      var6.setExactMatch(var4 && var5);
      var6.setSearchInName(var3);
      var6.setSearchInTicker(var4);
      if (var1.containsKey("symbols")) {
         var6.setNames(this.tryGetParamValue(var1, "symbols").split("[\\n,;]+"));
      }

      try {
         String var7 = this.tryGetParamValue(var1, "exchange");
         var6.setMarketId(Long.valueOf(var7));
      } catch (Exception var8) {
      }

      if (var6.getMarketId() == 0L && var6.getNames() == null) {
         throw new Exception(L.t("Exchange or text must be set", new Object[0]));
      }

      List var9 = HistoryDataManager.get().getStockTickers(var6);
      var2.put("tickers", new TickerDtoToJsonTransformer(TickerKind.STOCK).toJson(var9));
      var2.put("success", L.t("Data lookup.", new Object[0]));
      return var2.toString();
   }

   private String onUpdate() throws Exception {
      ArrayList var1 = DataManager.listForSource(5);
      return this.performUpdate(var1);
   }

   private String performUpdate(ArrayList<DataInfo> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      if (var1.isEmpty()) {
         var2.put("success", L.t("No symbols to update.", new Object[0]));
         return var2.toString();
      }

      List var3 = var1.stream().filter(var0 -> var0.uSymbol != null).map(var0 -> var0.uSymbol).collect(Collectors.toList());
      TickerFilterDto var4 = new TickerFilterDto();
      var4.setSearchInTicker(true);
      var4.setExactMatch(true);
      var4.setNames(var3.toArray(new String[0]));
      List var5 = HistoryDataManager.get().getStockTickers(var4);
      Map var6 = var5.stream().collect(Collectors.toMap(TickerDto::getTicker, var0 -> (TickerDto)var0));
      LinkedList var7 = new LinkedList();

      for (DataInfo var9 : var1) {
         if (var9.uSymbol != null && this.isDownloadAllowed(var9) && !DataManager.isLimited(var9.symbol)) {
            var7.add(var9);
         }
      }

      DownloadDispatcher.get().updateHistoryData(var7, var6);
      if (!var1.isEmpty() && var7.isEmpty()) {
         throw new RuntimeException(L.t("You don't have subscriptions for requested data.", new Object[0]));
      }

      var2.put("success", L.t("Update started.", new Object[0]));
      return var2.toString();
   }

   private boolean isDownloadAllowed(DataInfo var1) {
      String var2 = var1.timeframe;
      boolean var3 = !var2.equals("M1");
      return HistoryDataSubscription.getInstance().isAllowedEquity(var1.uSymbol, var3);
   }

   private String onVerifySubscription(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      HistoryDataSubscription var3 = HistoryDataSubscription.getInstance();
      var2.put("eodSubscriptionActive", var3.isEquitiesEODActive());
      var2.put("minuteSubscriptionActive", var3.isEquitiesMinuteActive());
      var2.put("freeEquities", var3.getFreeEquities());
      var2.put("success", L.t("Subscription verified.", new Object[0]));
      return var2.toString();
   }

   private String onUpdateDataAction(Map<String, String[]> var1) throws Exception {
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

   private String onUpdateAll() throws Exception {
      return this.onUpdate();
   }

   private String onUpdateSelected(Map<String, String[]> var1) throws Exception {
      try {
         String[] var2 = this.tryGetParam(var1, "symbols")[0].split(",");
         ArrayList var4 = new ArrayList();

         for (int var5 = 0; var5 < var2.length; var5++) {
            String var6 = var2[var5];
            DataInfo var3 = DataManager.getDataInfo("History", var6);
            if (var3 == null) {
               Log.error(String.format("Ticker '%s' not found. Failed to update data.", var6));
            } else if (var3.source == 5 && var3.sourceDataId == 0) {
               var4.add(var3);
            }
         }

         return this.performUpdate(var4);
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var7);
      }
   }
}
