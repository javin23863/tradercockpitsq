package com.strategyquant.tradinglib.engine;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Trader;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.backtest.BacktestDataFeed;
import com.strategyquant.tradinglib.connection.ConnectionManager;
import com.strategyquant.tradinglib.exception.StrategyStoppedException;
import com.strategyquant.tradinglib.execution.IExecutionEngine;
import com.strategyquant.tradinglib.optimization.ParametersSettings;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;
import com.strategyquant.tradinglib.setup.Portfolio;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import com.strategyquant.tradinglib.strategy.MarketData;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import com.strategyquant.tradinglib.strategy.TraderUtils;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktestEngine extends AbstractBacktestEngine {
   public static final Logger Log = LoggerFactory.getLogger("BacktestEngine");
   private static final String KEY_SEPARATOR = "/";
   private Portfolio portfolio = new Portfolio();
   private SettingsMap stopwatchData;
   private TradingEngine tradingEngine;
   private BacktestDataFeed backtestDataFeed;
   private ITradingSimulator defaultTradingSimulator;
   private HashMap<String, ITradingSimulator> tradingSimulators = new HashMap<>();
   public ResultsGroup resultsGroup = null;
   private int dismissBadStrategies = 0;
   private boolean warningsBadStrategies = false;
   private OutOfSample outOfSampleSettings = null;
   public static final AtomicLong backtestsCount = new AtomicLong(0L);
   private boolean isAlgowizard = false;

   public BacktestEngine(ITradingSimulator var1) {
      this.defaultTradingSimulator = var1;
   }

   @Override
   public void addSetup(SettingsMap var1) throws Exception {
      this._addSetup(var1);
   }

   private String _addSetup(SettingsMap var1, String... var2) throws Exception {
      if (var1.containsKey("BacktestChart") && var1.get("BacktestChart") instanceof ChartSetup) {
         var1.set("ApplyExitsAtTheEndOfRule", this.defaultTradingSimulator.getApplyExitsAtTheEndOfRule());
         double var3 = this.getMinDistance(var1);
         StrategyBase var5 = StrategyBase.getStrategy(var1);
         if (var5.wasUsed()) {
            var5 = var5.clone();
         }

         this.isAlgowizard = var1.getBoolean("IsAlgoWizard", false);
         if (var1.containsKey("TradingOptions") && var1.get("TradingOptions") instanceof TradingOptions) {
            TradingOptions var6 = (TradingOptions)var1.get("TradingOptions");
            Variable var7 = var5.variables().get("Day_Trade_On");
            if (var7 != null && var7.getValueAsInt() == 1) {
               for (int var8 = 0; var8 < var6.size(); var8++) {
                  TradingOption var9 = var6.get(var8);
                  if (var9.getClass().getSimpleName().equals("ExitAtEndOfDay")) {
                     var9.setParameterValue("ExitAtEndOfDay", "true");
                     var9.setParameterValue("EODExitTime", "0");
                     break;
                  }
               }
            }

            var5.setTradingOptions(var6);
         }

         if (var1.containsKey("ATM")) {
            ATM var10 = (ATM)var1.get("ATM");
            if (var10 instanceof ATM) {
               var5.setATM(var10);
            }
         }

         ChartSetup var11 = (ChartSetup)var1.get("BacktestChart");
         var11.setMinDistance(var3);
         String var12;
         if (var2.length == 0) {
            var2 = new String[]{var11.getCharts().get(0).getConnectionName()};
            var12 = var2[0];
         } else {
            var12 = var2[0];
         }

         var5.setTradeControllers(var12, this.getTradeControllers(var2, var1));
         BacktestTradingSetup var13 = new BacktestTradingSetup(this, var11, var1, var5);
         if (var1.containsKey("DismissBadStrategies")) {
            this.dismissBadStrategies = var1.getInt("DismissBadStrategies");
         }

         if (var1.containsKey("StrategyDismissWarnings")) {
            this.warningsBadStrategies = var1.getBoolean("StrategyDismissWarnings");
         }

         this.outOfSampleSettings = (OutOfSample)var1.get("OutOfSample");
         return this.portfolio.add(var13);
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

   private Trader[] getTradeControllers(String[] var1, SettingsMap var2) throws DataException, TradingException {
      Trader[] var3 = new Trader[var1.length];

      for (int var4 = 0; var4 < var1.length; var4++) {
         String var5 = var1[var4];
         var3[var4] = new Trader(var5, this.getTradingSimulator(var5, var2));
      }

      return var3;
   }

   private IExecutionEngine getTradingSimulator(String var1, SettingsMap var2) throws TradingException {
      if (!this.tradingSimulators.containsKey(var1)) {
         ITradingSimulator var3 = this.defaultTradingSimulator.clone();
         var3.setConnection(var1);
         var3.initSettings(var2);
         this.tradingSimulators.put(var1, var3);
      }

      return this.tradingSimulators.get(var1);
   }

   @Override
   public BacktestEngine runBacktest() throws Exception {
      return this.runBacktest(null, null, false);
   }

   public BacktestEngine runBacktest(boolean var1) throws Exception {
      return this.runBacktest(null, null, var1);
   }

   @Override
   public BacktestEngine runBacktest(String var1, String var2, boolean var3) throws Exception {
      return this.runBacktest(var1, var2, var3, false);
   }

   public BacktestEngine runBacktest(String var1, String var2, boolean var3, boolean var4) throws Exception {
      long var5 = System.currentTimeMillis();
      this.stopwatchData = new SettingsMap();

      try {
         this.initializeBacktest();
         this.backtestDataFeed.start();
         if (var3 || this.loadDataProgressEngine == null) {
            long var7 = System.currentTimeMillis();
            this.computeResults(var1, var2, var7 - var5, var4);
            this.initGlobalATR();
            if (Log.isDebugEnabled()) {
               Log.debug("Backtest finished in : {} ms.", var7 - var5);
               Log.debug("Stats computation finished in : {} ms.", System.currentTimeMillis() - var7);
            }

            this.stopwatchData.set("TotalRunTime", System.currentTimeMillis() - var5);
         }
      } catch (Exception var12) {
         if (var12 instanceof TaskStoppedException) {
            Log.info("Task stopped");
            throw var12;
         }

         if (!(var12 instanceof StrategyStoppedException)) {
            throw var12;
         }

         Log.info("Strategy stopped manually with reason: " + var12.getMessage());
      } finally {
         this.destroyRemainingObjects();
         this.portfolio.deinitialize();
         this.transferSpecialValuesToResultsGroup();
         if (this.tradingEngine != null) {
            this.tradingEngine.deinitialize();
         }

         backtestsCount.incrementAndGet();
      }

      return this;
   }

   private void transferSpecialValuesToResultsGroup() {
      if (this.resultsGroup != null && this.portfolio != null) {
         TradingSetup var1 = this.portfolio.getFirstTradingSetup();
         if (var1 != null) {
            StrategyBase var2 = var1.getStrategy();
            if (var2 != null) {
               SettingsMap var3 = var2.getSpecialValues();
               if (var3 != null) {
                  for (String var7 : var3.getAllKeys()) {
                     if (var7 != null) {
                        Object var8 = var3.get(var7);
                        if (var8 != null) {
                           this.resultsGroup.specialValues().set(var7, var8);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void initGlobalATR() {
      TradingSetup var1 = this.portfolio.getTradingSetups().get(0);
      this.globalATR = var1.getGlobalATR();
   }

   public ResultsGroup getResults() {
      return this.resultsGroup;
   }

   private void computeResults(String var1, String var2, long var3, boolean var5) throws Exception {
      if (this.resultsGroup == null) {
         SettingsMap var6 = this.portfolio.getFirstTradingSetup().getSettings();
         StrategyBase var7 = this.portfolio.getFirstTradingSetup().getStrategy();
         if (var1 == null) {
            var1 = (String)var6.get("ResultGroupName");
         }

         if (var1 == null) {
            var1 = (String)var6.get("StrategyName");
         }

         if (var1 == null) {
            var1 = var7.getStrategyName();
         }

         this.resultsGroup = new ResultsGroup(var1);
         this.resultsGroup.setLastSettings(this.lastSettingsXml);
         if (var7.Explore != null) {
            this.resultsGroup.specialValues().set(SpecialValues.Explore, var7.Explore);
         }

         OrdersList var8 = TraderUtils.getHistoryOrders(var7.Trader("History"));
         if (var5) {
            var8.removeEndOfTest();
         }

         var8.sort(new OrderComparatorByOpenTime());
         long var9 = -1L;
         int var11 = 0;
         if (var6.containsKey("StrategyCheckDate")) {
            var9 = var6.getLong("StrategyCheckDate");
         }

         if (var9 > 0L) {
            var11 = (int)(this.portfolio.getFirstTradingSetup().getMarketData().Chart(0).Bars() * 0.3);
         }

         if (BadStrategyException.check(this.dismissBadStrategies, 1) && var8.size() == 0) {
            this.destroyRemainingObjects();
            throw new BadStrategyException(1);
         }

         Int2ReferenceOpenHashMap var12 = new Int2ReferenceOpenHashMap();
         double var13 = 0.0;
         SettingsMap var15 = null;
         Element var16 = null;
         int var17 = 0;
         ArrayList var18 = this.portfolio.getTradingSetups();

         for (int var19 = 0; var19 < var18.size(); var19++) {
            TradingSetup var20 = (TradingSetup)var18.get(var19);
            String var21 = null;
            String var22 = var20.getName();
            SettingsMap var23 = var20.getSettings();
            if (var19 == 0 && var2 != null) {
               var21 = var2;
            } else {
               if (var19 == 0) {
                  String var24 = var20.getChartSetup().getCharts().get(0).getSymbol();
                  String var25 = var20.getChartSetup().getCharts().get(0).getTimeframe();
                  var21 = "Main: " + var24 + "/" + var25;
               }

               if (var21 == null) {
                  var21 = var20.getName();
               }
            }

            StrategyBase var36 = var20.getStrategy();
            var17 += var36.getAmbiguousTrades();
            int var37 = 0;
            if (var36 != null) {
               var37 = var36.getStrategyProblems();
            }

            var13 += var23.getDouble("MoneyManagement.InitialCapital");
            var23.set("StrategyDismissSettings", var20.getDismissBadStrategies());
            var23.set("StrategyDismissWarnings", var20.getWarningsBadStrategies());
            ChartSetup var26 = (ChartSetup)var23.get("BacktestChart");
            int var27 = var26.getTestPrecision();
            var23.set("BacktestPrecision", var27);
            this.resultsGroup.addSubresult(var21, var23);
            int var28 = var21.hashCode();
            var12.clear();

            for (int var29 = 0; var29 < var8.size(); var29++) {
               Order var30 = var8.get(var29);
               if (var30.SetupName.hashCode() == var28 && !var12.containsKey(var30.Symbol.hashCode())) {
                  var12.put(var30.Symbol.hashCode(), var30.Symbol);
               }

               if (var28 != var22.hashCode() && var30.SetupName.hashCode() == var22.hashCode()) {
                  var30.SetupName = var21;
               }

               if (var11 > 0 && var30.BarsInTrade > var11) {
                  var37 |= BadStrategyException.setOrThrow(this.dismissBadStrategies, this.warningsBadStrategies, 64);
               }
            }

            if (var8.size() == 0) {
               String var38 = var20.getChartSetup().getCharts().get(0).getSymbol();
               if (!var12.containsKey(var38.hashCode())) {
                  var12.put(var38.hashCode(), var38);
               }
            }

            HashMap var39 = var20.getInstrumentsInfoMap();

            for (String var31 : var39.keySet()) {
               InstrumentInfo var32 = (InstrumentInfo)var39.get(var31);
               this.resultsGroup.symbols().add(var31, var32.instrument, var32);
            }

            ObjectIterator var41 = var12.values().iterator();

            while (var41.hasNext()) {
               String var43 = (String)var41.next();
               this.resultsGroup.addResultSymbol(var21, var43);
            }

            if (var15 == null) {
               var15 = var20.getSettings();
            }

            Result var42 = this.resultsGroup.subResult(var21);
            if (var20.getSettings().getBoolean("StoreChartData", false)) {
               var42.addTradingChartsData(var20, var8, this.isAlgowizard);
            }

            var42.setWorstDailyEquity(TraderUtils.getWorstDailyEquity(var20.getStrategy().Trader("History")));
            var16 = var20.getStrategy().getStrategyXml();
            if (var16 != null) {
               var42.addStrategyXml(var16);
            }

            var42.setStrategyProblems(var37);
         }

         var6 = this.resultsGroup.portfolio().getSettings();
         var6.setIfNotExists("MoneyManagement.InitialCapital", var13);
         if (var15 != null) {
            var6.setIfNotExists("OutOfSample", var15.get("OutOfSample"));
         }

         if (var16 != null) {
            this.resultsGroup.portfolio().addStrategyXml(var16);
         }

         ChartSetup var34 = (ChartSetup)var15.get("BacktestChart");
         ChartDef var35 = var34.getMainChart();
         this.resultsGroup.subResult(var2).setString(SpecialValues.Symbol, var34.getSymbol());
         this.resultsGroup.subResult(var2).setString(SpecialValues.Timeframe, var34.getTimeframe());
         this.resultsGroup.specialValues().set(SpecialValues.HistoryFrom, var35.getHistoryFrom());
         this.resultsGroup.specialValues().set(SpecialValues.HistoryTo, var35.getHistoryTo());
         this.resultsGroup.specialValues().set(SpecialValues.Precision, var34.getTestPrecision());
         this.resultsGroup.specialValues().set(SpecialValues.BacktestDuration, SQUtils.round(var3 / 1000.0, 2));
         this.resultsGroup.specialValues().set(SpecialValues.TotalTicks, this.backtestDataFeed.getTotalTicks());
         this.resultsGroup.specialValues().set(SpecialValues.LastModified, System.currentTimeMillis());
         this.resultsGroup.specialValues().set(SpecialValues.AmbiguousTrades, var17);
         this.resultsGroup.specialValues().set(SpecialValues.Complexity, this.computeDegreesOfFreedom(var16));
         this.resultsGroup.orders().addAll(var8);
         var8.clear();
         this.recognizeOOS(var6.get("OutOfSample"));
         this.resultsGroup.setOOSSettings((OutOfSample)var6.get("OutOfSample"));
         this.resultsGroup.computeAllStats();
         this.destroyRemainingObjects();
      }
   }

   private int computeDegreesOfFreedom(Element var1) {
      if (var1 == null) {
         return 0;
      }

      try {
         StrategyBase var2 = StrategyBase.createXmlStrategy(var1.clone().detach());
         var2.transformToVariables(true, ParametersSettings.AllParamTypes);
         Variables var3 = var2.variables();
         int var4 = 0;

         for (Variable var6 : var3) {
            byte var7 = var6.getInternalType();
            String var8 = var6.getName();
            if (!var8.contains("EntrySignal")
               && !var8.contains("ExitSignal")
               && !var8.contains("ReplaceExistingOrders")
               && !var8.startsWith("Magic")
               && var7 != 3
               && var7 != 0
               && var7 != 2) {
               var4++;
            }
         }

         int var10 = this.countConditionsInStrategy(var1);
         return var4 + var10;
      } catch (Exception var9) {
         Log.error("Cannot compute degrees of freedom: ", var9);
         return 0;
      }
   }

   private int countConditionsInStrategy(Element var1) {
      List var2 = var1.getChildren();
      int var3 = 0;
      if (var2 != null && var2.size() > 0) {
         for (int var4 = 0; var4 < var2.size(); var4++) {
            Element var5 = (Element)var2.get(var4);
            if (var5.getName().equals("Item")) {
               String var6 = var5.getAttributeValue("returnType");
               String var7 = var5.getAttributeValue("key");
               if (var6 != null
                  && var6.equals("boolean")
                  && !var7.equals("BooleanVariable")
                  && !var7.equals("Not")
                  && !var7.equals("Boolean")
                  && !var7.equals("MarketPositionIsLong")
                  && !var7.equals("MarketPositionIsShort")) {
                  var3++;
               }
            }

            if (!var5.getName().equals("CustomBlocks")) {
               var3 += this.countConditionsInStrategy(var5);
            }
         }

         return var3;
      } else {
         return 0;
      }
   }

   private void printKeys(String var1, String[] var2) {
      String var3 = "";

      for (String var7 : var2) {
         var3 = var3 + var7 + ",";
      }

      Log.info(var1 + " - All keys: " + var3);
   }

   private void destroyRemainingObjects() {
      if (this.portfolio != null) {
         for (TradingSetup var2 : this.portfolio.getTradingSetups()) {
            var2.getStrategy().destroyHistoryTrades();
         }
      }
   }

   private void recognizeOOS(Object var1) throws Exception {
      OutOfSample var2 = null;
      if (var1 != null) {
         var2 = (OutOfSample)var1;
      }

      for (int var3 = 0; var3 < this.resultsGroup.orders().size(); var3++) {
         Order var4 = this.resultsGroup.orders().get(var3);
         if (var2 == null) {
            var4.SampleType = 11;
         } else {
            var4.SampleType = var2.getSampleType(var4);
         }
      }
   }

   private void initializeBacktest() throws Exception {
      ConnectionManager var1 = new ConnectionManager();
      this.tradingEngine = new TradingEngine(var1);
      this.backtestDataFeed = new BacktestDataFeed(
         this.defaultTradingSimulator.getTestPrecision(), var1, this.defaultTradingSimulator, this.getSimulatorsAsArray()
      );
      this.backtestDataFeed.setDataModifierCallback(this.dataModifierCallback);
      this.backtestDataFeed.setStopPauseEngine(this.stopPauseEngine);
      this.backtestDataFeed.setLoadDataProgressEngine(this.loadDataProgressEngine);
      this.initTradingSetups();
      this.runChecks();
      this.portfolio.initialize();
      this.initializeDataFeedEngine();

      for (TradingSetup var3 : this.portfolio.getTradingSetups()) {
         var3.start(this.singleThreaded);
      }
   }

   private ITradingSimulator[] getSimulatorsAsArray() {
      Collection var1 = this.tradingSimulators.values();
      ITradingSimulator[] var2 = new ITradingSimulator[var1.size()];
      int var3 = 0;

      for (ITradingSimulator var5 : var1) {
         var2[var3++] = var5;
      }

      return var2;
   }

   private void initTradingSetups() throws Exception {
      for (TradingSetup var2 : this.portfolio.getTradingSetups()) {
         BacktestTradingSetup var3 = (BacktestTradingSetup)var2;
         var3.setTradingEngine(this.tradingEngine);
         var3.setUsePreparedData(this.usePreparedData);
         var3.initRunnable(null);
         var3.setProgressListener(this.backtestProgressListener);
         var3.initTradingOptions();
      }
   }

   private void initializeDataFeedEngine() throws Exception {
      long var1 = System.currentTimeMillis();
      this.backtestDataFeed.initialize(this.portfolio, this.defaultTradingSimulator, this.outOfSampleSettings);
      this.stopwatchData.set("LoadDataTime", System.currentTimeMillis() - var1);
   }

   protected void runChecks() throws TradingException {
      if (this.portfolio == null) {
         throw new TradingException("Initialization: PortfolioSetup cannot be null!");
      }

      this.portfolio.runChecks();
   }

   @Override
   public int getEngineId() {
      return this.defaultTradingSimulator.getEngineId();
   }

   public ArrayList<TradingSetup> getTradingSetups() {
      return this.portfolio.getTradingSetups();
   }

   @Override
   public double getGlobalATR() {
      return this.globalATR;
   }

   public MarketData getMarketData() throws Exception {
      try {
         this.stopwatchData = new SettingsMap();
         this.initializeBacktest();
         return this.backtestDataFeed.getMarketData();
      } catch (Exception var5) {
         if (var5 instanceof TaskStoppedException) {
            Log.info("Task stopped");
            throw var5;
         }

         if (!(var5 instanceof StrategyStoppedException)) {
            throw var5;
         }

         Log.info("Strategy stopped manually with reason: " + var5.getMessage());
      } finally {
         this.destroyRemainingObjects();
         this.portfolio.deinitialize();
         if (this.tradingEngine != null) {
            this.tradingEngine.deinitialize();
         }

         backtestsCount.incrementAndGet();
      }

      return null;
   }
}
