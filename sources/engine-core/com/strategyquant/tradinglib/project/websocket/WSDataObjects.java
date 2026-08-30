package com.strategyquant.tradinglib.project.websocket;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionElement;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.datalib.timezone.Timezone;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.connection.Connection;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.databank.DatabankViewsManager;
import com.strategyquant.tradinglib.engine.TradingEngine;
import com.strategyquant.tradinglib.engine.TradingSetup;
import com.strategyquant.tradinglib.engine.TradingSetupStatus;
import com.strategyquant.tradinglib.historyData.HistoryDataManager;
import com.strategyquant.tradinglib.historyData.SpecialSymbolsManager;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.project.CustomConfigs;
import com.strategyquant.tradinglib.riskmanagement.RiskManagementMethodsList;
import com.strategyquant.tradinglib.tradelist.TradelistTableColumnEntry;
import com.strategyquant.tradinglib.tradelist.TradelistTableView;
import com.strategyquant.tradinglib.tradelist.TradelistViewsManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSDataObjects {
   private static final Logger Log = LoggerFactory.getLogger(WSDataObjects.class);

   public static DataToSend getStrategies() {
      JSONArray var0 = new JSONArray();

      for (TradingSetup var2 : TradingEngine.getLiveEngine().getTradingSetups()) {
         JSONObject var3 = new JSONObject();
         var3.put("SetupID", var2.getSetupId());
         var3.put("StrategyName", var2.getStrategy().getStrategyName());
         var3.put("ConnectionName", var2.getChartSetup().getCharts().get(0).getConnectionName());
         var3.put("Symbol", var2.getChartSetup().getCharts().get(0).getSymbol());
         var3.put("Status", L.tsq(TradingSetupStatus.toString(var2.getStatus())));
         var0.put(var3);
      }

      return new DataToSend("strategies", var0);
   }

   public static DataToSend getConnections() {
      ConnectionManager var0 = TradingEngine.getLiveEngine().getConnectionManager();
      JSONArray var1 = new JSONArray();

      for (Connection var3 : var0.getAllConnections()) {
         JSONObject var4 = new JSONObject();
         var4.put("ConnectionPlugin", var3.getPluginName());
         var4.put("ConnectionName", var3.getConnectionName());
         var4.put("ConnectionState", var3.getStatus());
         var4.put("ConnectionParams", var3.getConnectionParams().toJSON());
         var4.put("ConnectionType", var3.getType());
         if (var3.isExecutionEngine()) {
            var4.put("OpenPositions", var3.getExecutionEngine().getOpenOrdersCount(false));
            var4.put("AccountBalance", var3.getExecutionEngine().getAccountBalance());
            var4.put("AccountPL", var3.getExecutionEngine().getTotalPL());
         } else {
            var4.put("OpenPositions", "N/A");
            var4.put("AccountBalance", "N/A");
            var4.put("AccountPL", "N/A");
         }

         var1.put(var4);
      }

      return new DataToSend("connections", var1);
   }

   public static DataToSend getDataForSingleSymbol(String var0, String var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      DataInfo var4 = DataManager.getDataInfo("History", var0);

      try {
         JSONObject var5 = new JSONObject();
         var5.put("symbol", var4.symbol);
         var5.put("barType", var4.barTimeType);
         var5.put("dateFrom", var4.dateFrom);
         var5.put("dateTo", var4.dateTo);
         var5.put("rows", var4.rows);
         var5.put("totalDays", SQTime.getDaysBetween(var4.dateFrom, var4.dateTo));
         var5.put("connection", var4.connection);
         var5.put("filename", var4.filename);
         var5.put("instrument", var4.instrument);
         var5.put("timeframe", var4.timeframe);
         var5.put("timezone", Timezone.print(var4.timezone, var4.source, false));
         var5.put("timezoneShort", Timezone.print(var4.timezone, var4.source, true));
         var5.put("source", var4.source);
         var5.put("secondsRecords", var4.secondsRecords);
         InstrumentInfo var6 = InstrumentManager.getInstrumentInfo(var4.instrument);
         var5.put("pointValue", var6.pointValue);
         var5.put("tickSize", var6.tickSize);
         var5.put("tickStep", var6.tickStep);
         var5.put("dataType", var6.dataType);
         var5.put("spread", var6.defaultSpread);
         var5.put("slippage", var6.defaultSlippage);
         var5.put("commissions", var6.commissions);
         var5.put("swap", var6.swap);
         var5.put("uSymbol", var4.uSymbol);
         var5.put("uSymbolName", var4.uSymbolName);
         var5.put("id", var4.id);
         var5.put("sourceDataId", var4.sourceDataId);
         var5.put("show", var4.show);
         var3.put(var5);
      } catch (Exception var7) {
         Log.error("Cannot load symbol. Exc.", var7);
         Log.error("Deleting symbol " + var4.symbol, var7);
         DataManager.deleteData(var4.connection, var4.symbol);
      }

      var2.put("action", var1);
      var2.put("symbols", var0);
      var2.put("data", var3);
      return new DataToSend("data", var2);
   }

   public static DataToSend getData(String var0, String var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      Map var4 = null;
      BasketOfStocksManager var5 = BasketOfStocksManager.getInstance();
      if (var5 != null) {
         List var6 = BasketOfStocksManager.getInstance().getGroups();
         var4 = var6.stream().collect(Collectors.toMap(BasketDto::getName, var0x -> (BasketDto)var0x));
      }

      ArrayList var17 = DataManager.list();
      HashMap var7 = new HashMap();
      HashSet var8 = new HashSet();

      for (DataInfo var10 : var17) {
         var8.add(var10.symbol);
         var7.put(var10.symbol, var10);
      }

      for (DataInfo var19 : var17) {
         if (!SpecialSymbolsManager.isSpecial(var19)) {
            try {
               JSONObject var11 = new JSONObject();
               var11.put("symbol", var19.symbol);
               var11.put("barType", var19.barTimeType);
               var11.put("dateFrom", var19.dateFrom);
               var11.put("dateTo", var19.dateTo);
               var11.put("rows", var19.rows);
               var11.put("totalDays", SQTime.getDaysBetween(var19.dateFrom, var19.dateTo));
               var11.put("connection", var19.connection);
               var11.put("filename", var19.filename);
               var11.put("instrument", var19.instrument);
               var11.put("timeframe", var19.timeframe);
               var11.put("timezone", Timezone.print(var19.timezone, var19.source, false));
               var11.put("timezoneShort", Timezone.print(var19.timezone, var19.source, true));
               var11.put("source", var19.source);
               var11.put("secondsRecords", var19.secondsRecords);
               InstrumentInfo var12 = InstrumentManager.getInstrumentInfo(var19.instrument);
               var11.put("pointValue", var12.pointValue);
               var11.put("tickSize", var12.tickSize);
               var11.put("tickStep", var12.tickStep);
               var11.put("dataType", var12.dataType);
               var11.put("spread", var12.defaultSpread);
               var11.put("slippage", var12.defaultSlippage);
               var11.put("commissions", var12.commissions);
               var11.put("swap", var12.swap);
               var11.put("uSymbol", var19.uSymbol);
               var11.put("uSymbolName", var19.uSymbolName);
               var11.put("id", var19.id);
               var11.put("sourceDataId", var19.sourceDataId);
               var11.put("show", var19.show);
               var11.put("broker", var19.brokerId);
               var11.put("brokerName", BrokerManager.getInstance().getBrokerName(var19.brokerId));
               boolean var13 = true;
               if (var5 != null && var19.symbol.startsWith("[")) {
                  BasketDto var14 = (BasketDto)var4.get(var19.symbol);
                  if (var14 != null) {
                     int var15 = BasketOfStocksManager.getDownloaded(var7, var14.getId());
                     var13 = BasketOfStocksManager.isReadyForUse(var15, var14.getTotal());
                  }
               }

               var11.put("readyToUse", var13);
               String var20 = null;
               if (var19.source == 5) {
                  var20 = HistoryDataManager.get().getStockExchange(var19.uSymbol);
               } else if (var19.source == 6) {
                  var20 = HistoryDataManager.get().getFuturesExchange(var19.uSymbol);
               }

               var11.put("exchange", var20 == null ? "" : var20);
               var3.put(var11);
            } catch (Exception var16) {
               Log.error("Cannot load symbol. Exc.", var16);
               Log.error("Deleting symbol " + var19.symbol, var16);
               DataManager.deleteData(var19.connection, var19.symbol);
            }
         }
      }

      var2.put("action", var1);
      var2.put("symbols", var0);
      var2.put("data", var3);
      return new DataToSend("data", var2);
   }

   public static DataToSend getData(Set<String> var0, String var1, String var2) throws Exception {
      JSONObject var3 = new JSONObject();
      JSONArray var4 = new JSONArray();

      for (DataInfo var7 : DataManager.list()) {
         try {
            if (var0.contains(var7.symbol)) {
               JSONObject var8 = new JSONObject();
               var8.put("symbol", var7.symbol);
               var8.put("barType", var7.barTimeType);
               var8.put("dateFrom", var7.dateFrom);
               var8.put("dateTo", var7.dateTo);
               var8.put("rows", var7.rows);
               var8.put("totalDays", SQTime.getDaysBetween(var7.dateFrom, var7.dateTo));
               var8.put("connection", var7.connection);
               var8.put("filename", var7.filename);
               var8.put("instrument", var7.instrument);
               var8.put("timeframe", var7.timeframe);
               var8.put("timezone", Timezone.print(var7.timezone, var7.source, false));
               var8.put("timezoneShort", Timezone.print(var7.timezone, var7.source, true));
               var8.put("source", var7.source);
               var8.put("secondsRecords", var7.secondsRecords);
               InstrumentInfo var9 = InstrumentManager.getInstrumentInfo(var7.instrument);
               var8.put("pointValue", var9.pointValue);
               var8.put("tickSize", var9.tickSize);
               var8.put("tickStep", var9.tickStep);
               var8.put("dataType", var9.dataType);
               var8.put("spread", var9.defaultSpread);
               var8.put("slippage", var9.defaultSlippage);
               var8.put("commissions", var9.commissions);
               var8.put("swap", var9.swap);
               var8.put("uSymbol", var7.uSymbol);
               var8.put("uSymbolName", var7.uSymbolName);
               var8.put("id", var7.id);
               var8.put("sourceDataId", var7.sourceDataId);
               var8.put("show", var7.show);
               var4.put(var8);
            }
         } catch (Exception var10) {
            Log.error("Cannot load symbol. Exc.", var10);
            Log.error("Deleting symbol " + var7.symbol, var10);
            DataManager.deleteData(var7.connection, var7.symbol);
         }
      }

      var3.put("action", var2);
      var3.put("symbols", var1);
      var3.put("data", var4);
      return new DataToSend("data", var3);
   }

   public static DataToSend getCustomData(String var0, String var1) throws Exception {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();

      for (CustomDataInfo var6 : CustomDataManager.list()) {
         try {
            JSONObject var7 = new JSONObject();
            var7.put("id", var6.id);
            var7.put("name", var6.name);
            var7.put("dataType", var6.dataType);
            var7.put("values", var6.values);
            var7.put("valuesObject", var6.valuesObject);
            var7.put("timeframe", var6.timeframe);
            var7.put("rows", var6.rows);
            var7.put("dateFrom", var6.dateFrom);
            var7.put("dateTo", var6.dateTo);
            var7.put("totalDays", SQTime.getDaysBetween(var6.dateFrom, var6.dateTo));
            var7.put("filename", var6.filename);
            var7.put("timeframe", var6.timeframe);
            var3.put(var7);
         } catch (Exception var8) {
            Log.error("Cannot load custom data. Exc.", var8);
         }
      }

      var2.put("action", var1);
      var2.put("names", var0);
      var2.put("data", var3);
      return new DataToSend("customdata", var2);
   }

   public static DataToSend getBaskets() {
      JSONArray var0 = new JSONArray();
      List var1 = BasketOfStocksManager.getInstance().getGroups();

      ArrayList var2;
      try {
         DataManager.get();
         var2 = DataManager.list();
      } catch (Exception var11) {
         throw new RuntimeException(L.t("Error while fetching list of symbols", new Object[0]), var11);
      }

      HashMap var3 = new HashMap();
      HashSet var4 = new HashSet();

      for (DataInfo var6 : var2) {
         var4.add(var6.symbol);
         var3.put(var6.symbol, var6);
      }

      for (BasketDto var13 : var1) {
         JSONObject var7 = new JSONObject();
         var7.put("id", var13.getId());
         var7.put("name", var13.getName());
         var7.put("count", var13.getCount());
         var7.put("desc", var13.getDesc());
         List var8 = BasketOfStocksManager.getInstance().getStocks(var13.getId());
         List var9 = var8.stream().map(StockDto::getTicker).collect(Collectors.toList());
         var7.put("stocks", "," + String.join(",", var9) + ",");
         var7.put("existingSymbols", BasketOfStocksManager.getNumberOfSymbolsInGroup(var4, var13));
         int var10 = BasketOfStocksManager.getDownloaded(var3, var13.getId());
         var7.put("downloadedInfo", var10 + " / " + var13.getTotal());
         var7.put("readyToUse", BasketOfStocksManager.isReadyForUse(var10, var13.getTotal()));
         var0.put(var7);
      }

      return new DataToSend("baskets", var0);
   }

   public static DataToSend getBrokers() {
      JSONArray var0 = new JSONArray();

      for (BrokerDto var3 : BrokerManager.getInstance().getAllBrokers()) {
         JSONObject var4 = new JSONObject();
         var4.put("id", var3.getId());
         var4.put("name", var3.getName());
         var4.put("customizedStocks", var3.getCustomizedStocks());
         var4.put("desc", var3.getDesc());
         var4.put("stockPickerUse", var3.isStockPickerUse());
         var4.put("mtUse", var3.isMtUse());
         var4.put("postfix", var3.getPostfix());
         var4.put("mtTimezone", var3.getMtTimezone());
         List var5 = BrokerManager.getInstance().getStocks(var3.getId());
         var4.put("stocks", "," + String.join(",", var5) + ",");
         var0.put(var4);
      }

      return new DataToSend("brokers", var0);
   }

   public static DataToSend getInstruments() throws DataException {
      JSONArray var0 = new JSONArray();

      for (InstrumentInfo var3 : InstrumentManager.list()) {
         JSONObject var4 = new JSONObject();
         var4.put("defaultSpread", var3.defaultSpread);
         var4.put("defaultSlippage", var3.defaultSlippage);
         var4.put("pointValue", var3.pointValue);
         var4.put("commissions", var3.commissions);
         var4.put("decimals", var3.decimals);
         var4.put("tickSize", var3.tickSize);
         var4.put("tickStep", var3.tickStep);
         var4.put("tickValueInMoney", var3.tickValueInMoney);
         var4.put("description", var3.description);
         var4.put("instrument", var3.instrument);
         var4.put("dataType", var3.dataType);
         var4.put("exchange", var3.exchange);
         var4.put("country", var3.country);
         var4.put("sector", var3.sector);
         var4.put("swap", var3.swap);
         var4.put("minDistance", var3.minDistance);
         var4.put("broker", var3.broker);
         var4.put("orderSizeMultiplier", var3.orderSizeMultiplier);
         var4.put("orderSizeStep", var3.orderSizeStep);
         JSONArray var5 = new JSONArray();
         var4.put("aliases", var5);
         var0.put(var4);
      }

      return new DataToSend("instruments", var0);
   }

   public static DataToSend getAliases() throws DataException {
      JSONArray var0 = new JSONArray();
      return new DataToSend("aliases", var0);
   }

   public static DataToSend getSessions() throws Exception {
      JSONArray var0 = new JSONArray();

      for (Session var2 : SessionManager.getSessions()) {
         JSONObject var3 = new JSONObject();
         var3.put("name", var2.getSessionName());
         var3.put("broker", var2.getBroker());
         var3.put("brokerName", BrokerManager.getInstance().getBrokerName(var2.getBroker()));

         try {
            JSONArray var4 = new JSONArray();
            if (var2.getElements() != null) {
               for (SessionElement var6 : var2.getElements()) {
                  JSONObject var7 = new JSONObject();
                  var7.put("dayFrom", var6.getDayFromStr());
                  var7.put("timeFrom", var6.getTimeFromStr());
                  var7.put("dayTo", var6.getDayToStr());
                  var7.put("timeTo", var6.getTimeToStr());
                  var7.put("eod", var6.isEOD());
                  var4.put(var7);
               }
            }

            var3.put("elements", var4);
            var0.put(var3);
         } catch (Exception var8) {
            Log.error("Cannot load session", var8);
         }
      }

      return new DataToSend("sessions", var0);
   }

   public static DataToSend getTimeframes() {
      return getTimeframes(true);
   }

   public static DataToSend getTimeframes(boolean var0) {
      JSONArray var1 = new JSONArray();

      for (String var3 : TimeframeManager.getTimeframes()) {
         if (var0 || !"TICK".equals(var3)) {
            var1.put(var3);
         }
      }

      return new DataToSend("timeframes", var1);
   }

   public static DataToSend getExchanges() {
      JSONArray var0 = new JSONArray();

      for (String var2 : InstrumentManager.getExchanges()) {
         var0.put(var2);
      }

      return new DataToSend("exchanges", var0);
   }

   public static DataToSend getCountries() {
      JSONArray var0 = new JSONArray();

      for (String var2 : InstrumentManager.getCountries()) {
         var0.put(var2);
      }

      return new DataToSend("countries", var0);
   }

   public static DataToSend getSectors() {
      JSONArray var0 = new JSONArray();

      for (String var2 : InstrumentManager.getSectors()) {
         var0.put(var2);
      }

      return new DataToSend("sectors", var0);
   }

   public static DataToSend getTradelistViews() {
      JSONArray var0 = new JSONArray();

      for (String var2 : TradelistViewsManager.getInstance().getViews().keySet()) {
         JSONObject var3 = new JSONObject();
         JSONArray var4 = new JSONArray();

         TradelistTableView var5;
         try {
            var5 = TradelistViewsManager.getInstance().getView(var2);
         } catch (Exception var10) {
            Log.error("Cannot load view '" + var2 + "'. ", var10);
            continue;
         }

         var3.put("name", var2);

         for (TradelistTableColumnEntry var7 : var5.columns) {
            try {
               JSONObject var8 = new JSONObject();
               var8.put("entryName", var7.getColumnEntryName());
               var8.put("columnName", var7.tableColumn.getName());
               var8.put("class", var7.tableColumn.getClass().getSimpleName());
               var8.put("width", var7.tableColumn.getWidth());
               var8.put("type", var7.tableColumn.getType());
               var8.put("fontBold", var7.tableColumn.isFontBold());
               var4.put(var8);
            } catch (Exception var9) {
               Log.error("Cannot load columns of view '" + var2 + "'. ", var9);
            }
         }

         var3.put("columns", var4);
         var0.put(var3);
      }

      return new DataToSend("tradelistViews", var0);
   }

   public static DataToSend getDatabankViews() {
      return new DataToSend("databankViews", DatabankViewsManager.getInstance().toJSON());
   }

   public static DataToSend getMMMethods() {
      JSONArray var0 = new JSONArray();

      for (MoneyManagementMethod var2 : MoneyManagementMethodsList.get().getAvailableClasses()) {
         JSONObject var3 = new JSONObject();
         var3.put("engine", var2.getEngine());
         var3.put("name", var2.getName());
         var3.put("help", var2.getNote());
         var3.put("description", var2.getDescription());
         var3.put("class", var2.getClass().getSimpleName());
         var3.put("config", XMLUtil.elementToString(var2.getXML()));
         var0.put(var3);
      }

      return new DataToSend("mmMethods", var0);
   }

   public static DataToSend getRMMethods() {
      JSONArray var0 = new JSONArray();

      for (RiskManagementMethod var2 : RiskManagementMethodsList.get().getAvailableClasses()) {
         JSONObject var3 = new JSONObject();
         var3.put("name", var2.getName());
         var3.put("help", var2.getNote());
         var3.put("class", var2.getClass().getSimpleName());
         var3.put("config", XMLUtil.elementToString(var2.getXML()));
         var0.put(var3);
      }

      return new DataToSend("rmMethods", var0);
   }

   public static DataToSend getTaskTemplates() {
      JSONArray var0 = new JSONArray();
      HashMap var1 = CustomConfigs.list();

      for (String var3 : var1.keySet()) {
         JSONObject var4 = new JSONObject();
         var4.put("name", var3);
         var4.put("project", var1.get(var3));
         var0.put(var4);
      }

      return new DataToSend("taskConfigs", var0);
   }

   public static DataToSend getTradingOptions() {
      JSONArray var0 = new JSONArray();
      HashMap var1 = CustomConfigs.list();

      for (String var3 : var1.keySet()) {
         JSONObject var4 = new JSONObject();
         var4.put("name", var3);
         var4.put("project", var1.get(var3));
         var0.put(var4);
      }

      return new DataToSend("taskConfigs", var0);
   }
}
