package com.strategyquant.tradinglib.optimization;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.time.SQTimeOld;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.options.parameters.ReservedBars;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.simplegrid.SimpleGridEngine;
import com.strategyquant.tradinglib.strategy.MarketData;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import com.strategyquant.tradinglib.wfo.WFVariant;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationEngineWF extends OptimizationEngineSimple {
   public static final Logger Log = LoggerFactory.getLogger("OptimizationEngineWF");
   private Element conditionsElem;
   private ArrayList<Condition> conditions;
   private int thresholdPct;
   private int robCombRows;
   private int robCombCols;
   private int robMinComb;
   protected int runNumber = 0;
   private boolean wfBatchCreated;

   public OptimizationEngineWF(String var1, StopPauseEngine var2, GridJob var3, String var4, ILastEventListener var5, String var6) {
      super(var1, var2, var3, var4, var5, var6);
   }

   @Override
   public void initialize(SettingsMap var1) throws Exception {
      super.initialize(var1);
      if (this.optimizationSettings.type != 1) {
         this.conditionsElem = this.optimizationSettings.elConditions;
         this.conditions = this.optimizationSettings.conditions;
         this.thresholdPct = this.optimizationSettings.thresholdPct;
         this.robCombRows = this.optimizationSettings.robCombRows;
         this.robCombCols = this.optimizationSettings.robCombCols;
         this.robMinComb = this.optimizationSettings.robMinComb;
      }
   }

   public int getTotalStepsCount() {
      if (this.optimizationSettings.type == 1) {
         return this.getTotalStepsForSimpleOptimization();
      }

      int var1 = 0;
      if (this.optimizationSettings.optimizationType == 1 || this.optimizationSettings.optimizationType == 0) {
         var1 = this.getTotalStepsForSimpleOptimization();
      }

      this.initOptimization();
      int var2 = var1;
      if (this.optimizationSettings.type == 2) {
         var2 += this.getTotalStepsForWF(var1, this.optimizationSettings.param1Start, this.optimizationSettings.param2Start);
      } else if (this.optimizationSettings.type == 3) {
         var2 += this.getTotalStepsForMultiWF(var1) - 1;
      }

      return var2;
   }

   private int getTotalStepsForMultiWF(int var1) {
      int var2 = 1;

      for (int var3 = this.optimizationSettings.param1Start; var3 <= this.optimizationSettings.param1Stop; var3 += this.optimizationSettings.param1Step) {
         for (int var4 = this.optimizationSettings.param2Start; var4 <= this.optimizationSettings.param2Stop; var4 += this.optimizationSettings.param2Step) {
            var2 += this.getTotalStepsForWF(var1, var3, var4) - 1;
         }
      }

      return var2;
   }

   private int getTotalStepsForWF(int var1, int var2, int var3) {
      byte var4 = 1;
      ArrayList var5 = this.computeWFPeriods(var2, var3, 0, null);
      return var4 + var5.size() * 2;
   }

   @Override
   protected void initOptimization() {
      ChartSetups var1 = (ChartSetups)this.settings.get("ChartSetups");
      this.totalDays = SQTimeOld.getDaysBetween(var1.getMainSetup().getMainChart().getHistoryFrom(), var1.getMainSetup().getMainChart().getHistoryTo());
   }

   protected WalkForwardResult optimizeWF(ResultsGroup var1, int var2, int var3, int var4, int var5, int var6, MarketData var7, boolean var8) throws Exception {
      double var9 = this.settings.getDouble("MoneyManagement.InitialCapital");
      if (this.optimizationSettings.periodType == 30 && var7 == null) {
         var6 = this.getReservedBars();
         if (var6 > 0) {
            var7 = this.getMarketData();
         }
      }

      ArrayList var11 = this.computeWFPeriods(var4, var5, var6, var7);
      String var12 = this.settings.getString("StrategyName");
      if (var1 == null) {
         short[] var13 = this.prepareIndexesArray(this.parameters.getUsedCount());
         String var14 = "WF Optimization - " + var12;
         var1 = this.testStrategy(var13, var14, var12, 0L, 0L);
         this.increaseStep();
      }

      if (this.progressEngine.isStopped()) {
         return null;
      }

      String var20 = OptimizationConst.printWFByPeriodType(var2, var5, var4);
      String var22 = "Computing " + var20;
      this.printLog(var22);
      if (this.lastEventListener != null) {
         this.lastEventListener.setLastEvent(var22);
      }

      var20 = this.performSimulatedWFOptimization(var1, var20, var3, var11, var12);
      String var15 = var1.subResult(var1.getMainResultKey()).getString(SpecialValues.Symbol);
      String var16 = var1.subResult(var1.getMainResultKey()).getString(SpecialValues.Timeframe);
      ResultsGroup var17;
      if (var8) {
         Result var18 = var1.subResult(var20);
         var18.setSettings(this.settings.clone());
         String var19 = this.getStrName(var1);
         var17 = new ResultsGroup(var19 + " - WFM " + var4 + "x" + var5);
         var17.addSubresult(var20, var18.getSettings(), var18);
         var17.orders().addAll(var1.orders().filterWithClone(var20, (byte)0, (byte)127));
      } else {
         var17 = var1;
      }

      if (this.progressEngine.isStopped()) {
         return null;
      }

      WalkForwardResult var23 = new WalkForwardResult();
      var23.wfPeriods = var11;
      var23.resultName = var20;
      var23.stats = var1.subResult(var20).stats((byte)0, (byte)10, (byte)127);
      var23.statsOOS = var1.subResult(var20).stats((byte)0, (byte)10, (byte)20);
      var23.testParams = var1.specialValues().getString("OptimizationParameters");
      if (var3 == 2) {
         WalkForwardMatrixResult var24 = new WalkForwardMatrixResult();
         var24.start1 = var4;
         var24.start2 = var5;
         var24.activeParam1 = var4;
         var24.activeParam2 = var5;
         var24.periodType = var2;
         var24.addWFResult(var4, var5, var23);
         var24.computeRobustnessResults(var1, this.conditionsElem, this.conditions, this.thresholdPct, this.robCombRows, this.robCombCols, this.robMinComb);
         if (var24.passed) {
            var17.specialValues().set(SpecialValues.FiltersResultFailedReason, SpecialValues.FiltersResultPassed);
         } else {
            var17.specialValues().set(SpecialValues.FiltersResultFailedReason, var24.dismissalMessage);
         }

         var17.mainResult().set("WalkForwardResult", var24);
         var17.specialValues().set("WalkForwardConditions", this.conditionsElem);
         if (var8) {
            var17.mainResult().set("WalkForwardResult", var24);
            var17.specialValues().set("WalkForwardConditions", this.conditionsElem);
            var17.specialValues().set(SpecialValues.Symbol, var15);
            var17.specialValues().set(SpecialValues.Timeframe, var16);
            var17.portfolio().addStrategyXml(var1.getStrategyXml());
         }

         var17.symbols().add(var1.symbols());
         this.addResultToDatabank(var17);
         this.optimizationProfile.reset();
         var17.setOptimizationProfile(this.optimizationProfile);
      }

      this.settings.set("MoneyManagement.InitialCapital", var9);
      if (var8) {
         var17.setLastSettings(var1.getLastSettings());
      }

      return var23;
   }

   private String getStrName(ResultsGroup var1) {
      String var2 = var1.getName();
      int var3 = var2.indexOf("- RA");
      if (var3 < 0) {
         return var2;
      }

      int var4 = var2.indexOf(" -", var3 + 2);
      return var4 < 0 ? var2 : var2.substring(var3 + 2, var4);
   }

   private long printTime(long var1, String var3) {
      long var4 = System.currentTimeMillis();
      System.out.println(var3 + " : " + (var4 - var1));
      return var4;
   }

   private int getReservedBars() {
      for (TradingOption var3 : (TradingOptions)this.settings.get("TradingOptions")) {
         if (var3 instanceof ReservedBars) {
            ReservedBars var4 = (ReservedBars)var3;
            return var4.ReservedBars;
         }
      }

      return 0;
   }

   private void processWFOptJobResult(WFOptJobResult var1, JobDetails var2) {
   }

   protected void optimizeMultiWF() throws Exception {
      if (!this.progressEngine.isStopped()) {
         short[] var1 = this.prepareIndexesArray(this.parameters.getUsedCount());
         String var2 = this.settings.getString("StrategyName");
         String var3 = "WF Matrix - " + var2;
         ResultsGroup var4 = this.testStrategy(var1, var3, var2, 0L, 0L);
         if (this.progressEngine.isStopped()) {
            if (var4 != null) {
               var4.clear();
            }
         } else {
            this.printLog("Computing Walk-Forward Matrix");
            WalkForwardMatrixResult var5 = new WalkForwardMatrixResult();
            var5.start1 = this.optimizationSettings.param1Start;
            var5.stop1 = this.optimizationSettings.param1Stop;
            var5.increment1 = this.optimizationSettings.param1Step;
            var5.start2 = this.optimizationSettings.param2Start;
            var5.stop2 = this.optimizationSettings.param2Stop;
            var5.increment2 = this.optimizationSettings.param2Step;
            var5.periodType = this.optimizationSettings.periodType;
            int var6 = 0;
            MarketData var7 = null;
            if (this.optimizationSettings.periodType == 30) {
               var6 = this.getReservedBars();
               if (var6 > 0) {
                  var7 = this.getMarketData();
               }
            }

            for (int var8 = var5.start1; var8 <= var5.stop1; var8 += var5.increment1) {
               for (int var9 = var5.start2; var9 <= var5.stop2; var9 += var5.increment2) {
                  WalkForwardResult var10 = this.optimizeWF(var4, this.optimizationSettings.periodType, 3, var8, var9, var6, var7, false);
                  if (this.progressEngine.isStopped()) {
                     if (var4 != null) {
                        var4.clear();
                     }

                     return;
                  }

                  if (var10 != null) {
                     var5.addWFResult(var8, var9, var10);
                  }
               }
            }

            var5.computeRobustnessResults(var4, this.conditionsElem, this.conditions, this.thresholdPct, this.robCombRows, this.robCombCols, this.robMinComb);
            var4.mainResult().set("WalkForwardResult", var5);
            if (var5.passed) {
               var4.specialValues().set(SpecialValues.FiltersResultFailedReason, SpecialValues.FiltersResultPassed);
            } else {
               var4.specialValues().set(SpecialValues.FiltersResultFailedReason, var5.dismissalMessage);
            }

            var4.specialValues().set("WalkForwardConditions", this.conditionsElem);
            this.optimizationProfile.reset();
            var4.setOptimizationProfile(this.optimizationProfile);
            this.addResultToDatabank(var4);
         }
      }
   }

   private String performSimulatedWFOptimization(ResultsGroup var1, final String var2, int var3, final ArrayList<WalkForwardPeriod> var4, final String var5) throws Exception {
      this.runNumber++;
      long var6 = Long.MAX_VALUE;
      long var8 = Long.MIN_VALUE;
      if (this.optimizationSettings.optimizationType == 1) {
      }

      final ArrayList var10 = new ArrayList();

      for (int var11 = 0; var11 < var4.size(); var11++) {
         var10.add(null);
      }

      this.wfBatchCreated = false;
      SimpleGridEngine var24 = new SimpleGridEngine<WFSimulationJob, WFSimulationJobResult>(
         this.getStopPauseEngine(), null, this.optimizationSettings.singleThreaded
      ) {
         @Override
         protected ArrayList<WFSimulationJob> createJobsBatch(int var1, GridClient var2x) throws Exception {
            return OptimizationEngineWF.this.createWFSimBatch(var2x, OptimizationEngineWF.this.getStopPauseEngine(), var2, var4, var5);
         }

         protected void processResult(WFSimulationJobResult var1, JobDetails var2x) throws Exception {
            OptimizationEngineWF.this.processWFSimJobResult(var1, var2x, var10);
         }

         @Override
         protected void onError(String var1, Exception var2x) {
            Log.error("MC Error {}, Exception ", var1, var2x);
         }
      };
      var24.start();
      var24.unregisterListeners();
      var1.addSubresult(var2, this.settings);
      long var12 = Long.MAX_VALUE;
      long var14 = Long.MIN_VALUE;
      OutOfSample var16 = new OutOfSample();

      for (int var17 = 0; var17 < var4.size(); var17++) {
         WalkForwardPeriod var18 = (WalkForwardPeriod)var4.get(var17);
         if (var18.runFrom < var6) {
            var6 = var18.runFrom;
         }

         if (var18.runTo > var8) {
            var8 = var18.runTo;
         }

         if (var18.runFrom < var12) {
            var12 = var18.runFrom;
         }

         if (var18.optimizeTo > var14) {
            var14 = var18.optimizeTo;
         }

         OrdersList var19 = (OrdersList)var10.get(var17);
         if (var19 != null) {
            for (int var22 = 0; var22 < var19.size(); var22++) {
               Order var23 = var19.get(var22);
               var23.IsInPortfolio = 0;
               var23.SetupName = var2;
               var23.Ticket = var22 + 1;
               long var20;
               if (this.optimizationSettings.periodType == 30) {
                  var20 = var23.CloseTime;
               } else {
                  var20 = var23.OpenTime;
               }

               if (var20 >= var6) {
                  var23.SampleType = 21;
               }
            }

            var1.orders().addAll(var19);
            var19.clear();
         }
      }

      var16.addRange(var6, var8, (byte)20);
      SettingsMap var25 = var1.subResult(var2).getSettings();
      var25.set("PortfolioDataStart", var12);
      var25.set("PortfolioDataEnd", var14);
      OutOfSample var26 = (OutOfSample)var25.get("OutOfSample");
      var25.set("OutOfSample", var16);
      var1.subResult(var2).computeAllStats(var1.specialValues(), null);
      var25.set("OutOfSample", var26);
      Result var27 = var1.subResult(var2);
      ResultsGroup var28 = new ResultsGroup("wfRG");
      var28.addSubresult(var2, var27.getSettings(), var27);
      double var21 = this.fitnessFunction.computeFitness(var28, (byte)0, (byte)10);
      var27.setFitness((byte)10, var21);
      var21 = this.fitnessFunction.computeFitness(var28, (byte)0, (byte)127);
      var27.setFitness((byte)127, var21);
      var21 = this.fitnessFunction.computeFitness(var28, (byte)0, (byte)20);
      var27.setFitness((byte)20, var21);
      var28.portfolio().setFitness((byte)127, var21);
      return var2;
   }

   protected ArrayList<WFSimulationJob> createWFSimBatch(GridClient var1, StopPauseEngine var2, String var3, ArrayList<WalkForwardPeriod> var4, String var5) {
      if (this.wfBatchCreated) {
         return null;
      }

      ArrayList var6 = new ArrayList();

      for (int var7 = 0; var7 < var4.size(); var7++) {
         WalkForwardPeriod var8 = (WalkForwardPeriod)var4.get(var7);
         String var9 = String.format("%s WFO %d", var3, var7);
         boolean var10 = var7 == var4.size() - 1;
         WFSimulationJob var11 = new WFSimulationJob(
            var9,
            this.getWFSimJobSettings(),
            this.getStopPauseEngine(),
            this.lastEventListener,
            var3,
            var10,
            this.fitnessFunction,
            this.settings,
            this.optimizationSettings,
            this.fitnessStatsComputer.getClone(),
            this.wfoSimulationEngine,
            var8,
            var7,
            var5
         );
         var6.add(var11);
      }

      this.wfBatchCreated = true;
      return var6.size() == 0 ? null : var6;
   }

   private Map<String, Serializable> getWFSimJobSettings() {
      return new HashMap<>();
   }

   protected void processWFSimJobResult(WFSimulationJobResult var1, JobDetails var2, ArrayList<OrdersList> var3) {
      String var4 = var2.getJobID();
      this.increaseStep(2);
      if (var2.isSuccess()) {
         OrdersList var5 = var1.getOrders();
         int var6 = var1.getIndex();
         if (var5 != null && var6 >= 0 && var6 < var3.size()) {
            var3.set(var6, var5);
         } else {
            Log.debug("WF Sim job returned empty orders");
         }
      } else {
         var2.getException();
         Log.error(String.format("Optimization %s failed, error: %s", var4, var2.getException()));
      }
   }

   private String performSimulatedWFOptimizationOld(ResultsGroup var1, String var2, int var3, ArrayList<WalkForwardPeriod> var4) throws Exception {
      this.runNumber++;
      ResultsGroup var6 = null;
      OrdersList var7 = new OrdersList("performSimulatedWFOptimization");
      long var8 = Long.MAX_VALUE;
      if (this.optimizationSettings.optimizationType == 1) {
      }

      long var10 = System.currentTimeMillis();

      for (int var14 = 0; var14 < var4.size(); var14++) {
         WalkForwardPeriod var15 = (WalkForwardPeriod)var4.get(var14);
         if (var15.runFrom < var8) {
            var8 = var15.runFrom;
         }

         if (this.lastEventListener != null) {
            String var16 = L.t("Computing period %d for %s", new Object[]{var14, var2});
            this.lastEventListener.setLastEvent(var16);
         }

         ResultsGroup var5 = this.runSimpleSimulatedOptimization(var14, var15.optimizeFrom, var15.optimizeTo);
         if (var14 == 0 && var5 != null) {
            var7.addAll(var5.orders());
         }

         if (this.progressEngine.isStopped()) {
            if (var5 != null) {
               var5.clear();
            }

            return null;
         }

         this.increaseStep();
         var15.optimizationStatData = var5.portfolio().stats((byte)0, (byte)10, (byte)127).getClone();
         if (var14 == 0) {
         }

         if (var14 < var4.size() - 1) {
            short[] var23 = (short[])var5.specialValues().get("OptimizationParametersArray");
            if (this.optimizationSettings.optimizationType == 1) {
               var6 = this.testStrategy(var23, "WF Simulation", "Run strategy " + var14, var15.runFrom, var15.runTo);
            } else {
               var6 = this.runSimpleSimulationRun(var5, "Run strategy " + var14, var15.runFrom, var15.runTo);
            }

            var15.runStatData = var6.portfolio().stats((byte)0, (byte)10, (byte)127).getClone();
            if (var14 != 0) {
            }

            var7.addAll(var6.orders());
            var6.clear();
         }

         var15.testParameters = var5.specialValues().getString("OptimizationParameters");
         var5.clear();
         this.increaseStep();
      }

      var1.addSubresult(var2, this.settings);

      for (int var19 = 0; var19 < var7.size(); var19++) {
         Order var21 = var7.get(var19);
         var21.SetupName = var2;
         var21.Ticket = var19 + 1;
         if (var21.OpenTime >= var8) {
            var21.SampleType = 21;
         }
      }

      var1.orders().addAll(var7);
      var7.clear();
      var1.subResult(var2).computeAllStats(var1.specialValues(), null);
      Result var20 = var1.subResult(var2);
      ResultsGroup var22 = new ResultsGroup("wfRG");
      var22.addSubresult(var2, var20.getSettings(), var20);
      double var24 = this.fitnessFunction.computeFitness(var22, (byte)0, (byte)10);
      var20.setFitness((byte)10, var24);
      var24 = this.fitnessFunction.computeFitness(var22, (byte)0, (byte)127);
      var20.setFitness((byte)127, var24);
      var24 = this.fitnessFunction.computeFitness(var22, (byte)0, (byte)20);
      var20.setFitness((byte)20, var24);
      var22.portfolio().setFitness((byte)127, var24);
      return var2;
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
      var7.computeAllStats();
      return var7;
   }

   protected ResultsGroup runSimpleSimulatedOptimization(int var1, long var2, long var4) throws Exception {
      ResultsGroup var7 = null;
      double var10 = -1.0;
      ArrayList var12 = this.wfoSimulationEngine.getVariants();
      String var13 = "";

      for (int var14 = 0; var14 < var12.size(); var14++) {
         if (this.progressEngine.isStopped()) {
            if (var7 != null) {
               var7.clear();
            }

            return null;
         }

         ResultsGroup var6 = this.wfoSimulationEngine
            .simulateTestCandidate(this.fitnessStatsComputer, (WFVariant)var12.get(var14), var2, var4, this.cloneBacktestSettings(var2, var4));
         if (var6 != null) {
            double var8 = this.computeFitness(var6);
            if (var10 != -1.0 && !(var10 < var8)) {
               var6.clear();
            } else {
               if (var7 != null) {
                  var7.clear();
               }

               var10 = var8;
               var7 = var6;
               var13 = ((WFVariant)var12.get(var14)).params;
               var7.specialValues().set("OptVariantIndex", var14);
            }
         }
      }

      Log.debug("Best candiate params: {}", var13);
      if (var7 == null) {
         throw new Exception(L.t("Optimization not successful, please check log for more information!", new Object[0]));
      }

      var7.computeAllStats();
      return var7;
   }

   protected ArrayList<WalkForwardPeriod> computeWFPeriods(int var1, int var2, int var3, MarketData var4) {
      int var5;
      int var6;
      if (this.optimizationSettings.periodType != 20 && this.optimizationSettings.periodType != 30) {
         double var7 = var2;
         double var9 = var1 / 100.0;
         var5 = (int)(this.totalDays / (1.0 + var7 * var9));
         var6 = (int)(var5 * var9);
         long var11 = this.totalDays - (var5 + (int)var7 * var6) - ((int)var7 + 2);
         var5 = (int)(var5 + var11);
      } else {
         var5 = var1;
         var6 = var2;
      }

      ArrayList var14 = this.computeWalkForwardPeriods(var5, var6, var3, var4);

      for (int var8 = 0; var8 < var14.size(); var8++) {
         WalkForwardPeriod var15 = (WalkForwardPeriod)var14.get(var8);
         this.progressEngine.checkPaused();
      }

      return var14;
   }

   private ArrayList<WalkForwardPeriod> computeWalkForwardPeriods(int var1, int var2, int var3, MarketData var4) {
      ChartSetups var5 = (ChartSetups)this.settings.get("ChartSetups");
      ChartSetup var6 = var5.getMainSetup();
      int var8 = -1;
      int var9 = -1;
      SQTimeOld var7;
      if (var3 > 0 && var4 != null) {
         long var10 = var4.getDateBarsFromDate(0L, var3);
         SQTimeOld var12 = new SQTimeOld(var10);
         var7 = new SQTimeOld(SQTime.correctDayStart(var12.getMilis()));
      } else {
         var7 = new SQTimeOld(var6.getMainChart().getHistoryFrom());
      }

      ArrayList var24 = new ArrayList();
      long var11 = -1L;
      boolean var13 = false;
      if (var3 > 0 && var4 != null) {
         var2--;
      }

      int var17 = 0;
      WalkForwardPeriod var18 = null;

      do {
         var17++;
         WalkForwardPeriod var19 = new WalkForwardPeriod();
         if (var3 > 0 && var4 != null) {
            if (var18 == null) {
               var19.optimizeFrom = var7.getMilis();
               long var15 = var4.getDateBarsFromDate(var19.optimizeFrom, var1);
               var19.optimizeTo = SQTime.correctDayEnd(var15);
               var19.runFrom = SQTime.correctDayStart(this.addDaysReturnDate(var19.optimizeTo, 1).getMilis());
               var15 = var4.getDateBarsFromDate(var19.runFrom, var2);
               var19.runTo = SQTime.correctDayEnd(var15);
            } else {
               var19.runFrom = SQTime.correctDayStart(this.addDaysReturnDate(var18.runTo, 1).getMilis());
               long var27 = var4.getDateBarsFromDate(var19.runFrom, var2);
               var19.runTo = SQTime.correctDayEnd(var27);
               var19.optimizeTo = SQTime.correctDayEnd(this.addDaysReturnDate(var19.runFrom, -1).getMilis());
               var27 = var4.getDateBarsFromDate(var19.optimizeTo, -(var1 + 0));
               var19.optimizeFrom = SQTime.correctDayStart(var27);
            }

            var9 = SQTime.getDiffInDays(var19.optimizeTo, var19.optimizeFrom) + 1;
            var8 = SQTime.getDiffInDays(var19.runTo, var19.runFrom) + 1;
            var18 = var19;
         } else {
            if (this.optimizationSettings.floating) {
               var19.optimizeFrom = var7.getMilis();
            } else {
               var19.optimizeFrom = var6.getMainChart().getHistoryFrom();
            }

            if (var11 < 0L) {
               var11 = var19.optimizeFrom;
            } else {
               var11 = var7.getMilis();
            }

            var19.optimizeTo = SQTime.correctDayEnd(this.addDaysReturnDate(var11, var1).getMilis());
            var19.runFrom = SQTime.correctDayStart(this.addDaysReturnDate(var19.optimizeTo, 1).getMilis());
            var19.runTo = this.addDaysReturnDate(var19.runFrom, var2).getMilis();
            var9 = var1;
            var8 = var2;
         }

         SQTimeOld var20 = this.addDaysReturnDate(var19.runTo, var8 + 1 + 1);
         if (var20.getMilis() > var6.getMainChart().getHistoryTo()) {
            var20.addDays(-var8 - 1);
            int var14 = SQTimeOld.getDaysBetween(var20.getMilis(), var6.getMainChart().getHistoryTo());
            if (var14 < 0) {
               if (var4 == null) {
                  break;
               }

               if (var18 != null) {
                  int var21 = SQTime.getDaysBetween(var18.runFrom, var18.runTo);
                  if (var8 < var21 * 0.6) {
                     break;
                  }
               }
            }

            if (var14 < var8 / 3) {
               var19.runTo = var6.getMainChart().getHistoryTo();
               var13 = true;
            }
         }

         if (!var13 && var19.runTo >= var6.getMainChart().getHistoryTo()) {
            var19.runTo = var6.getMainChart().getHistoryTo();
            var13 = true;
         }

         int var25 = Math.abs(SQTime.getDaysBetween(var6.getMainChart().getHistoryTo(), var19.runTo));
         if (!var13 && var25 >= 0 && var25 <= 0.3 * var8) {
            var19.runTo = var6.getMainChart().getHistoryTo();
            var13 = true;
         }

         if (var19.runTo < var19.runFrom) {
            break;
         }

         var24.add(var19);
         if (var3 <= 0 || var4 == null) {
            var7.addDays(var2 + 1);
         }
      } while (!var13);

      if (var3 > 0 && var4 != null) {
         WalkForwardPeriod var29 = (WalkForwardPeriod)var24.get(var24.size() - 1);
         var29.runTo = var6.getMainChart().getHistoryTo();
      }

      WalkForwardPeriod var30 = new WalkForwardPeriod();
      var30.optimizeTo = var6.getMainChart().getHistoryTo();
      if (this.optimizationSettings.floating) {
         var30.optimizeFrom = this.addDaysReturnDate(var30.optimizeTo, -var9 - 1).getMilis();
      } else {
         var30.optimizeFrom = var6.getMainChart().getHistoryFrom();
      }

      var30.runFrom = var6.getMainChart().getHistoryTo();
      var30.runTo = this.addDaysReturnDate(var30.runFrom, var8).getMilis();
      var30.futurePeriod = true;
      var24.add(var30);
      return var24;
   }

   private SQTimeOld addDaysReturnDate(long var1, int var3) {
      SQTimeOld var4 = new SQTimeOld(var1);
      var4.addDays(var3);
      return var4;
   }
}
