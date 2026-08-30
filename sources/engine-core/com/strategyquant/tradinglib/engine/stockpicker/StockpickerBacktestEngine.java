package com.strategyquant.tradinglib.engine.stockpicker;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.Trader;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.AbstractBacktestEngine;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.PortfolioBacktester;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.log.StockpickerLog;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;
import com.strategyquant.tradinglib.engine.stockpicker.data.LoadedPickerData;
import com.strategyquant.tradinglib.engine.stockpicker.signals.CollectedSignals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.Signals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignal;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignalComparatorByRevScore;
import com.strategyquant.tradinglib.explore.Explore;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.options.parameters.LimitOver;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import com.strategyquant.tradinglib.options.parameters.StoreChartData;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByTicket;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockpickerBacktestEngine extends AbstractBacktestEngine {
   public static final Logger Log = LoggerFactory.getLogger(StockpickerBacktestEngine.class);
   public static final String StockpickerPath = SQPaths.userDirPath + "/stockpicker/";
   private Trader tradeController;
   private PortfolioBacktester backtester;
   private PortfolioTrader trader;
   private IBacktestProgressListener backtestProgressListener;
   private StopPauseEngine stopPauseEngine;
   private ILastEventListener lastEventListener;
   private StockpickerLog pickerLog = new StockpickerLog();
   private SettingsMap mainSettings;
   private double initialCapital;
   private int MaxOpenPositionsLong;
   private int MaxOpenPositionsShort;
   private long dayStart;
   private Element elStrategy;
   private String mainSymbol = null;
   DataInfo mainSymbolInfo = null;
   private ChartSetup setup;
   private double slippage;
   private CommissionsMethod commission;
   private SwapMethod swap;
   private double LimitOver;
   private boolean AllowBetterLimitFill;
   private String lastSettingsXml = null;
   private StrategyBase strategy;
   private boolean storeChartData = false;
   private boolean isAlgowizard = false;

   public StockpickerBacktestEngine() {
      this.tradeController = new Trader("TradeController", null);
      this.backtester = new PortfolioBacktester(this.pickerLog);
      this.trader = new PortfolioTrader(this.tradeController, this.pickerLog);
   }

   public ResultsGroup runBacktest(String var1) throws Exception {
      try {
         this.checkListeners();
         OrdersList var2 = null;
         long var3 = System.currentTimeMillis();
         long var5 = this.setup.getMainChart().getHistoryFrom();
         long var7 = this.setup.getMainChart().getHistoryTo();
         this.pickerLog.init(var1, this.mainSymbol, var5, var7);
         Log.debug("Collecting trade signals...");
         this.lastEventListener.setLastEvent(L.t("Collecting trade signals...", new Object[0]));
         this.backtester.setStopPauseEngine(this.stopPauseEngine);
         this.backtester.setLastEventListener(this.lastEventListener);
         this.backtester.setStrategyXml(this.elStrategy);
         PickerEntrySignalComparatorByRevScore var9 = new PickerEntrySignalComparatorByRevScore();
         this.backtester.setReducer(3 * this.MaxOpenPositionsLong, 3 * this.MaxOpenPositionsShort, var9);
         this.backtester.backtest();
         LoadedPickerData var10 = this.backtester.getPickerData();
         CollectedSignals var11 = this.backtester.getSignals();
         ObjectArrayList var12 = new ObjectArrayList();
         ObjectArrayList var13 = this.backtester.getSymbolsBacktested();
         if (!var13.isEmpty()) {
            Log.debug("Executing trades based on the signals...");
            this.lastEventListener.setLastEvent(L.t("Executing trades based on the signals...", new Object[0]));
            this.trader.onStart(this.initialCapital, this.slippage, this.commission, this.swap, this.LimitOver, this.AllowBetterLimitFill);
            int var15 = 0;
            var5 = SQTime.getDateInMs(var5);
            var7 = SQTime.getDateInMs(var7);
            int var16 = var10.timeline.searchIndexByTimeD(var5, true);
            int var17 = var10.timeline.searchIndexByTimeD(var7, false);
            IntOpenHashSet var18 = new IntOpenHashSet();

            for (int var19 = var16; var19 <= var17; var19++) {
               int var14 = 50 + (int)((double)var19 / var17 / 2.0 * 100.0);
               if (var14 != var15 && var14 % 10.0 == 0.0) {
                  this.backtestProgressListener.setProgress(var14 == 100 ? 95 : var14);
                  var15 = var14;
               }

               long var20 = var10.timeline.indexToTimeD(var19);
               Signals var22 = (Signals)var11.get(var20);
               if (var22 != null && var22.exitSignalExists()) {
                  if (var22.longEntrySignalExists()) {
                     ObjectArrayList var23 = var22.getLongEntrySignals().list();

                     for (int var24 = 0; var24 < var23.size(); var24++) {
                        int var25 = ((PickerEntrySignal)var23.get(var24)).symbol.hashCode();
                        var18.add(var25);
                     }
                  }

                  if (var22.shortEntrySignalExists()) {
                     ObjectArrayList var40 = var22.getShortEntrySignals().list();

                     for (int var42 = 0; var42 < var40.size(); var42++) {
                        int var44 = ((PickerEntrySignal)var40.get(var42)).symbol.hashCode();
                        var18.add(var44);
                     }
                  }

                  var22.getExitSignals().reduceCount(var18);
               }

               if (this.checkStopped()) {
                  throw new TaskStoppedException();
               }

               if (this.pickerLog.storeLogs()) {
                  this.pickerLog.print("\n-------------------------------------------");
                  this.pickerLog.print(SQTime.toDateString(var20));
                  this.pickerLog.print(var22, this.trader, true);
               }

               this.trader.onBar(var20, var19, var10, var22);
               if (this.pickerLog.storeLogs()) {
                  this.pickerLog.print(var22, this.trader, false);
               }

               var18.clear();
               ObjectArrayList var41 = this.trader.getOpenPositions();

               for (int var43 = 0; var43 < var41.size(); var43++) {
                  LiveOrderObj var45 = (LiveOrderObj)var41.get(var43);
                  var18.add(var45.getSymbolHash());
               }
            }

            this.trader.onClose();
            var2 = this.trader.getHistoryOrders();
            var12 = this.trader.getTradedSymbols();
         }

         int var33 = this.backtester.getTotalTicks();
         double var34 = (System.currentTimeMillis() - var3) / 1000.0;
         ResultsGroup var35 = null;
         if (!var13.isEmpty() && !var12.isEmpty()) {
            var35 = this.createResultsGroup(var1, var12, var2, var33, var34);
         } else {
            var35 = this.emptyRg(var1, var33, var34);
         }

         this.checkBadStrategy(var35);
         if (this.pickerLog.storeLogs) {
            try {
               String var37 = "// "
                  + String.format("Entry type: %s", PickerTriggerTypes.toString(this.strategy.Stockpicker.entryType))
                  + "\n// "
                  + String.format("Exit type: %s", PickerTriggerTypes.toString(this.strategy.Stockpicker.exitType))
                  + "\n// "
                  + String.format("Max open positions Long: %d", this.strategy.Stockpicker.MaxOpenPositionsLong)
                  + "\n// "
                  + String.format("Max open positions Short: %d", this.strategy.Stockpicker.MaxOpenPositionsShort)
                  + "\n//\n// "
                  + String.format("Symbols loaded: %d", this.backtester.getTotalSymbolsLoaded())
                  + "\n// "
                  + String.format("Symbols backtested: %d", var13.size())
                  + "\n// "
                  + String.format("Symbols traded: %d", var12.size());
               String var39 = this.pickerLog.toString(var37);
               var35.specialValues().set(SpecialValues.StockpickerLog, var39);
            } catch (Exception var29) {
               Log.info("Failed to save StockpickerLog to strategy ResultsGroup...", var29);
            }
         }

         var35.specialValues().set(SpecialValues.StockpickerStrategy, true);
         var35.mainResult().setWorstDailyEquity(this.trader.getWorstDailyEquity());
         var35.mainResult().setMaxDailyDD(this.trader.getMaxDailyDD());
         var35.mainResult().setMaxDailyDDPct(this.trader.getMaxDailyDDPct());
         this.updateMaxOpenDD(var35);
         Explore var38 = this.backtester.getExplore();
         if (var38 != null) {
            var35.specialValues().set(SpecialValues.Explore, var38);
         }

         return var35;
      } finally {
         if (this.strategy != null && this.strategy.Stockpicker != null) {
            this.strategy.Stockpicker.clear();
         }

         if (this.strategy != null) {
            this.strategy.Stockpicker = null;
         }

         if (this.backtester != null) {
            this.backtester.clear();
         }
      }
   }

   private void checkBadStrategy(ResultsGroup var1) throws Exception {
      if (!this.isAlgowizard) {
         int var2 = var1.specialValues().getInt("StrategyProblems", 0);
         int var3 = SettingsMap.getInt(this.mainSettings.get("DismissBadStrategies"), 0);
         boolean var4 = SettingsMap.getBool(this.mainSettings.get("StrategyDismissWarnings"), false);
         OrdersList var6 = var1.orders();
         if (var6.size() < 20) {
            int var5 = BadStrategyException.setOrThrow(var3, var4, 2);
            if (var5 == 2) {
               var2 |= var5;
            }
         }

         int var7 = 0;
         int var8 = 0;
         int var9 = 0;
         int var10 = 0;
         int var11 = 0;

         for (int var12 = 0; var12 < var6.size(); var12++) {
            Order var13 = var6.get(var12);
            var8++;
            if (var13.isFilledOrder()) {
               var7++;
               if (var13.PL == 0.0F) {
                  var9++;
               }

               long var14 = var13.CloseTime - var13.OpenTime;
               if (var14 == 0L && var13.OpenPrice == var13.ClosePrice) {
                  var10++;
               }

               if (var13.BarsInTrade == 0) {
                  var11++;
               }
            }
         }

         if (var7 == 0) {
            int var16 = BadStrategyException.setOrThrow(var3, var4, 1);
            if (var16 == 1) {
               var2 |= var16;
            }
         }

         if (var7 < var8 * 0.1) {
            var2 |= BadStrategyException.setOrThrow(var3, var4, 256);
         }

         int var17 = var7 / 10;
         if (var17 > 100) {
            var17 = 100;
         }

         if (var9 > var17) {
            var2 |= BadStrategyException.setOrThrow(var3, var4, 4);
         }

         if (var10 > var17) {
            var2 |= BadStrategyException.setOrThrow(var3, var4, 16);
         }

         if (var11 > var17) {
            var2 |= BadStrategyException.setOrThrow(var3, var4, 8);
         }

         var17 = var7 / 20;
         if (var17 > 50) {
            byte var19 = 50;
         }

         var1.specialValues().set("StrategyProblems", var2);
      }
   }

   private void updateMaxOpenDD(ResultsGroup var1) {
      try {
         SQStats var2 = var1.portfolio().statsOrNull((byte)0, (byte)10, (byte)127);
         var2.set("OpenDrawdown", this.trader.getMaxOpenDrawdown());
         var2.set("OpenDrawdownPct", this.trader.getMaxOpenDrawdownPct());
         this.recalculateReturnOpenDDRatio(var2);
      } catch (Exception var3) {
         Log.error("Error while updating OpenDrawdown/OpenDrawdownPct stats...", var3);
      }
   }

   private void recalculateReturnOpenDDRatio(SQStats var1) {
      try {
         double var2 = var1.getDouble("NetProfit");
         double var4 = Math.abs(var1.getDouble("OpenDrawdown"));
         int var6 = var1.getInt("NumberOfTrades");
         if (var6 == 0) {
            var1.set("ReturnOpenDDRatio", 0.0);
            return;
         }

         if (var4 == 0.0) {
            if (var2 == 0.0) {
               var1.set("ReturnOpenDDRatio", 0.0);
            } else {
               var1.set("ReturnOpenDDRatio", 10.0);
            }

            return;
         }

         var1.set("ReturnOpenDDRatio", SQUtils.round2(SQUtils.safeDivide(var2, var4)));
      } catch (Exception var7) {
         Log.error("Error while recalculating ReturnOpenDDRatio...", var7);
      }
   }

   private void checkListeners() {
      if (this.lastEventListener == null) {
         this.setLastEventListener(new ILastEventListener() {
            @Override
            public void setLastEvent(String var1) {
            }
         });
      }

      if (this.backtestProgressListener == null) {
         this.registerProgressListener(new IBacktestProgressListener() {
            @Override
            public void setProgress(int var1) {
            }

            @Override
            public void increaseProgressStep() {
            }
         });
      }
   }

   private ResultsGroup emptyRg(String var1, int var2, double var3) throws Exception {
      ResultsGroup var5 = new ResultsGroup(var1);
      SettingsMap var6 = this.mainSettings;
      var5.symbols().add(this.mainSymbol, this.mainSymbolInfo.instrument, this.mainSymbolInfo.symbolInfo);
      Result var7 = new Result(this.mainSymbol, var5, var6);
      var7.setString(SpecialValues.Symbol, this.mainSymbol);
      var7.setString(SpecialValues.Timeframe, "D1");
      var5.addSubresult(this.mainSymbol, var6, var7);
      StrategyBase var8 = StrategyBase.getStrategy(var6);
      var5.portfolio().addStrategyXml(var8.getStrategyXml());
      var5.portfolio().addStrategy(var8);
      ChartSetup var9 = (ChartSetup)this.mainSettings.get("BacktestChart");
      ChartDef var10 = var9.getMainChart();
      var5.specialValues().set(SpecialValues.HistoryFrom, var10.getHistoryFrom());
      var5.specialValues().set(SpecialValues.HistoryTo, var10.getHistoryTo());
      var5.specialValues().set(SpecialValues.Precision, var9.getTestPrecision());
      var5.specialValues().set(SpecialValues.BacktestDuration, var3);
      var5.specialValues().set(SpecialValues.TotalTicks, var2);
      var5.specialValues().set(SpecialValues.LastModified, System.currentTimeMillis());
      var5.specialValues().set(SpecialValues.Complexity, 0);
      var5.setOOSSettings((OutOfSample)this.mainSettings.get("OutOfSample"));
      var5.computeAllStats();
      return var5;
   }

   private ResultsGroup createResultsGroup(String var1, ObjectArrayList<String> var2, OrdersList var3, int var4, double var5) throws Exception {
      ResultsGroup var7 = new ResultsGroup(var1);
      var7.setLastSettings(this.lastSettingsXml);
      SettingsMap var8 = this.mainSettings;
      var7.symbols().add(this.mainSymbol, this.mainSymbolInfo.instrument, this.mainSymbolInfo.symbolInfo);
      Result var9 = new Result(this.mainSymbol, var7, this.mainSettings);
      var9.setString(SpecialValues.Symbol, this.mainSymbol);
      var9.setString(SpecialValues.Timeframe, "D1");
      var7.addSubresult(this.mainSymbol, var8, var9);
      var3.sort(new OrderComparatorByTicket());
      this.recognizeOOS(var3, this.mainSettings.get("OutOfSample"), var1);
      var7.orders().addAll(var3);
      var3.clear();
      if (var7.getResultKeys().size() > 1) {
         var7.createPortfolioResult();
      }

      StrategyBase var10 = StrategyBase.getStrategy(var8);
      Element var11 = var10.getStrategyXml();
      this.modifyStrategyXml(var11);
      var7.portfolio().addStrategyXml(var11);
      var7.portfolio().addStrategy(var10);
      ChartSetup var12 = (ChartSetup)this.mainSettings.get("BacktestChart");
      ChartDef var13 = var12.getMainChart();
      var7.specialValues().set(SpecialValues.HistoryFrom, var13.getHistoryFrom());
      var7.specialValues().set(SpecialValues.HistoryTo, var13.getHistoryTo());
      var7.specialValues().set(SpecialValues.Precision, var12.getTestPrecision());
      var7.specialValues().set(SpecialValues.BacktestDuration, var5);
      var7.specialValues().set(SpecialValues.TotalTicks, var4);
      var7.specialValues().set(SpecialValues.LastModified, System.currentTimeMillis());
      var7.specialValues().set(SpecialValues.Complexity, 0);
      var7.setOOSSettings((OutOfSample)this.mainSettings.get("OutOfSample"));
      var7.computeAllStats();
      var7.mainResult().copyStats(var7.portfolio());
      if (this.storeChartData) {
         LoadedPickerData var14 = this.backtester.getPickerData();
         Stockpicker var15 = this.strategy.Stockpicker;
         var9.addTradingChartsData(var14, var15, var7.orders(), this.isAlgowizard);
      }

      var7.specialValues().set("StrategyProblems", this.strategy.getStrategyProblems());
      return var7;
   }

   private void modifyStrategyXml(Element var1) {
      if (!this.isAlgowizard) {
         try {
            Element var2 = var1.getChild("Strategy");
            var2.setAttribute("entryTriggeredAt", PickerTriggerTypes.getKey(this.strategy.Stockpicker.entryType));
            var2.setAttribute("exitTriggeredAt", PickerTriggerTypes.getKey(this.strategy.Stockpicker.exitType));
            List var3 = var2.getChild("Rules").getChild("Events").getChildren("Event");

            for (int var4 = 0; var4 < var3.size(); var4++) {
               Element var5 = (Element)var3.get(var4);
               List var6 = var5.getChildren("Rule");

               for (int var7 = 0; var7 < var6.size(); var7++) {
                  Element var8 = (Element)var6.get(var7);
                  String var9 = var8.getAttributeValue("type");
                  if (var9.equals("StockpickerEntryExit")) {
                     String var21 = var8.getAttributeValue("name");
                     byte var20;
                     if (var21.equalsIgnoreCase("Long")) {
                        var20 = 1;
                     } else {
                        var20 = -1;
                     }

                     Iterator var22 = var8.getChildren("Exit").iterator();
                     if (var22.hasNext()) {
                        Element var23 = (Element)var22.next();
                        String var24 = var23.getAttributeValue("atEndOfDay");
                        if (var24 != null) {
                           var24 = "false";
                           if (var20 == 1) {
                              if (this.strategy.Stockpicker.eodLong != 10) {
                                 var24 = "true";
                              }
                           } else if (this.strategy.Stockpicker.eodShort != 10) {
                              var24 = "true";
                           }

                           var23.setAttribute("atEndOfDay", var24);
                        }
                     }
                  } else if (var9.equals("StockpickerPS") && !this.strategy.Stockpicker.isSingleAssetStrategy()) {
                     String var11 = var8.getAttributeValue("name");
                     byte var10;
                     if (var11.equalsIgnoreCase("Position Score Long")) {
                        var10 = 1;
                     } else {
                        var10 = -1;
                     }

                     Element var12 = var8.getChild("Value");

                     for (Element var14 : var12.getChildren("Item")) {
                        List var15 = var14.getChildren("Param");
                        Element var16 = (Element)var15.get(0);
                        Element var17 = (Element)var15.get(1);
                        String var18 = var16.getAttributeValue("name");
                        if (var18.equals("MaxOpenPositions")) {
                           if (var10 == 1) {
                              var17.setText(this.strategy.Stockpicker.MaxOpenPositionsLong + "");
                           } else {
                              var17.setText(this.strategy.Stockpicker.MaxOpenPositionsShort + "");
                           }
                        }
                     }
                  }
               }
            }
         } catch (Exception var19) {
            throw new Error("Error while modifying strategy xml - " + var19.getMessage(), var19);
         }
      }
   }

   public static String createBasketName(String var0) {
      return var0.startsWith("[") && var0.endsWith("]") ? var0 : "[" + var0 + "]";
   }

   private void recognizeOOS(OrdersList var1, Object var2, String var3) throws Exception {
      OutOfSample var4 = null;
      if (var2 != null) {
         var4 = (OutOfSample)var2;
      }

      for (int var5 = 0; var5 < var1.size(); var5++) {
         Order var6 = var1.get(var5);
         var6.Ticket = var5 + 1;
         var6.SetupName = var3;
         if (var4 == null) {
            var6.SampleType = 11;
         } else {
            var6.SampleType = var4.getSampleType(var6);
         }
      }
   }

   @Override
   public void addSetup(SettingsMap var1) throws Exception {
      this.mainSettings = var1;
      this.isAlgowizard = var1.getBoolean("IsAlgoWizard", false);
      if (var1.containsKey("BacktestChart") && var1.get("BacktestChart") instanceof ChartSetup) {
         double var2 = this.getMinDistance(var1);
         this.strategy = StrategyBase.getStrategy(var1);
         if (this.strategy.wasUsed()) {
            this.strategy = this.strategy.clone();
         }

         this.strategy.isAlgoWizard = this.isAlgowizard;
         if (var1.containsKey("TradingOptions") && var1.get("TradingOptions") instanceof TradingOptions) {
            TradingOptions var4 = (TradingOptions)var1.get("TradingOptions");
            this.strategy.setTradingOptions(var4);
         }

         ChartSetup var10 = (ChartSetup)var1.get("BacktestChart");
         var10.setMinDistance(var2);
         ChartDef var5 = var10.getMainChart();
         this.setup = var10;
         this.mainSymbol = var5.getSymbol();
         this.mainSymbolInfo = DataManager.getDataInfo("History", this.mainSymbol, true);
         if (this.mainSymbolInfo == null) {
            throw new TradingException(String.format("Symbol with name '%s' doesn't exist.", this.mainSymbol));
         }

         if (this.mainSymbolInfo.originalSymbol != null) {
            this.mainSymbol = this.mainSymbolInfo.originalSymbol;
         }

         this.dayStart = SQTime.correctDayStart(var5.getHistoryFrom());
         this.strategy.setSettings(var1);
         this.strategy.callOnInit();
         if (!this.strategy.isStockpicker()) {
            throw new TradingException(String.format("Strategy '%s' is NOT Stockpicker strategy.", this.strategy.getStrategyName()));
         }

         if (this.strategy.Stockpicker.isSingleAssetStrategy()) {
            this.strategy.Stockpicker.MaxOpenPositionsLong = 1;
            this.strategy.Stockpicker.MaxOpenPositionsShort = 1;
         }

         this.MaxOpenPositionsLong = this.strategy.Stockpicker.MaxOpenPositionsLong;
         this.MaxOpenPositionsShort = this.strategy.Stockpicker.MaxOpenPositionsShort;
         this.trader.setMaxOpenPos(this.strategy.Stockpicker.MaxOpenPositionsLong, this.strategy.Stockpicker.MaxOpenPositionsShort);
         this.initialCapital = (Double)var1.get("MoneyManagement.InitialCapital");
         this.slippage = 0.0;
         if (var1.containsKey("Slippage") && var1.get("Slippage") instanceof Double || var1.get("Slippage") instanceof Integer) {
            this.slippage = var1.getDouble("Slippage");
         }

         if (var1.containsKey("Commission") && var1.get("Commission") instanceof CommissionsMethod) {
            this.commission = (CommissionsMethod)var1.get("Commission");
         }

         if (var1.containsKey("Swap") && var1.get("Swap") instanceof SwapMethod) {
            this.swap = (SwapMethod)var1.get("Swap");
         }

         this.tradeController.setStrategy(this.strategy);
         this.backtester.addSetup(var1);
         this.backtester.setStrategy(this.strategy);
         StockpickerOptions var6 = this.strategy.getStockpickerOptions();
         this.pickerLog.storeLogs(var6 != null ? var6.storeLogs() : true);
         this.AllowBetterLimitFill = var6 != null ? var6.allowBetterLimitFill() : false;
         this.LimitOver = 0.0;
         TradingOption[] var7 = this.strategy.getTradingOptions();
         if (var7 != null) {
            for (int var8 = 0; var8 < var7.length; var8++) {
               TradingOption var9 = var7[var8];
               if (!MainApp.isBacktestNode() && var9 instanceof StoreChartData) {
                  this.storeChartData = ((StoreChartData)var9).StoreChartData;
               } else if (var9 instanceof LimitOver) {
                  this.LimitOver = ((LimitOver)var9).LimitOver;
               }
            }
         }

         this.backtester.storeChartData(this.storeChartData);
         this.backtester.setDataModifierCallback(this.dataModifierCallback);
      } else {
         throw new TradingException("Setting 'SettingsKeys.BacktestChart' is not set or has incorrect value! It must be an instance of ChartSetup object.");
      }
   }

   private double getMinDistance(SettingsMap var1) throws TradingException {
      if (var1.containsKey("MinDistance")) {
         try {
            return (Double)var1.get("MinDistance");
         } catch (ClassCastException var4) {
            try {
               return ((Integer)var1.get("MinDistance")).intValue();
            } catch (ClassCastException var3) {
               throw new TradingException("Setting 'TradingSetup.MinimumDistance was set, but it has incorrect value! It must be an int or double number!");
            }
         }
      } else {
         return 0.0;
      }
   }

   private boolean checkStopped() {
      return this.stopPauseEngine.isStopped();
   }

   @Override
   public void registerProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
      this.backtester.registerProgressListener(var1);
      this.trader.registerProgressListener(var1);
   }

   @Override
   public void setStopPauseEngine(StopPauseEngine var1) {
      this.stopPauseEngine = var1;
   }

   public void setLastEventListener(ILastEventListener var1) {
      this.lastEventListener = var1;
   }

   public void addStrategyXml(Element var1) {
      this.elStrategy = var1;
   }

   public static ChartSetup createTempStockGroup(ChartSetup var0) throws Exception {
      String var1 = var0.getMainChart().getSymbol();
      String var2 = null;
      DataInfo var4 = DataManager.getDataInfo("History", var1);
      if (var4 == null) {
         throw new Exception(L.t("Data for connection '%s' and symbol '%s' cannot be found!", new Object[]{"History", var1}));
      }

      var2 = createBasketName(var1);
      int var3 = Math.abs(var2.hashCode()) * -1;
      BasketDto var5 = BasketOfStocksManager.getInstance().getBasket(var3);
      if (var5 == null) {
         var5 = BasketOfStocksManager.getInstance().createCustomGroup(var2, var3, var4.symbol, true, null);
      }

      var4 = DataManager.getDataInfo("History", var2);
      if (var4 == null) {
         DataManager.addCustomData("History", var2, var3, null);
      }

      return var0.getClone(var2);
   }

   @Override
   public void setLastSettings(String var1) {
      this.lastSettingsXml = var1;
   }

   @Override
   public int getEngineId() {
      return 0;
   }

   @Override
   public BacktestEngine runBacktest() throws Exception {
      ResultsGroup var1 = this.runBacktest("StockpickerBacktest");
      BacktestEngine var2 = new BacktestEngine(null);
      var2.resultsGroup = var1;
      return var2;
   }

   @Override
   public double getGlobalATR() {
      return 0.0;
   }

   @Override
   public BacktestEngine runBacktest(String var1, String var2, boolean var3) throws Exception {
      ResultsGroup var4 = this.runBacktest(var1);
      BacktestEngine var5 = new BacktestEngine(null);
      var5.resultsGroup = var4;
      return var5;
   }
}
