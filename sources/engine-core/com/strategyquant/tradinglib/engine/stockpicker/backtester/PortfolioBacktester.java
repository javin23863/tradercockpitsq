package com.strategyquant.tradinglib.engine.stockpicker.backtester;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.log.StockpickerLog;
import com.strategyquant.tradinglib.engine.stockpicker.data.LoadedPickerData;
import com.strategyquant.tradinglib.engine.stockpicker.data.PickerDataCache;
import com.strategyquant.tradinglib.engine.stockpicker.data.PickerDataModifier;
import com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups.LoadedStockGroupData;
import com.strategyquant.tradinglib.engine.stockpicker.signals.CollectedSignals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignal;
import com.strategyquant.tradinglib.explore.Explore;
import com.strategyquant.tradinglib.options.parameters.BrokerOption;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.robustnesstests.DataModifierCallback;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioBacktester {
   public static final Logger Log = LoggerFactory.getLogger(PortfolioBacktester.class);
   private ObjectArrayList<String> symbolsBacktested = new ObjectArrayList();
   private StopPauseEngine stopPauseEngine;
   private ILastEventListener lastEventListener;
   private StrategyBase strategy;
   private int lastIndex;
   private Element elStrategy;
   private IBacktestProgressListener backtestProgressListener;
   private BasketDto dto;
   private ChartSetup chartSetup;
   private LoadedPickerData loadedPickerData;
   private CollectedSignals signals = new CollectedSignals();
   private int totalTicks;
   private SettingsMap mainSettings;
   private int maxEntrySignalsLong;
   private int maxEntrySignalsShort;
   private Comparator<PickerEntrySignal> entrySignalComparatorByScore;
   double total;
   double processed;
   private int progress;
   private int lastProgress = 0;
   private boolean storeChartData = false;
   private StockpickerLog pickerLog;
   protected DataModifierCallback dataModifierCallback;

   public PortfolioBacktester(StockpickerLog var1) {
      this.pickerLog = var1;
   }

   public void backtest() throws Exception {
      long var1 = System.currentTimeMillis();
      this.beforeStart();
      Log.debug("Running on grid...");
      this.runBacktests();
      Log.debug("Finished running on grid...");
      long var3 = System.currentTimeMillis() - var1;
      String var5 = SQTimeOld.formatDateTime((int)(var3 / 1000L));
      Log.debug("Collecting trading signals done in {}", var5);
   }

   public void beforeStart() throws Exception {
      this.backtestProgressListener.setProgress(1);
      this.loadSettings(this.strategy.getSettings());
      this.loadData();
      this.backtestProgressListener.setProgress(25);
   }

   private void loadSettings(SettingsMap var1) throws Exception {
      this.chartSetup = (ChartSetup)var1.get("BacktestChart");
      ChartDef var2 = this.chartSetup.getMainChart();
      String var3 = var2.getSymbol();
      DataInfo var4 = DataManager.getDataInfo("History", var3, true);
      if (var4 == null) {
         throw new DataException(2, "Data for connection 'History' and symbol '" + var3 + "' cannot be found!");
      }

      int var5 = var4.basketId;
      this.dto = BasketOfStocksManager.getInstance().getBasket(var5);
      if (this.dto == null) {
         throw new DataException(2, String.format("Group with id %d doesn't exist", var5));
      }

      if (!DataManager.isGroupAlias(var3)) {
         throw new DataException(2, String.format("Main chart symbol '%s' must be a group alias.", var3));
      }

      for (int var7 = 1; var7 < this.chartSetup.getCharts().size(); var7++) {
         ChartDef var6 = this.chartSetup.getCharts().get(var7);
         if (!var6.getTimeframe().equals("D1") && !var6.getTimeframe().equals("Weekly") && !var6.getTimeframe().equals("Monthly")) {
            throw new Exception(
               L.t(
                  "Invalid Additional chart %d - %s/%s definition. TF must be one of: %s, %s, %s.",
                  new Object[]{var7, var6.getSymbol(), var6.getTimeframe(), "D1", "Weekly", "Monthly"}
               )
            );
         }
      }
   }

   public void loadData() throws Exception {
      PickerDataCache var1 = PickerDataCache.getInstance();
      var1.registerProgressListener(this.backtestProgressListener);
      this.loadedPickerData = var1.loadData(this.chartSetup, this.dto, this.stopPauseEngine, this.lastEventListener);
      if (this.loadedPickerData.stockGroupData.getSymbolsCount() == 0) {
         throw new Exception(L.t("No data found.", new Object[0]));
      }

      this.modifyData();
   }

   private void modifyData() throws TradingException {
      if (this.dataModifierCallback != null) {
         this.loadedPickerData = this.loadedPickerData.clone();
         long var1 = this.chartSetup.getMainChart().getHistoryFrom();
         long var3 = this.chartSetup.getMainChart().getHistoryTo();
         PickerDataModifier var5 = new PickerDataModifier();
         var5.modifyData(this.loadedPickerData, this.dataModifierCallback, var1, var3);
      }
   }

   public void setDataModifierCallback(DataModifierCallback var1) {
      this.dataModifierCallback = var1;
   }

   private void runBacktests() throws Exception {
      this.lastIndex = 0;
      ChartDef var1 = this.chartSetup.getMainChart();
      long var2 = var1.getHistoryFrom();
      long var4 = var1.getHistoryTo();
      List var6 = this.getBrokerSymbols();
      List var7 = BasketOfStocksManager.getInstance().getStocks(this.dto.getId(), var6);
      this.total = var7.size();
      this.processed = 0.0;
      this.totalTicks = 0;
      BacktestData var8 = new BacktestData(this.chartSetup, this.loadedPickerData, this.signals);
      String var9 = UUID.randomUUID().toString().replace("-", "");
      Log.debug("Running stockpicker backtest from {} to {} for {} symbols.", new Object[]{SQTime.toDateString(var2), SQTime.toDateString(var4), this.total});
      if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
         this.pickerLog
            .print(
               String.format(
                  "\nRunning stockpicker backtest from %s to %s for %d symbols.",
                  SQTime.toDateString(var2),
                  SQTime.toDateString(var4),
                  Double.valueOf(this.total).longValue()
               )
            );
      }

      for (int var10 = 0; var10 < this.total; var10++) {
         if (this.stopPauseEngine.isStopped()) {
            throw new TaskStoppedException();
         }

         try {
            PortfolioBacktestJobResult var11 = this.runOneBacktest(var10, var7, var2, var4, this.strategy, var9, var8);
            this.processed++;
            this.progress = (int)(this.processed / this.total / 2.5 * 100.0) + 25;
            if (this.backtestProgressListener != null && this.progress != this.lastProgress && this.progress % 10.0 == 0.0) {
               this.backtestProgressListener.setProgress(this.progress);
               this.lastProgress = this.progress;
            }

            if (var11 != null) {
               this.totalTicks = this.totalTicks + var11.totalTicks;
               if (!this.symbolsBacktested.contains(var11.symbol)) {
                  this.symbolsBacktested.add(var11.symbol);
               }
            }
         } catch (Exception var12) {
            if (!(var12 instanceof TaskStoppedException)) {
               Log.error("PortfolioBacktester Error {}, Exception ", var12.getMessage(), var12);
               throw var12;
            }
         }
      }
   }

   private List<String> getBrokerSymbols() throws DataException {
      String var1 = null;
      TradingOption[] var2 = this.strategy.getTradingOptions();
      if (var2 != null) {
         for (int var3 = 0; var3 < var2.length; var3++) {
            TradingOption var4 = var2[var3];
            if (var4 instanceof BrokerOption) {
               var1 = ((BrokerOption)var4).PickerBroker;
               break;
            }
         }
      }

      if (var1 != null && !var1.equals("No filter")) {
         List var10 = new ArrayList();
         if (MainApp.isBacktestNode()) {
            String var11 = this.mainSettings.getString("BrokerSymbols", null);
            if (var11 == null) {
               return null;
            }

            String[] var5 = var11.split("\n");

            for (String var9 : var5) {
               var9 = var9.trim();
               if (!var9.trim().isEmpty()) {
                  var10.add(var9);
               }
            }
         } else {
            BrokerDto var12 = BrokerManager.getInstance().getBroker(var1);
            if (var12 == null) {
               throw new DataException(2, String.format("Broker with name %s doesn't exist", var1));
            }

            var10 = BrokerManager.getInstance().getStocks(var12.getId());
         }

         return var10;
      } else {
         return null;
      }
   }

   private PortfolioBacktestJobResult runOneBacktest(int var1, List<StockDto> var2, long var3, long var5, StrategyBase var7, String var8, BacktestData var9) throws Exception {
      PortfolioBacktestJob var10 = this.createBacktestJob(var1, var2, var3, var5, var7, var8, var9);
      return var10 != null ? var10.call() : null;
   }

   private PortfolioBacktestJob createBacktestJob(int var1, List<StockDto> var2, long var3, long var5, StrategyBase var7, String var8, BacktestData var9) throws TradingException {
      StockDto var10 = (StockDto)var2.get(var1);
      DataInfo var11 = DataManager.getDataInfo("History", var10.getTicker(), true);
      if (var11 != null && var11.rows >= 1) {
         String var12 = var11.symbol;
         if (!this.loadedPickerData.symbolDataExists(var12)) {
            Log.debug("Data for symbol '" + var12 + "' not found.");
            return null;
         }

         if (Stockpicker.isDebugModeEnabled()) {
            Log.debug("Collecting trading signals for ticker {}", var12);
         }

         this.lastEventListener.setLastEvent(L.t("Collecting trading signals for ticker '%s'.", new Object[]{var12}));
         var7.setSymbol(var12);
         var7.setInstrumentInfo(var11.symbolInfo);
         if (!this.storeChartData) {
            var7.Stockpicker.TALibIndicators.clear();
         }

         return new PortfolioBacktestJob(
            var12,
            this.stopPauseEngine,
            this.lastEventListener,
            var7,
            var9,
            this.elStrategy,
            var8,
            var3,
            var5,
            var10,
            this.maxEntrySignalsLong,
            this.maxEntrySignalsShort,
            this.entrySignalComparatorByScore,
            this.pickerLog
         );
      } else {
         Log.debug("Data for symbol '" + var10.getTicker() + "' cannot be found!");
         return null;
      }
   }

   public ObjectArrayList<String> getSymbolsBacktested() {
      return this.symbolsBacktested;
   }

   public void setStopPauseEngine(StopPauseEngine var1) {
      this.stopPauseEngine = var1;
   }

   public void setLastEventListener(ILastEventListener var1) {
      this.lastEventListener = var1;
   }

   public void addSetup(SettingsMap var1) throws Exception {
      this.mainSettings = var1;
   }

   public void setStrategy(StrategyBase var1) {
      this.strategy = var1;
   }

   public void setStrategyXml(Element var1) {
      this.elStrategy = var1;
   }

   public void registerProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
   }

   public LoadedPickerData getPickerData() {
      return this.loadedPickerData;
   }

   public LoadedStockGroupData getStockGroupData() {
      return this.loadedPickerData.stockGroupData;
   }

   public int getTotalSymbolsLoaded() {
      return this.loadedPickerData.stockGroupData.getSymbolsCount();
   }

   public CollectedSignals getSignals() {
      return this.signals;
   }

   public Explore getExplore() {
      return this.strategy.Explore;
   }

   public int getTotalTicks() {
      return this.totalTicks * 4;
   }

   public void setReducer(int var1, int var2, Comparator<PickerEntrySignal> var3) {
      this.maxEntrySignalsLong = var1;
      this.maxEntrySignalsShort = var2;
      this.entrySignalComparatorByScore = var3;
   }

   public void storeChartData(boolean var1) {
      this.storeChartData = var1;
   }

   public void clear() {
      if (this.signals != null) {
         this.signals.clear();
      }

      if (this.dataModifierCallback != null) {
         this.loadedPickerData.clear();
         this.loadedPickerData = null;
      }
   }
}
