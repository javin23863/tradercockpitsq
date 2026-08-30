package com.strategyquant.tradinglib.optimization;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.L;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.ConditionsChecker;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.gp.EvolutionPipeline;
import com.strategyquant.tradinglib.gp.GPEngine;
import com.strategyquant.tradinglib.gp.GPEvolutionMessage;
import com.strategyquant.tradinglib.gp.GPEvolutionPopulationMessage;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.GPSettings;
import com.strategyquant.tradinglib.gp.IGPEvolutionMessagesListener;
import com.strategyquant.tradinglib.gp.RouletteWheelSelection;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.simplegrid.SimpleGridEngine;
import com.strategyquant.tradinglib.wfo.WFOSimulationEngine;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationEngineSimple extends OptimizationEngineBase {
   public static final Logger Log = LoggerFactory.getLogger("OptimizationEngineSimple");
   private int lastIndex;
   protected WFOSimulationEngine wfoSimulationEngine;
   protected int actualStep;
   private GPEngine<OptimizationIndexes> gpEngine;
   private GridJob parentGridJob;
   protected OptimizationProfile optimizationProfile;
   protected ConditionsChecker conditionsChecker = new ConditionsChecker();

   public OptimizationEngineSimple(String var1, StopPauseEngine var2, GridJob var3, String var4, ILastEventListener var5, String var6) {
      super(var1, var2, var4, var5, var6);
      this.parentGridJob = var3;
   }

   protected int getTotalStepsForSimpleOptimization() {
      long var1 = this.parameters.getTotalCombinations();
      PopulationGenerations var3 = OptimizationMethods.suggestPopulationGenerations(
         var1, this.optimizationSettings.optimizationMethod, this.optimizationSettings.maxOptimizationBacktests
      );
      return var3.method == 1 ? (int)var1 + 1 : 1 + var3.generations * var3.population;
   }

   protected int runSimpleOptimization(int var1, long var2, long var4, int var6, boolean var7) throws Exception {
      long var8 = this.parameters.getTotalCombinations();
      PopulationGenerations var10 = OptimizationMethods.suggestPopulationGenerations(
         var8, this.optimizationSettings.optimizationMethod, this.optimizationSettings.maxOptimizationBacktests
      );
      long var11 = OptimizationMethods.getTotalCombinations(var8, var10, this.optimizationSettings.maxOptimizationBacktests);
      int var13 = 0;
      this.wfoSimulationEngine = null;
      if (var1 == 0) {
         short[] var14 = this.prepareIndexesArray(this.parameters.getUsedCount());
         String var15 = this.settings.getString("StrategyName");
         this.printLog(L.t("Testing original parameters", new Object[0]));
         if (var8 == 1L || var7 || !this.optimizationSettings.dontSaveOriginalStr) {
            if (var8 == 1L) {
               var15 = var15 + "(Optimized)";
            }

            ResultsGroup var16 = this.testStrategy(var14, var15, var15, 0L, 0L);
            var13 = var16.portfolio().stats((byte)0, (byte)10, (byte)127).getInt("NumberOfTrades");
            if (var7) {
               this.wfoSimulationEngine = new WFOSimulationEngine(this.optimizationSettings.periodType == 30);
            }

            this.addResultToDatabank(var16);
            this.optimizationProfile.addOriginalResult(var16);
            if (var8 == 1L) {
               return var13;
            }
         }

         this.increaseStep();
      }

      if (var8 == 1L) {
      }

      if (var10.method == 1) {
         this.printLog(L.t("Brute Force method", new Object[0]));
         this.runBruteForceOptimization(var1, var2, var4, var6, var8);
      } else {
         this.printLog(L.t("Genetic optimization method", new Object[0]));
         Log.debug("Population: {}, Generations: {}", var10.population, var10.generations);
         this.printLog(L.t("Genetic optimization of parameters started, please wait, it might take some time...", new Object[0]));
         this.runGeneticOptimization(var10, var1, var2, var4, var6, var8, var11);
      }

      return var13;
   }

   private void runGeneticOptimization(PopulationGenerations var1, int var2, long var3, long var5, final int var7, long var8, long var10) throws Exception {
      GPSettings var12 = this.getGPSettings(var1);
      this.gpEngine = new GPEngine<>(var12, this.progressEngine, null, this.parentGridJob, this.source);
      this.gpEngine.setEvolutionMessagesListener(new IGPEvolutionMessagesListener<OptimizationIndexes>() {
         public void newCandidate(OptimizationIndexes var1) {
            OptimizationEngineSimple.this.processNewCandidate(var1, OptimizationEngineSimple.this.gpEngine, var7);
         }

         @Override
         public void evolutionMessage(GPEvolutionMessage var1) {
            OptimizationEngineSimple.this.processEvolutionMessage(var1);
         }

         @Override
         public void lastPopulation(GPEvolutionPopulationMessage var1) {
         }

         @Override
         public void evolutionException(Serializable var1) {
         }
      });
      this.gpEngine.start();
      if (!var12.singleThreaded) {
         do {
            Thread.sleep(200L);
         } while (!this.gpEngine.isFinished() && !this.progressEngine.isStopped());
      }

      this.gpEngine.unregisterListeners();
   }

   private GPSettings<OptimizationIndexes> getGPSettings(PopulationGenerations var1) throws Exception {
      GPSettings var2 = new GPSettings();
      var2.populationSize = var1.population;
      var2.maxGenerations = var1.generations;
      var2.elitismSize = var2.populationSize <= 10 ? 1 : 2;
      var2.decimationCoefficient = 0;
      var2.numberOfIslands = 1;
      var2.migrationRate = 0.1;
      var2.migrationGenerationsModulo = 8;
      var2.naturalFitness = true;
      var2.printPopulationToLog = true;
      var2.sendEveryCandidateEvent = true;
      var2.rng = new MersenneTwisterRng();
      OptimizationIndexesFactory var3 = new OptimizationIndexesFactory(this.parameters);
      var2.factory = var3;
      ArrayList var4 = new ArrayList(3);
      var4.add(new OptimizationIndexesCrossover(this.parameters));
      var4.add(new OptimizationIndexesMutation(this.parameters, 0.2));
      var2.evolutionPipeline = new EvolutionPipeline(var4);
      var2.evaluator = new OptimizerBacktestEvaluator(
         this.getBacktestSettings(), this.optimizationSettings, this.progressEngine, this.lastEventListener, this.lastSettingsXml
      );
      var2.selectionStrategy = new RouletteWheelSelection();
      var2.singleThreaded = this.optimizationSettings.singleThreaded;
      return var2;
   }

   private BacktestSettings getBacktestSettings() throws Exception {
      BacktestSettings var1 = new BacktestSettings();
      var1.initialCapital = (Double)this.settings.get("MoneyManagement.InitialCapital");
      var1.fitnessFunction = (IFitnessFunction)this.settings.get("FitnessFunction");
      var1.tradingOptions = ((TradingOptions)this.settings.get("TradingOptions")).getClone();
      var1.moneyManagementMethod = (MoneyManagementMethod)this.settings.get("MoneyManagement.Method");
      var1.riskManagementMethod = (RiskManagementMethod)this.settings.get("RiskManagement");
      ATM var2 = (ATM)this.settings.get("ATM");
      var1.atm = var2 != null ? var2.getClone() : null;
      var1.rankingConditions = null;
      var1.dismissBadStrategies = 0;
      var1.outOfSample = null;
      var1.chartSetups = ((ChartSetups)this.settings.get("ChartSetups")).getClone(0L, 0L);
      var1.useCrossChecks = false;
      var1.crossChecks = null;
      var1.slippage = (Double)this.settings.get("Slippage");
      var1.minDistance = (Double)this.settings.get("MinDistance");
      var1.commission = (CommissionsMethod)this.settings.get("Commission");
      var1.swap = (SwapMethod)this.settings.get("Swap");
      var1.strategyName = this.settings.getString("StrategyName");
      var1.elStrategy = (Element)this.settings.get("StrategyXml");
      var1.lastSettings = (Element)this.settings.get("StrategyLastSettings");
      return var1;
   }

   protected void processNewCandidate(OptimizationIndexes var1, GPEngine<OptimizationIndexes> var2, int var3) {
      GPIDs var4 = var1.getGPIDs();
      String var5 = String.format("Candidate %d.%d.%d", var4.islandIndex, var4.generationIndex, var4.nodeIndex);
      int var6 = var1.getDismissalReason();
      String var7 = String.format(".%2f", var1.getFitness((byte)0));
      String var8 = OptimizationIndexes.convertToString(var1.getIndexes());
      if (var6 == 0) {
         ResultsGroup var9 = var1.getResult();
         if (var9 != null) {
            try {
               double var10 = var9.specialValues().getDouble(SpecialValues.BacktestDuration, 0.0);
               this.processOptimizationResult(var9, var3, var10);
            } catch (Exception var12) {
               var12.printStackTrace();
               Log.error("Exception processing result", var12);
            }
         } else {
            Log.debug("Candidate {} doesn't have test result set!", var5);
         }
      } else {
         Log.debug("Dismissed {} with fitness {} - {} : {}", new Object[]{var5, var7, var1.getErrorMsg(), var8});
      }
   }

   protected void processEvolutionMessage(GPEvolutionMessage var1) {
      if (var1.messageType == 30) {
         this.increaseStep(var1.populationSize);
      }
   }

   private void runBruteForceOptimization(final int var1, long var2, long var4, final int var6, final long var7) throws Exception {
      if (var6 == 1) {
      }

      this.lastIndex = 0;
      SimpleGridEngine var9 = new SimpleGridEngine<OptimizationTestJob, BacktestResult>(
         this.progressEngine, this.parentGridJob, this.optimizationSettings.singleThreaded
      ) {
         @Override
         protected ArrayList<OptimizationTestJob> createJobsBatch(int var1x, GridClient var2x) throws Exception {
            return OptimizationEngineSimple.this.createBatch(var1, var1x, var2x, var7);
         }

         protected void processResult(BacktestResult var1x, JobDetails var2x) throws Exception {
            OptimizationEngineSimple.this.processSimpleOptimizationResult(var1x, var2x, var6);
         }

         @Override
         protected void onError(String var1x, Exception var2x) {
            OptimizationEngineSimple.this.printLog(var1x);
            OptimizationEngineSimple.this.progressEngine.stop();
         }
      };
      var9.start();
      var9.unregisterListeners();
   }

   protected ArrayList<OptimizationTestJob> createBatch(int var1, int var2, GridClient var3, long var4) throws Exception {
      ArrayList var6 = new ArrayList();
      int var7 = 0;

      for (long var8 = this.lastIndex; var8 < this.lastIndex + var2 && var8 < var4; var8++) {
         String var10 = String.format("Optimized %d.%d", var1, var8);
         OptimizationTestJob var11 = new OptimizationTestJob(var10, this.getParams(var10, var8), this.progressEngine, this.optimizationSettings.periodType);
         var6.add(var11);
         var7++;
      }

      this.lastIndex += var7;
      return var6.size() == 0 ? null : var6;
   }

   private Map<String, Serializable> getParams(String var1, long var2) throws Exception {
      short[] var4 = this.prepareIndexesArray(this.parameters.getUsedCount());
      this.parameters.prepareTestParameters(var2, var4);
      HashMap var5 = new HashMap();
      String var6 = this.settings.getString("StrategyName");
      var5.put("ChartSetups", ((ChartSetups)this.settings.get("ChartSetups")).getClone());
      var5.put("Slippage", this.settings.getDouble("Slippage"));
      var5.put("MinDistance", this.settings.getDouble("MinDistance"));
      var5.put("Commission", (CommissionsMethod)this.settings.get("Commission"));
      var5.put("Swap", (SwapMethod)this.settings.get("Swap"));
      var5.put("MoneyManagement.InitialCapital", this.settings.getDouble("MoneyManagement.InitialCapital"));
      var5.put("MoneyManagement.Method", (MoneyManagementMethod)this.settings.get("MoneyManagement.Method"));
      var5.put("RiskManagement", (RiskManagementMethod)this.settings.get("RiskManagement"));
      var5.put("TestPrecision", this.settings.getInt("TestPrecision"));
      var5.put("ResultGroupName", var6 + " - " + var1);
      var5.put("MainResultKey", var6);
      var5.put("FitnessFunction", this.fitnessFunction);
      var5.put("TradingOptions", ((TradingOptions)this.settings.get("TradingOptions")).getClone());
      ATM var7 = (ATM)this.settings.get("ATM");
      if (var7 != null) {
         var5.put("ATM", var7.getClone());
      }

      StrategyBase var8 = (StrategyBase)this.settings.get("StrategyObject");
      StrategyParamData var9 = StrategyBase.createStrategyVariation(var8, this.optimizationSettings, this.optimizationSettings.transformToFullIndexes(var4));
      StrategyBase var10 = var9.getStrategy();
      String var11 = var9.getParams();
      var5.put("StrategyXml", var10.getStrategyXml());
      var5.put("StrategyLastSettings", (Element)this.settings.get("StrategyLastSettings"));
      var5.remove("StrategyObject");
      var5.put("OptimizationParams", var11);
      var5.put("OptimizationParamIndexes", var4);
      return var5;
   }

   protected void processSimpleOptimizationResult(BacktestResult var1, JobDetails var2, int var3) throws Exception {
      String var4 = var2.getJobID();
      this.increaseStep();
      if (var2.isSuccess()) {
         ResultsGroup var5 = var1.getResult();
         if (var5 != null) {
            this.processOptimizationResult(var5, var3, var2.getDuration() / 1000.0);
         } else {
            this.printLog(L.t("Optimization %s failed, error: %s", new Object[]{var4, var1.getDismissalMessage()}));
         }
      } else {
         var2.getException();
         this.printLog(L.t("Optimization %s failed, error: %s", new Object[]{var4, var2.getException()}));
      }
   }

   private void processOptimizationResult(ResultsGroup var1, int var2, double var3) throws Exception {
      if (var2 != 1 || this.wfoSimulationEngine != null) {
         this.printLog(L.t("Testing %s, OK took %.3f s.", new Object[]{var1.getName(), var3}));
      }

      this.optimizationProfile.addResult(var1);
      if (var1.getOptimizationProfile() == null) {
         var1.setOptimizationProfile(this.optimizationProfile);
      }

      boolean var5 = true;
      if (var2 == 1 && this.wfoSimulationEngine == null) {
         String var6 = this.checkConditions(var1);
         if (var6 != null) {
            this.printLog(L.t("Testing %s, dismissed : %s", new Object[]{var1.getName(), var6}));
            if (this.optimizationSettings.type == 4) {
               this.addResultToDatabank(var1);
            }
         } else {
            this.printLog(L.t("Testing %s, OK took %.3f s.", new Object[]{var1.getName(), var3}));
            this.addResultToDatabank(var1);
         }

         var5 = false;
      } else if (this.optimizationSettings.type == 4) {
         this.addResultToDatabank(var1);
      }

      if (this.wfoSimulationEngine != null) {
         this.wfoSimulationEngine.saveTestToFile(var1);
      }

      if (var5) {
         var1.clear();
      }
   }

   private String checkConditions(ResultsGroup var1) {
      if (this.optimizationSettings.conditions == null) {
         return null;
      }

      for (Condition var3 : this.optimizationSettings.conditions) {
         try {
            if (var3.isUsed()) {
               boolean var4 = this.conditionsChecker.check(var1, var3);
               if (!var4) {
                  return this.conditionsChecker.dismissalMessage;
               }
            }
         } catch (CrossCheckDataNotExistException var5) {
         } catch (Exception var6) {
            this.printLog(String.format("Error while evaluating conditions: %s", var6.getMessage()));
            Log.error("Error while evaluating condition: ", var6);
         }
      }

      return null;
   }

   protected void clearOptimizationProfile() {
      if (this.optimizationProfile != null) {
         this.optimizationProfile.compute(true, true);
         this.optimizationProfile.clearData();
      }
   }
}
