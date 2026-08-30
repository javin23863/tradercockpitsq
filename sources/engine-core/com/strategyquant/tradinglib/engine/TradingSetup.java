package com.strategyquant.tradinglib.engine;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.utils.SQUUID;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.setup.BarUpdateInfo;
import com.strategyquant.tradinglib.setup.TradingSetupRunnable;
import com.strategyquant.tradinglib.strategy.MarketData;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TradingSetup {
   public static final Logger Log = LoggerFactory.getLogger("TradingSetup");
   protected SettingsMap settings;
   protected ChartSetup chartSetup;
   protected StrategyBase strategy;
   protected TradingSetupRunnable runnable;
   protected TradingEngine tradingEngine;
   protected int tradingType;
   private MarketData MarketData;
   private volatile boolean inInit = false;
   private int reservedBars = 50;
   private int status = 0;
   private final int setupId;
   private int numberOfStartingBars = 0;
   private String setupName;
   private int userDefinedStartingBar;
   private int engineId;
   private long strategyCheckDate = -1L;
   private int dismissBadStrategies = 0;
   private boolean warningsBadStrategies = false;
   private IBacktestProgressListener backtestProgressListener = null;
   private MarketData loadedMarketData;
   protected boolean usePreparedData = true;
   private double globalATR = -1.0;
   private int barEventType;
   protected int indyStartingBar = 0;

   TradingSetup(TradingEngine var1, int var2, ChartSetup var3, SettingsMap var4, StrategyBase var5) {
      this.tradingType = 0;
      this.runnable = new TradingSetupRunnable(this, this.usePreparedData);
      this.settings = this.createDefaultSettingsMap(var4);
      this.tradingEngine = var1;
      this.engineId = var2;
      this.chartSetup = var3;
      this.strategy = var5;
      if (var3 != null) {
         var3.setUsed();
      }

      var5.setSettings(this.settings);
      String var6 = SQUUID.randomUUID();
      this.setupId = var6.hashCode();
      if (var5 != null) {
         this.setupName = var5.getStrategyName();
      }

      this.initInternalSettings();
   }

   protected void initInternalSettings() {
      this.userDefinedStartingBar = 0;
      if (this.settings != null) {
         this.userDefinedStartingBar = this.settings.getInt("StartingBar", 0);
         if (this.userDefinedStartingBar < 0) {
            this.userDefinedStartingBar = 0;
         }

         int var1 = this.settings.getInt("ReservedBars", 0);
         this.userDefinedStartingBar = this.userDefinedStartingBar + (this.engineId != 56756755 && this.engineId != 938213070 ? 0 : var1);
         this.indyStartingBar = this.userDefinedStartingBar;
      }

      if (this.settings.containsKey("StrategyCheckDate")) {
         this.strategyCheckDate = this.settings.getLong("StrategyCheckDate");
      }

      if (this.settings.containsKey("DismissBadStrategies")) {
         this.dismissBadStrategies = this.settings.getInt("DismissBadStrategies");
      } else if (this.settings.containsKey("StrategyDismissSettings")) {
         this.dismissBadStrategies = this.settings.getInt("StrategyDismissSettings");
      }

      if (this.settings.containsKey("StrategyDismissWarnings")) {
         this.warningsBadStrategies = this.settings.getBoolean("StrategyDismissWarnings");
      }
   }

   public String getName() {
      return this.setupName;
   }

   public void setName(String var1) {
      this.setupName = var1;
   }

   void setTradingType(int var1) {
      this.tradingType = var1;
   }

   public void initDefaultValues() {
   }

   private SettingsMap createDefaultSettingsMap(SettingsMap var1) {
      if (var1 == null) {
         var1 = new SettingsMap();
      }

      var1.setIfNotExists("BacktestPrecision", 1);
      var1.setIfNotExists("Slippage", 0);
      var1.setIfNotExists("MinDistance", 0);
      var1.setIfNotExists("MoneyManagement.InitialCapital", 10000.0);
      return var1;
   }

   public void start(boolean var1) throws Exception {
      if (this.usePreparedData) {
         this.MarketData = this.loadedMarketData.cloneToPrepared(this, this.chartSetup, this.indyStartingBar);
         if (this.strategy != null) {
            this.MarketData
               .setPerformanceParams(
                  this.strategy.hasOnTickRule(), this.strategy.hasDailyDataBlock(), this.strategy.hasWeeklyDataBlock(), this.strategy.hasMonthlyDataBlock()
               );
         }
      } else {
         this.MarketData = new MarketData(this, this.chartSetup, 0);
      }

      this.inInit = true;
      this.strategy.callOnInit(this);
      this.inInit = false;
      if (this.tradingType == 0) {
         this.runChecks();
         this.registerConnectionSymbols();
         Log.debug("Before loading history data, MarketData.getMainChartBars() = " + this.MarketData.Chart(0).Time.size());
         this.requestHistoryData(TradingEngine.getLiveEngine().getConnectionManager());
         Log.debug("After loading history data, MarketData.getMainChartBars() = " + this.MarketData.Chart(0).Time.size());
         this.numberOfStartingBars = this.MarketData.Chart(0).Time.size();
         if (this.numberOfStartingBars > 0 && Log.isDebugEnabled()) {
            Log.debug("Last bar time: {}", SQTime.toDateMinuteString(this.MarketData.Chart(0).Time(0)));
         }
      }

      this.status = 1;
      if (!var1) {
      }
   }

   private void requestHistoryData(ConnectionManager var1) throws Exception {
      this.MarketData.requestHistoryData(var1, this.reservedBars);
   }

   private void registerConnectionSymbols() throws Exception {
      this.chartSetup.registerConnectionSymbols(this.tradingEngine.getConnectionManager());
   }

   public void stop() throws Exception {
      this.runnable.stopRunner();
      if (this.strategy != null) {
         this.strategy.Deinitialize();
         this.strategy.destroy();
      }

      this.status = 2;
   }

   public ChartSetup getChartSetup() {
      return this.chartSetup;
   }

   public TradingEngine getTradingEngine() {
      return this.tradingEngine;
   }

   public void runChecks() throws Exception {
      if (this.tradingEngine == null) {
         throw new TradingException("Canot call TradingSetup.runChecks() before initializing TradingSetup.tradingEngine !");
      }

      this.chartSetup.runChecks(this.tradingEngine.getConnectionManager());
   }

   public MarketData getMarketData() {
      return this.MarketData;
   }

   public boolean isInInit() {
      return this.inInit;
   }

   public boolean callStrategyOnBarUpdate(BarUpdateInfo var1, int var2) throws Exception {
      if (!var1.updatedMainChart) {
         return true;
      }

      this.strategy.UpdatedChart = var1.updatedChart;
      this.strategy.MultipleChartsUpdated = var1.multipleChartsUpdated;
      this.strategy.UpdateEventType = var2;
      this.strategy.updateTradeControllers();
      this.strategy.checkBadStrategy();
      this.strategy.updateJobProgress();
      boolean var3 = false;
      if (this.chartSetup.getTestPrecision() == 5) {
         var3 = var2 == 2;
      } else if (this.strategy.getEventType() == 4) {
         if (this.barEventType == 2) {
            if (var2 != 3) {
               var3 = true;
            }
         } else if (var2 == 3) {
            var3 = true;
         }
      } else if (this.strategy.getEventType() == 6) {
         var3 = true;
      } else if (this.strategy.getEventType() == var2) {
         var3 = true;
      }

      if (var3) {
         if (var2 == 3) {
            if (this.userDefinedStartingBar <= 0) {
            }

            this.reservedBars = 0;
            boolean var4 = true;

            for (int var5 = 0; var5 < this.MarketData.getChartsCount(); var5++) {
               ChartData var6 = this.MarketData.Chart(var5);
               if (var6.Bars() <= this.userDefinedStartingBar) {
                  var4 = false;
               }
            }

            if (var4) {
               ChartData var8 = this.MarketData.Chart(0);
               this.strategy.OnBarUpdate();
               if (this.globalATR < 0.0) {
                  this.globalATR = this.computeATR(var8);
               }

               return true;
            }
         } else {
            ChartData var7 = this.MarketData.Chart(0);
            if (var7.Bars() > this.reservedBars + this.userDefinedStartingBar) {
               var7.setCurrentBar(this.reservedBars, this.userDefinedStartingBar);
               this.strategy.OnBarUpdate();
               if (this.globalATR < 0.0) {
                  this.globalATR = this.computeATR(var7);
               }

               return true;
            }
         }
      }

      return true;
   }

   public void callOptionsOnTick(TickEvent var1) throws Exception {
      this.strategy.callOptionsOnTick(var1);
   }

   public void callExitEOD(TickEvent var1) throws Exception {
      this.strategy.callExitEOD(var1);
   }

   public Exception getTradingException() {
      return this.runnable.getTradingException();
   }

   public boolean isRunning() {
      return this.runnable.isAlive();
   }

   public void setReservedBars(int var1) {
      this.reservedBars = var1;
   }

   public int getReservedBars() {
      return this.reservedBars;
   }

   public StrategyBase getStrategy() {
      return this.strategy;
   }

   public int getStatus() {
      return this.status;
   }

   public int getSetupId() {
      return this.setupId;
   }

   public int getStartingBars() {
      return this.numberOfStartingBars;
   }

   public HashMap<String, InstrumentInfo> getInstrumentsInfoMap() {
      HashMap var1 = new HashMap();

      for (ChartDef var3 : this.getChartSetup().getCharts()) {
         if (!var1.containsKey(var3.getSymbol())) {
            var1.put(var3.getSymbol(), var3.getSymbolInfo());
         }
      }

      return var1;
   }

   public SettingsMap getSettings() {
      return this.settings;
   }

   public void destroy() {
      if (this.MarketData != null) {
         this.MarketData.destroy();
      }
   }

   public int getEngineId() {
      return this.engineId;
   }

   public long getStrategyCheckDate() {
      return this.strategyCheckDate;
   }

   public int getDismissBadStrategies() {
      return this.dismissBadStrategies;
   }

   public boolean getWarningsBadStrategies() {
      return this.warningsBadStrategies;
   }

   public void setProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
   }

   public void notifyAboutProgress(int var1) {
      if (this.backtestProgressListener != null) {
         this.backtestProgressListener.setProgress(var1);
      }
   }

   public void setLoadedMarketData(MarketData var1) {
      this.loadedMarketData = var1;
   }

   public void setUsePreparedData(boolean var1) {
      this.usePreparedData = var1;
   }

   public double getGlobalATR() {
      return this.globalATR;
   }

   private double computeATR(ChartData var1) throws TradingException {
      if (var1.High.size() <= 20) {
         return 0.0;
      }

      double var2 = 0.0;

      for (int var4 = 0; var4 < 19; var4++) {
         var2 = this.computeATROnBar(var4, var2, var1);
      }

      return var2;
   }

   private double computeATROnBar(int var1, double var2, ChartData var4) throws TradingException {
      byte var5 = 14;
      int var6 = 18 - var1;
      double var7 = var4.High.get(var6) - var4.Low.get(var6);
      if (var1 == 0) {
         return var7;
      }

      double var9 = var4.Close.get(var6 + 1);
      var7 = Math.max(Math.abs(var4.Low.get(var6) - var9), Math.max(var7, Math.abs(var4.High.get(var6) - var9)));
      return ((Math.min(var1 + 1, var5) - 1) * var2 + var7) / Math.min(var1 + 1, var5);
   }

   public void setBarEventType(int var1) {
      this.barEventType = var1;
      this.runnable.setBarEventType(var1);
   }

   public int getBarEventType() {
      return this.barEventType;
   }

   public int getIndyStartingBar() {
      return this.indyStartingBar;
   }
}
