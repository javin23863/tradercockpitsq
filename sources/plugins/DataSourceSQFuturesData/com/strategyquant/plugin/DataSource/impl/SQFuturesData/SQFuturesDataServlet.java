package com.strategyquant.plugin.DataSource.impl.SQFuturesData;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.BatchProgressController;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.DukasDataManager;
import com.strategyquant.datalib.data.InstrumentValueEvaluator;
import com.strategyquant.datalib.historyData.TickerFilterDto;
import com.strategyquant.datalib.historyData.dto.CommodityDto;
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
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.client.ClientProtocolException;
import org.jdom2.JDOMException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQFuturesDataServlet extends HttpJSONServlet {
   private static final String JOB_PREFIX = "HistoryDownloadJob_";
   private static final Logger Log = LoggerFactory.getLogger(SQFuturesDataServlet.class);
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
         case "updateBr":
            return this.onUpdateBr();
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
      List var2 = HistoryDataManager.get().getFuturesMarkets();
      var2.add(0, new IdName(0L, L.t("Any", new Object[0])));
      JSONArray var3 = new MarketDtoToJsonTransformer().toJson(var2);
      var1.put("exchanges", var3);
      var1.put("success", L.t("Exchanges listed.", new Object[0]));
      this.exchangesResponse = var1.toString();
      return this.exchangesResponse;
   }

   private String onAdd(final Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"symbols", "barType", "timezoneType"});
      JSONObject var2 = new JSONObject();
      this.canceled = false;
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this._onAdd(var1);
      } else {
         (new Thread() {
            @Override
            public void run() {
               SQFuturesDataServlet.this._onAdd(var1);
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
         final String[] var5 = var4.split(",");
         String var6 = var1.containsKey("postfix") ? this.tryGetParamValue(var1, "postfix") : "";
         int var7 = Integer.parseInt(this.tryGetParam(var1, "barType")[0]);
         int var8 = Integer.parseInt(this.tryGetParam(var1, "timezoneType")[0]);
         int var9 = Integer.parseInt(this.tryGetParam(var1, "timezoneShift")[0]);
         String var10 = this.tryGetParamValue(var1, "timezone");
         Object var11 = null;
         if (var8 == 2) {
            var11 = "Exchange";
         } else if (var8 == 1) {
            var11 = var10;
         } else {
            var11 = "Exchange " + (var9 < 0 ? var9 : "+" + var9);
         }

         List var12 = HistoryDataManager.get().getCommodities(var5);
         final Map var13 = var12.stream().collect(Collectors.toMap(CommodityDto::getId, var0 -> (CommodityDto)var0));
         TickerFilterDto var14 = new TickerFilterDto();
         var14.setSearchInTicker(true);
         var14.setExactMatch(true);
         var14.setNames(var5);
         List var15 = HistoryDataManager.get().getFuturesTickers(var14);
         LinkedList var16 = new LinkedList();
         LinkedList var17 = new LinkedList();

         for (TickerDto var19 : var15) {
            if (var19.isEod()) {
               var16.add(var19.getTicker());
            } else {
               var17.add(var19.getTicker());
            }
         }

         Map var31 = var15.stream().collect(Collectors.toMap(TickerDto::getTicker, var0 -> (TickerDto)var0));
         this.subscriptionCheck(var16.toArray(new String[0]), var31, true);
         this.subscriptionCheck(var17.toArray(new String[0]), var31, false);
         ArrayList var32 = new ArrayList(var5.length);

         for (int var21 = 0; var21 < var5.length; var21++) {
            String var20 = var5[var21];
            TickerDto var22 = (TickerDto)var31.get(var20);
            DataInfo var23 = new DataInfo();
            var32.add(var23);
            var23.barTimeType = var7;
            var23.timezone = (String)var11;
            var23.connection = "History";
            var23.symbol = var20 + var6;
            var23.source = 6;
            CommodityDto var24 = (CommodityDto)var13.get(var22.getCommodityId());
            var23.instrument = var24.getCode() + " - " + var22.getMarketName().replaceAll("Common ", "");
            var23.uSymbol = var22.getTicker();
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
               CommodityDto var2x = (CommodityDto)var13.get(var1.getCommodityId());
               return var2x.getTickStep().doubleValue();
            }

            public double getTickSize(TickerDto var1) {
               CommodityDto var2x = (CommodityDto)var13.get(var1.getCommodityId());
               return var2x.getTickSize().doubleValue();
            }

            public double getPointValue(TickerDto var1) {
               CommodityDto var2x = (CommodityDto)var13.get(var1.getCommodityId());
               return var2x.getPointValue().doubleValue();
            }

            public byte getInstrumentType(TickerDto var1) {
               String var2x = var1.getType();
               return (byte)(var2x != null && var2x.contains("ETF") ? 5 : 2);
            }

            public String getDescriptions(TickerDto var1) {
               return "History data instrument";
            }

            public double getOrderSizeMultiplier(TickerDto var1) {
               CommodityDto var2x = (CommodityDto)var13.get(var1.getCommodityId());
               return var2x.getOrderSizeMulti().doubleValue();
            }

            public double getOrderSizeStep(TickerDto var1) {
               CommodityDto var2x = (CommodityDto)var13.get(var1.getCommodityId());
               return var2x.getOrderSizeStep() == null ? 0.0 : var2x.getOrderSizeStep().doubleValue();
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
               return SQFuturesDataServlet.this.canceled;
            }

            public void finished() {
            }
         });
         SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         SQWebSocketManager.addToDataQueue(WSDataObjects.getData(var4, "add"), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var28) {
         Log.error("Cannot add symbols. Exc.", var28);
         var3 = var28.getMessage();
      } finally {
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

   private void subscriptionCheck(String[] var1, Map<String, TickerDto> var2, boolean var3) throws Exception {
      for (String var7 : var1) {
         TickerDto var8 = (TickerDto)var2.get(var7);
         if (var8 != null) {
            if (var3) {
               if (!HistoryDataSubscription.getInstance().isAllowedFuture(var7, var8.getMarketName(), true)) {
                  throw new Exception(L.t("Cannot add ticker '%s'. EOD data subscription not active.", new Object[]{var7}));
               }
            } else if (!HistoryDataSubscription.getInstance().isAllowedFuture(var7, var8.getMarketName(), false)) {
               throw new Exception(L.t("Cannot add ticker '%s'. Intraday data subscription not active.", new Object[]{var7}));
            }
         }
      }
   }

   private String onLookup(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      boolean var3 = Boolean.parseBoolean(((String[])var1.get("searchInName"))[0]);
      boolean var4 = Boolean.parseBoolean(((String[])var1.get("searchInTicker"))[0]);
      boolean var5 = Boolean.parseBoolean(((String[])var1.get("onlyContFutures"))[0]);
      boolean var6 = Boolean.parseBoolean(((String[])var1.get("exact"))[0]);
      if (!var3 && !var4) {
         throw new Exception(L.t("Search method not set.", new Object[0]));
      }

      TickerFilterDto var7 = new TickerFilterDto();
      var7.setExactMatch(var4 && var6);
      var7.setSearchInName(var3);
      var7.setSearchInTicker(var4);
      var7.setOnlyContFutures(var5);
      if (var1.containsKey("symbols")) {
         var7.setNames(this.tryGetParamValue(var1, "symbols").split("[\\n,;]+"));
      }

      try {
         String var8 = this.tryGetParamValue(var1, "exchange");
         var7.setMarketId(Long.valueOf(var8));
      } catch (Exception var9) {
      }

      if (var7.getMarketId() == 0L && var7.getNames() == null) {
         throw new Exception(L.t("Exchange or text must be set", new Object[0]));
      }

      List var10 = HistoryDataManager.get().getFuturesTickers(var7);
      var10 = var10.stream().filter(var0 -> !var0.getTicker().startsWith("@") || var0.getAliasTickerId() != null).collect(Collectors.toList());
      var2.put("tickers", new TickerDtoToJsonTransformer(TickerKind.FUTURES).toJson(var10));
      var2.put("success", L.t("Data lookup.", new Object[0]));
      return var2.toString();
   }

   private String onUpdate() throws Exception {
      ArrayList var1 = DataManager.listForSource(6);
      return this.performUpdate(var1);
   }

   private String onUpdateBr() throws Exception {
      ArrayList var1 = DataManager.listForSource(6);
      LinkedList var2 = new LinkedList();

      for (DataInfo var4 : var1) {
         String var5 = var4.instrument;
         if (var5 != null && var5.endsWith("- BMF")) {
            var2.add(var4);
         }
      }

      return this.performUpdate(var2);
   }

   private String performUpdate(List<DataInfo> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      if (var1.isEmpty()) {
         var2.put("success", L.t("No symbols to update.", new Object[0]));
         return var2.toString();
      }

      List var3 = var1.stream().map(var0 -> var0.uSymbol).collect(Collectors.toList());
      TickerFilterDto var4 = new TickerFilterDto();
      var4.setSearchInTicker(true);
      var4.setExactMatch(true);
      var4.setNames(var3.toArray(new String[0]));
      List var5 = HistoryDataManager.get().getFuturesTickers(var4);
      Map var6 = var5.stream().collect(Collectors.toMap(TickerDto::getTicker, var0 -> (TickerDto)var0));
      LinkedList var7 = new LinkedList();

      for (DataInfo var9 : var1) {
         if (this.isDownloadAllowed(var9, var6)) {
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

   private boolean isDownloadAllowed(DataInfo var1, Map<String, TickerDto> var2) throws ClientProtocolException, IllegalStateException, NoSuchAlgorithmException, IOException, JDOMException, SQLException {
      String var3 = var1.timeframe;
      boolean var4 = !var3.equals("M1");
      TickerDto var5 = (TickerDto)var2.get(var1.uSymbol);
      return HistoryDataSubscription.getInstance().isAllowedFuture(var1.uSymbol, var5.getMarketName(), var4);
   }

   private String onVerifySubscription(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      HistoryDataSubscription var3 = HistoryDataSubscription.getInstance();
      var2.put("eodSubscriptionActive", var3.isFuturesEODActive());
      var2.put("minuteSubscriptionActive", var3.isFuturesMinuteActive());
      var2.put("freeFutures", var3.getFreeFutures());
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
            DataInfo var3 = DataManager.getDataInfo("History", var2[var5]);
            if (var3.source == 6 && var3.sourceDataId == 0) {
               var4.add(var3);
            }
         }

         return this.performUpdate(var4);
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot update data.", new Object[0]), var6);
      }
   }
}
