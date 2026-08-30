package com.strategyquant.tradinglib.optimization;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestRunner;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.stats.StatsComputer;
import com.strategyquant.tradinglib.util.RngUtils;
import com.strategyquant.tradinglib.wfo.WFOSimulationEngine;
import com.strategyquant.tradinglib.wfo.WFVariant;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WFSimulationJob extends GridJob<WFSimulationJobResult> {
   public static final Logger Log = LoggerFactory.getLogger("WFSimulationJob");
   private ILastEventListener lastEventListener;
   private StopPauseEngine parentStopPauseEngine;
   private WalkForwardPeriod period;
   private String resultName;
   private boolean lastPeriod;
   private IFitnessFunction fitnessFunction;
   private SettingsMap settings;
   private OptimizationSettings optimizationSettings;
   private StatsComputer fitnessStatsComputer;
   private WFOSimulationEngine wfoSimulationEngine;
   private int index;
   private final String mainResultKey;

   public WFSimulationJob(
      String var1,
      Map<String, Serializable> var2,
      StopPauseEngine var3,
      ILastEventListener var4,
      String var5,
      boolean var6,
      IFitnessFunction var7,
      SettingsMap var8,
      OptimizationSettings var9,
      StatsComputer var10,
      WFOSimulationEngine var11,
      WalkForwardPeriod var12,
      int var13,
      String var14
   ) {
      super(var1, 1, var2);
      this.lastEventListener = var4;
      this.parentStopPauseEngine = var3;
      this.period = var12;
      this.resultName = var5;
      this.lastPeriod = var6;
      this.fitnessFunction = var7;
      this.settings = var8;
      this.optimizationSettings = var9;
      this.fitnessStatsComputer = var10;
      this.wfoSimulationEngine = var11;
      this.index = var13;
      this.mainResultKey = var14;
   }

   public WFSimulationJobResult call() throws Exception {
      DurationStats var1 = new DurationStats();
      long var2 = System.currentTimeMillis();

      try {
         long var4 = Long.MAX_VALUE;
         if (this.period.runFrom < var4) {
            var4 = this.period.runFrom;
         }

         if (this.lastEventListener != null) {
            String var6 = L.t("Computing period %d for %s", new Object[]{this.index, this.resultName});
            this.lastEventListener.setLastEvent(var6);
         }

         OrdersList var13 = new OrdersList("performSimulatedWFOptimization " + this.index);
         ResultsGroup var7 = this.runSimpleSimulatedOptimization(this.index, this.period.optimizeFrom, this.period.optimizeTo);
         if (this.index == 0 && var7 != null) {
            var13.addAll(var7.orders());
         }

         if (this.parentStopPauseEngine.isStopped()) {
            if (var7 != null) {
               var7.clear();
            }

            return null;
         } else {
            ResultsGroup var8 = null;
            this.period.optimizationStatData = var7.portfolio().stats((byte)0, (byte)10, (byte)127).getClone();
            if (!this.lastPeriod) {
               boolean var9 = true;
               short[] var10 = (short[])var7.specialValues().get("OptimizationParametersArray");
               if (var9) {
                  if (this.optimizationSettings.optimizationType == 1) {
                     var8 = this.testStrategy(var10, "WF Simulation", "Run strategy " + this.index, this.period.runFrom, this.period.runTo);
                  } else {
                     var8 = this.runSimpleSimulationRun(var7, "Run strategy " + this.index, this.period.runFrom, this.period.runTo);
                  }
               } else {
                  var8 = this.createEmptyCandidate(var7, this.fitnessStatsComputer, "Run strategy " + this.index, this.period.runFrom, this.period.runTo);
               }

               this.period.runStatData = var8.portfolio().stats((byte)0, (byte)10, (byte)127).getClone();
               var13.addAll(var8.orders());
               var8.clear();
            }

            this.period.testParameters = var7.specialValues().getString("OptimizationParameters");
            var7.clear();
            var1.addDuration(DurationStats.MainTest, System.currentTimeMillis() - var2);
            return new WFSimulationJobResult(var13, this.index, var1);
         }
      } catch (Exception var11) {
         if (var11 instanceof BadStrategyException) {
            BadStrategyException var5 = (BadStrategyException)var11;
            if (!var11.getMessage().contains("Automatic filter") && !var11.getMessage().contains(L.t("Automatic filter", new Object[]{true}))) {
               Log.error(String.format("Exception in Cross check - %s : ", this.getJobId(), BadStrategyException.getReasonAsString(var5.getReason())), var11);
               return new WFSimulationJobResult(L.t("Automatic filter", new Object[]{true}), 10004, var1);
            }
         } else {
            Log.error(String.format("Exception in Cross check - %s : ", this.getJobId()), var11);
         }

         return new WFSimulationJobResult(String.format("Exception in backtest: %s", var11.getMessage()), 10004, var1);
      }
   }

   private ResultsGroup createEmptyCandidate(ResultsGroup var1, StatsComputer var2, String var3, long var4, long var6) throws Exception {
      ArrayList var8 = this.wfoSimulationEngine.getVariants();
      String var9 = "";
      int var10 = (Integer)var1.specialValues().get("OptVariantIndex", -1);
      if (var10 < 0) {
         throw new Exception("Best variant index not found!");
      }

      WFVariant var11 = (WFVariant)var8.get(var10);
      ResultsGroup var12 = new ResultsGroup("Variant " + var11.paramsFile);
      var12.addSubresult(var3, this.settings.clone());
      ChartSetups var13 = (ChartSetups)this.settings.get("ChartSetups");

      for (ChartDef var15 : var13.getMainSetup().getCharts()) {
         var12.symbols().add(var15.getSymbol(), var15.getSymbolInfo().instrument, var15.getSymbolInfo());
         var12.addResultSymbol(var3, var15.getSymbol());
      }

      SettingsMap var17 = var12.portfolio().getSettings();
      var17.setIfNotExists("MoneyManagement.InitialCapital", 20000.0);
      Result var18 = var12.subResult(var3);
      SettingsMap var16 = var18.getSettings();
      var16.set("PortfolioDataStart", var4);
      var16.set("PortfolioDataEnd", var6);
      WFOSimulationEngine.computeStatsRequiredForFitness(var2, var3, var18, var12.orders(), var12.symbols());
      var12.specialValues().set("OptimizationParameters", var11.params);
      var12.specialValues().set("OptimizationParametersArray", var11.indexes);
      return var12;
   }

   protected ResultsGroup runSimpleSimulatedOptimization(int var1, long var2, long var4) throws Exception {
      return this.shouldOptimizeByIndex()
         ? this.runSimpleSimulatedOptimizationIndexFitness(var1, var2, var4)
         : this.runSimpleSimulatedOptimizationNormalFitness(var1, var2, var4);
   }

   private boolean shouldOptimizeByIndex() throws Exception {
      return (Boolean)this.settings.get("UseFitnessByIndex", false);
   }

   protected ResultsGroup runSimpleSimulatedOptimizationIndexFitness(int var1, long var2, long var4) throws Exception {
      ArrayList var7 = this.wfoSimulationEngine.getVariants();
      ArrayList var8 = new ArrayList();
      if (var1 == 1 && Log.isDebugEnabled()) {
         String var9 = SQTime.toDateMinuteString(var2);
         String var10 = SQTime.toDateMinuteString(var4);
         Log.debug("Optimize from {} to {}", var9, var10);
      }

      for (int var14 = 0; var14 < var7.size(); var14++) {
         WFVariant var16 = (WFVariant)var7.get(var14);
         if (this.parentStopPauseEngine.isStopped()) {
            this.clearCandidates(var8);
            return null;
         }

         ResultsGroup var6 = this.wfoSimulationEngine
            .simulateTestCandidate(this.fitnessStatsComputer, var16, var2, var4, this.cloneBacktestSettings(var2, var4));
         if (var6 != null) {
            var6.specialValues().set("OptVariantIndex", var14);
            var8.add(var6);
         }
      }

      if (var8.size() == 0) {
         throw new Exception(L.t("Optimization not successful, please check log for more information!", new Object[]{true}));
      }

      boolean var15 = false;
      ResultsGroup var17 = this.findBestCandidate(var8, this.fitnessFunction, var2);
      if (var17 == null) {
         throw new Exception(L.t("Optimization not successful (reason 2), please check log for more information!", new Object[]{true}));
      }

      int var11 = var17.specialValues().getInt("OptVariantIndex");
      String var12 = ((WFVariant)var7.get(var11)).params;
      Log.debug("Best candiate params: {}", var12);
      if (var17 == null) {
         throw new Exception(L.t("Optimization not successful, please check log for more information!", new Object[]{true}));
      }

      SettingsMap var13 = var17.subResult("Setup 1").getSettings();
      var13.set("PortfolioDataStart", var2);
      var13.set("PortfolioDataEnd", var4);
      var17.computeAllStats();
      return var17;
   }

   private ResultsGroup findBestCandidate(ArrayList<ResultsGroup> var1, IFitnessFunction var2, long var3) throws Exception {
      for (int var5 = 0; var5 < var1.size(); var5++) {
         ResultsGroup var6 = (ResultsGroup)var1.get(var5);
         var6.specialValues().set("OptIndexFitness", 0.0);
      }

      ArrayList var21 = var2.getMetricsForFitness();
      ArrayList var22 = new ArrayList(var1);

      for (int var7 = 0; var7 < var21.size(); var7++) {
         MetricForFitness var8 = (MetricForFitness)var21.get(var7);
         RngUtils.setIndexByMetric(var22, var8, var2);
      }

      ResultsGroup var24 = null;
      double var11 = Double.MAX_VALUE;
      double var13 = 0.0;
      String var15 = null;
      StringBuffer var16 = null;
      ArrayList var17 = null;
      if (this.optimizationSettings.debugWFFitnessPath != null) {
         var16 = new StringBuffer();
         var17 = RngUtils.printStrategyResultsHeader(var16, true, var2);
      }

      for (int var18 = 0; var18 < var1.size(); var18++) {
         ResultsGroup var23 = (ResultsGroup)var1.get(var18);
         if (this.parentStopPauseEngine.isStopped()) {
            if (var24 != null) {
               var24.clear();
            }

            return null;
         }

         double var9 = var23.specialValues().getDouble("OptIndexFitness");
         if (this.optimizationSettings.debugWFFitnessPath != null) {
            RngUtils.debugWFLogCandidate(var16, this.resultName, var23, var17);
         }

         String var19 = var23.specialValues().getString("OptimizationParameters", null);
         boolean var20 = this.isBetterCandidateFitnessIndex(var23, var9, var11, var13, var15, var19);
         if (var20) {
            if (var24 != null) {
               var24.clear();
            }

            var11 = var9;
            var13 = var23.portfolio().stats((byte)0, (byte)10, (byte)10).getInt("NumberOfTrades");
            var15 = var19;
            var24 = var23;
            var24.specialValues().set("OptVariantIndex", var18);
         } else {
            var23.clear();
         }
      }

      if (this.optimizationSettings.debugWFFitnessPath != null) {
         RngUtils.debugWFLogWriteFile(this.optimizationSettings, this.mainResultKey, this.resultName, var16, this.period);
      }

      return var24;
   }

   private boolean isBetterCandidateFitnessIndex(ResultsGroup var1, double var2, double var4, double var6, String var8, String var9) throws Exception {
      return var4 == Double.MAX_VALUE ? true : var2 < var4;
   }

   private boolean isVariantSmaller(String var1, String var2) {
      if (var2 == null) {
         return true;
      }

      String[] var3 = var1.split(",");
      String[] var4 = var2.split(",");
      if (var3.length != 0 && var3.length == var4.length) {
         for (int var5 = 0; var5 < var3.length; var5++) {
            String[] var6 = var3[var5].split("=");
            String[] var7 = var4[var5].split("=");
            if (var6.length == 0 || var6.length != var7.length) {
               return true;
            }

            if (!var6[0].equals(var7[0])) {
               return true;
            }

            double var8 = Double.parseDouble(var6[1]);
            double var10 = Double.parseDouble(var7[1]);
            if (var8 < var10) {
               return true;
            }

            if (var8 > var10) {
               return false;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private void clearCandidates(ArrayList<ResultsGroup> var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         ResultsGroup var3 = (ResultsGroup)var1.get(var2);
         if (var3 != null) {
            var3.clear();
         }
      }
   }

   protected ResultsGroup runSimpleSimulatedOptimizationNormalFitness(int var1, long var2, long var4) throws Exception {
      ResultsGroup var7 = null;
      double var10 = -1.0;
      double var12 = 0.0;
      WFVariant var14 = null;
      ArrayList var15 = this.wfoSimulationEngine.getVariants();
      String var16 = "";
      if (var1 == 1 && Log.isDebugEnabled()) {
         String var17 = SQTime.toDateMinuteString(var2);
         String var18 = SQTime.toDateMinuteString(var4);
         Log.debug("Optimize from {} to {}", var17, var18);
      }

      for (int var21 = 0; var21 < var15.size(); var21++) {
         WFVariant var23 = (WFVariant)var15.get(var21);
         if (this.parentStopPauseEngine.isStopped()) {
            if (var7 != null) {
               var7.clear();
            }

            return null;
         }

         ResultsGroup var6 = this.wfoSimulationEngine
            .simulateTestCandidate(this.fitnessStatsComputer, var23, var2, var4, this.cloneBacktestSettings(var2, var4));
         if (var6 != null) {
            double var8 = this.computeFitness(var6);
            boolean var19 = this.isBetterCandidate(var6, var8, var10, var12, var14, var23);
            if (var19) {
               if (var7 != null) {
                  var7.clear();
               }

               var10 = var8;
               var12 = var6.portfolio().stats((byte)0, (byte)10, (byte)10).getInt("NumberOfTrades");
               var14 = var23;
               var7 = var6;
               var16 = ((WFVariant)var15.get(var21)).params;
               var7.specialValues().set("OptVariantIndex", var21);
            } else {
               var6.clear();
            }
         }
      }

      if (var7 == null) {
         throw new Exception(L.t("Optimization not successful, please check log for more information!", new Object[]{true}));
      }

      SettingsMap var22 = var7.subResult("Setup 1").getSettings();
      var22.set("PortfolioDataStart", var2);
      var22.set("PortfolioDataEnd", var4);
      var7.computeAllStats();
      return var7;
   }

   private boolean isBetterCandidate(ResultsGroup var1, double var2, double var4, double var6, WFVariant var8, WFVariant var9) throws Exception {
      return var4 == -1.0 ? true : var2 > var4;
   }

   private boolean isVariantSmaller(WFVariant var1, WFVariant var2) {
      if (var2 == null) {
         return true;
      }

      String[] var3 = var1.params.split(",");
      String[] var4 = var2.params.split(",");
      if (var3.length != 0 && var3.length == var4.length) {
         for (int var5 = 0; var5 < var3.length; var5++) {
            String[] var6 = var3[var5].split("=");
            String[] var7 = var4[var5].split("=");
            if (var6.length == 0 || var6.length != var7.length) {
               return true;
            }

            if (!var6[0].equals(var7[0])) {
               return true;
            }

            double var8 = Double.parseDouble(var6[1]);
            double var10 = Double.parseDouble(var7[1]);
            if (var8 < var10) {
               return true;
            }

            if (var8 > var10) {
               return false;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   protected double computeFitness(ResultsGroup var1) throws Exception {
      return this.computeFitness(var1, (byte)0, (byte)10);
   }

   private double computeFitness(ResultsGroup var1, byte var2, byte var3) throws Exception {
      double var4;
      if (this.wfoSimulationEngine.isPeriodTypeBars()) {
         var4 = this.fitnessFunction.computeFitness(var1, var2, var3, false);
      } else {
         var4 = this.fitnessFunction.computeFitness(var1, var2, var3);
      }

      var1.portfolio().setFitness(var3, var4);
      if (var2 == 0 && var3 == 127) {
         Log.debug("Fitness computed to: " + var4);
      }

      return var4;
   }

   public ResultsGroup testStrategy(short[] var1, String var2, String var3, long var4, long var6) throws Exception {
      long var8 = System.currentTimeMillis();
      SettingsMap var10 = this.cloneBacktestSettings(var4, var6);
      var10.set("StrategyName", var2);
      var10.set("MainResultKey", var3);
      var10.set("ResultGroupName", var2);
      StrategyBase var11 = (StrategyBase)this.settings.get("StrategyObject");
      StrategyParamData var12 = StrategyBase.createStrategyVariation(var11, this.optimizationSettings, this.optimizationSettings.transformToFullIndexes(var1));
      StrategyBase var13 = var12.getStrategy();
      String var14 = var12.getParams();
      Element var15 = var13.getStrategyXml();
      var10.set("StrategyObject", StrategyBase.createXmlStrategy(var15));
      BacktestRunner var16 = new BacktestRunner(new BacktestSettings(var10), this.parentStopPauseEngine, null, false, this.lastEventListener, null);
      BacktestResult var17 = var16.execute();
      ResultsGroup var18 = var17.getResult();
      if (var18 == null) {
         throw new Exception(String.format("No results for backtest! Reason: %s", var17.getDismissalMessage()));
      }

      var18.specialValues().setString("OptimizationParameters", var14);
      var18.specialValues().set("OptimizationParametersArray", var1);
      double var19 = this.computeFitness(var18);
      var18.portfolio().setFitness((byte)127, var19);
      return var18;
   }

   protected SettingsMap cloneBacktestSettings(long var1, long var3) throws Exception {
      SettingsMap var5 = new SettingsMap();
      var5.set("ChartSetups", ((ChartSetups)this.settings.get("ChartSetups")).getClone(var1, var3));
      var5.set("MoneyManagement.InitialCapital", this.settings.getDouble("MoneyManagement.InitialCapital"));
      var5.set("TestPrecision", this.settings.getInt("TestPrecision"));
      var5.set("Slippage", this.settings.getDouble("Slippage"));
      var5.set("MinDistance", this.settings.getDouble("MinDistance"));
      var5.set("Commission", (CommissionsMethod)this.settings.get("Commission"));
      var5.set("Swap", (SwapMethod)this.settings.get("Swap"));
      var5.set("FitnessFunction", this.settings.get("FitnessFunction"));
      var5.set("MoneyManagement.Method", this.settings.get("MoneyManagement.Method"));
      var5.set("RiskManagement", this.settings.get("RiskManagement"));
      TradingOptions var6 = ((TradingOptions)this.settings.get("TradingOptions")).getClone();
      var5.set("TradingOptions", var6);
      ATM var7 = (ATM)this.settings.get("ATM");
      if (var7 != null) {
         var5.set("ATM", var7.getClone());
      }

      return var5;
   }

   private ResultsGroup runSimpleSimulationRun(ResultsGroup var1, String var2, long var3, long var5) throws Exception {
      double var10 = -1.0;
      ArrayList var12 = this.wfoSimulationEngine.getVariants();
      String var13 = "";
      int var14 = (Integer)var1.specialValues().get("OptVariantIndex", -1);
      if (var14 < 0) {
         throw new Exception("Best variant index not found!");
      }

      ResultsGroup var7 = this.wfoSimulationEngine
         .simulateTestCandidate(this.fitnessStatsComputer, (WFVariant)var12.get(var14), var3, var5, this.cloneBacktestSettings(var3, var5));
      if (var7 == null) {
         throw new Exception("Candidate is null!");
      }

      double var8 = this.computeFitness(var7);
      SettingsMap var15 = var7.subResult("Setup 1").getSettings();
      var15.set("PortfolioDataStart", var3);
      var15.set("PortfolioDataEnd", var5);
      var7.computeAllStats();
      return var7;
   }
}
