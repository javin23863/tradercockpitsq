package com.strategyquant.plugin.Task.impl.Build;

import com.strategyquant.allTimeStats.AllTimeStats;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.pp.ProjectPanel;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import com.strategyquant.tradinglib.backtestrunner.BacktestSettings;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.DismissStruct;
import com.strategyquant.tradinglib.correlation.FitPortfolio;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.exception.TaskErrorInfo;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.gp.EvolutionPipeline;
import com.strategyquant.tradinglib.gp.GPEngine;
import com.strategyquant.tradinglib.gp.GPEvolutionMessage;
import com.strategyquant.tradinglib.gp.GPEvolutionPopulationMessage;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.GPSettings;
import com.strategyquant.tradinglib.gp.IGPEvolutionMessagesListener;
import com.strategyquant.tradinglib.gp.IGPFinishedListener;
import com.strategyquant.tradinglib.gp.IStopConditionsChecker;
import com.strategyquant.tradinglib.gp.SelectedBlockGroups;
import com.strategyquant.tradinglib.gp.TournamentSelection;
import com.strategyquant.tradinglib.gp.strategies.BacktestEvaluator;
import com.strategyquant.tradinglib.gp.strategies.BuildSettings;
import com.strategyquant.tradinglib.gp.strategies.FixCustomBlocks;
import com.strategyquant.tradinglib.gp.strategies.FixNonRandomBlocks;
import com.strategyquant.tradinglib.gp.strategies.FixNumberOfExitTypes;
import com.strategyquant.tradinglib.gp.strategies.FixStockpickerBlocks;
import com.strategyquant.tradinglib.gp.strategies.FixUnusedDependentFormulas;
import com.strategyquant.tradinglib.gp.strategies.Node;
import com.strategyquant.tradinglib.gp.strategies.NodeCrossover;
import com.strategyquant.tradinglib.gp.strategies.NodeFactory;
import com.strategyquant.tradinglib.gp.strategies.NodeMutation;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import com.strategyquant.tradinglib.task.settings.buildmode.BuildMode;
import com.strategyquant.tradinglib.task.settings.buildtype.BuildType;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneticBuildEngine implements IBuildEngine, IStopConditionsChecker, ILastEventListener {
   public static final Logger Log = LoggerFactory.getLogger(GeneticBuildEngine.class);
   private static final String LOCK_GB = "GeneticBuildEngine";
   private SettingsMap settings;
   private Element taskSettings;
   private ProgressEngine progressEngine;
   private Databank initialPopulationDatabank;
   private Databank outputDatabank;
   private Databank lastGenerationDatabank;
   private double lastWorstFitness = 0.0;
   private GPEngine<Node> gpEngine;
   private SQProject project;
   private String lastSettingsXml;
   private TaskErrorInfo taskErrorInfo = null;
   private BacktestSettings backtestSettings;
   private IGPFinishedListener finishedListener;
   private int maxDatabankRecords;
   private String dismissMessage;
   private int dismissReason;
   private boolean finishSent = false;
   private int taskHash;
   private String strategyNamePrefix = "Strategy ";
   private CustomAnalysisMethod caMethod;
   private String caInputArgs;
   private boolean caFilter;

   public GeneticBuildEngine(
      SettingsMap var1,
      String var2,
      ProgressEngine var3,
      Databank var4,
      Databank var5,
      Databank var6,
      Element var7,
      String var8,
      int var9,
      CustomAnalysisMethod var10,
      boolean var11,
      String var12
   ) throws Exception {
      this.settings = var1;
      this.taskSettings = var7;
      this.progressEngine = var3;
      this.initialPopulationDatabank = var4;
      this.outputDatabank = var5;
      this.lastGenerationDatabank = var6;
      this.project = ProjectEngine.get(var2);
      this.lastSettingsXml = var8;
      this.taskHash = var9;
      this.caMethod = var10;
      this.caFilter = var11;
      this.caInputArgs = var12;
      this.maxDatabankRecords = var1.getInt("MaxStrategies", 100000);
      this.backtestSettings = this.getBacktestSettings();
      GPSettings var13 = this.getGPSettings();
      ArrayList var14 = this.getInitialPopulation();
      this.gpEngine = new GPEngine(var13, var3, var14, null, "Builder", this);
      this.gpEngine.setEvolutionMessagesListener(new IGPEvolutionMessagesListener<Node>() {
         public void newCandidate(Node var1) {
            GeneticBuildEngine.this.processNewCandidate(var1, GeneticBuildEngine.this.gpEngine);
         }

         public void evolutionMessage(GPEvolutionMessage var1) {
            GeneticBuildEngine.this.processEvolutionMessage(var1);
         }

         public void lastPopulation(GPEvolutionPopulationMessage var1) {
            GeneticBuildEngine.this.processLastPopulation(var1);
         }

         public void evolutionException(Serializable var1) {
            GeneticBuildEngine.this.processEvolutionException(var1);
         }
      });
      this.gpEngine.setFinishListener(new IGPFinishedListener() {
         public void finished(int var1) {
            GeneticBuildEngine.this.buildFinished(var1);
         }
      });
      this.project.resetGeneticInfo(true, var13.numberOfIslands);
   }

   protected void processEvolutionException(Serializable var1) {
      try {
         String var2 = (String)var1;
         this.progressEngine.printToLog(var2);
      } catch (Exception var3) {
      }
   }

   protected void buildFinished(int var1) {
      if (this.taskErrorInfo != null && var1 != 3) {
         File var2 = null;

         try {
            var2 = ProjectConfigHelper.getStrategyFile(this.outputDatabank, this.taskErrorInfo.strategyName, 1024, "GeneticBuildEngine");
         } catch (Exception var4) {
         }

         ProjectPanel.reportBug(this.taskSettings, var2, this.taskErrorInfo.strategyName, this.taskErrorInfo.exception);
      }

      if (this.finishedListener != null) {
         this.finishedListener.finished(var1);
      }
   }

   @Override
   public void testRun() throws Exception {
   }

   @Override
   public void start() throws Exception {
      this.gpEngine.start();
   }

   private void processNewCandidate(Node var1, GPEngine<Node> var2) {
      if (SQProject.isMemoryProtectionUsed()) {
         try {
            MemoryUsageChecker.checkAvailableMemory();
         } catch (Error var13) {
            this.project.onMemoryError(var13);
            if (var1.getResultGroup() != null) {
               var1.getResultGroup().clear();
            }

            return;
         }
      }

      if (this.shouldStop()) {
         if (!this.finishSent) {
            this.progressEngine.finish();
            this.finishSent = true;
         }

         Log.debug("Stopped, stop conditions met");
      } else {
         synchronized (this.outputDatabank) {
            String var4 = var1.getStrategyName(this.strategyNamePrefix);
            this.dismissMessage = null;
            this.dismissReason = var1.getDismissalReason();
            DurationStats var5 = var1.getDurationStats();
            AllTimeStats.getInstance().increaseStrategiesGenerated();
            ResultsGroup var6 = null;
            if (this.dismissReason == 0) {
               var6 = var1.getResultGroup();
               Log.debug("Received candidate {} with fitness {}", var4, var1.getFitness((byte)11));
               if (this.progressEngine.isStopped()) {
                  var6.clear();
                  Log.debug("Stopped, destroying received candidate {}", var4);
                  return;
               }

               GPIDs var7 = var1.getGPIDs();
               if (var7.generationIndex == 0) {
                  this.dismissReason = 10005;
                  this.dismissMessage = "initial generation, not saving to databank";
               }

               if (var7.islandIndex == 0 && this.dismissReason == 0) {
                  this.checkAutomaticDismissalRules(var6);
               }

               if (this.dismissReason == 0) {
                  if (!this.backtestSettings.warningsBadStrategies && !var6.isStockpickerStrategy()) {
                     var6.specialValues().set("StrategyProblems", 0);
                  }

                  var6.setName(var4);
                  boolean var8 = true;
                  if (this.caMethod != null) {
                     try {
                        this.caMethod.setInputArgs(this.caInputArgs);
                        var8 = this.caMethod.filterStrategy(this.project.getName(), this.project.getActiveTaskName(), this.outputDatabank.getName(), var6);
                     } catch (Exception var14) {
                        Log.error("Cannot add result to databank", var14);
                        if (var6 != null) {
                           var6.clear();
                        }
                     }
                  }

                  if (!var8 && this.caFilter) {
                     var6.specialValues().setString(SpecialValues.FiltersResultFailedReason, BadStrategyException.getReasonAsString(10038));
                     this.dismissReason = 10038;
                     this.setDismissalMessage(var1, var4);
                  } else {
                     this.addToDatabank(var6);
                  }
               }
            } else {
               this.setDismissalMessage(var1, var4);
            }

            JobDetails var16 = var1.getJobDetails();
            double var17 = var16.getDuration() / 1000.0;
            if (this.dismissMessage == null) {
               try {
                  if (this.dismissReason == 0 && var6.mainResult().containsKey("ReplacedWorseStrategy")) {
                     this.dismissReason = var6.mainResult().getInt("ReplacedWorseStrategy");
                  }
               } catch (Exception var12) {
               }

               this.printNewStrategyToLog(var16, var4, var5, null, var17);
               this.project.trackJob(var16, this.dismissReason, var5, 0, 0L, 0L);
            } else {
               this.printNewStrategyToLog(var16, var4, var5, this.dismissMessage, var17);
               this.project.trackJob(var16, this.dismissReason, var5, 0, 0L, 0L);
               if (this.dismissMessage.contains("OutOfMemoryError")) {
                  this.project.onMemoryError(null);
               }
            }

            this.project.getTrackingInfo().infiniteProgress = true;
            this.project
               .increaseGlobalStats(this.taskHash, L.t("Generated", new Object[0]), 1L, L.t("Accepted", new Object[0]), this.dismissReason == 0 ? 1L : 0L);
            if (this.project.getFailedJobsPercentage() > 0.7) {
               this.progressEngine.printToLog(L.t("GRID: Stopping build, job failures (exceptions) exceeded 70%!", new Object[0]));
               this.progressEngine.stop();
            }
         }
      }
   }

   private void checkAutomaticDismissalRules(ResultsGroup var1) {
      try {
         int var2 = var1.mainResult().getStrategyProblems();
         var2 &= this.backtestSettings.dismissBadStrategies;
         if (var2 != 0) {
            this.dismissReason = BadStrategyException.getFirstReason(var2);
            this.dismissMessage = BadStrategyException.getReasonAsString(this.dismissReason);
         }
      } catch (Exception var3) {
         Log.error("Error", var3);
      }
   }

   private void setDismissalMessage(Node var1, String var2) {
      if (this.dismissReason == 10001) {
         Exception var3 = var1.getException();
         if (var3 != null) {
            if (this.taskErrorInfo == null) {
               this.taskErrorInfo = new TaskErrorInfo(var2, SQUtils.stackTraceToString(var3));
            }

            this.project.increaseFailedJobsCount();
         }

         this.dismissMessage = var1.getErrorMsg();
      } else if (this.dismissReason == 10000) {
         this.dismissMessage = L.t("dismissed, all strategies in databank are better", new Object[0]);
      } else if (this.dismissReason == 10005) {
         this.dismissMessage = L.t("initial generation, not saving to databank", new Object[0]);
      } else if (this.dismissReason == 10038) {
         this.dismissMessage = BadStrategyException.getReasonAsString(this.dismissReason);
      } else {
         this.dismissMessage = var1.getErrorMsg();
      }
   }

   private void addToDatabank(ResultsGroup var1) {
      try {
         var1.removeUnsavableSettings();
         var1.specialValues().set(SpecialValues.DateGenerated, SQTime.getLocalCurrentTimeInMs());
         if (!this.outputDatabank.add(var1, true)) {
            this.dismissMessage = L.t("dismissed by databank", new Object[0]);
            this.dismissReason = 10006;
            var1.clear();
            var1 = null;
         } else {
            this.outputDatabank.updateBestResults(var1);
            AllTimeStats.getInstance().increaseStrategiesAccepted();
         }

         if (this.outputDatabank.getRecordsSizeNoLock() >= this.maxDatabankRecords - 5) {
            double var2 = this.outputDatabank.getWorstFitness();
            if (var2 > 0.0 && var2 > this.lastWorstFitness) {
               this.lastWorstFitness = var2;
               this.gpEngine.broadcastNewThresholdFitness(this.lastWorstFitness);
            }
         }
      } catch (Exception var4) {
         this.dismissMessage = L.t("exception", new Object[0]);
         Log.error("Error adding strategy to databank", var4);
      } catch (OutOfMemoryError var5) {
         var1.clear();
         this.project.onMemoryError(var5);
      }
   }

   public boolean shouldStop() {
      return BuildStopConditionsChecker.shouldStop(this.outputDatabank, this.settings, this.progressEngine, this.project.getTrackingInfo());
   }

   private void processLastPopulation(GPEvolutionPopulationMessage<Node> var1) {
      Log.debug(
         "\n\nLast population received: {}, {}, {} ===============================",
         new Object[]{var1.currentGeneration, var1.islandIndex + 1, var1.population.size()}
      );
      if (this.lastGenerationDatabank != null) {
         try {
            this.lastGenerationDatabank.removeAll(true, "GenEvo processLastPopulation- Remove last generation strategies.");
         } catch (Exception var8) {
            Log.error("Removing last generation strategies failed", var8);
         }

         for (int var2 = 0; var2 < var1.population.size(); var2++) {
            Node var3 = (Node)var1.population.get(var2);
            GPIDs var4 = var3.getGPIDs();
            ResultsGroup var5 = var3.getResultGroup();

            try {
               if (var5 != null) {
                  this.lastGenerationDatabank.add(var5, false);
               }
            } catch (Exception var7) {
               Log.error("Adding last generation strategy failed", var7);
            }
         }

         this.lastGenerationDatabank.refreshGrid();
      }
   }

   private void printNewStrategyToLog(JobDetails var1, String var2, DurationStats var3, String var4, double var5) {
      if (!this.progressEngine.isStopped()) {
         StringBuilder var7 = new StringBuilder(var2);
         if (var3 == null) {
            if (var4 == null) {
               var7.append("\n   - " + L.t("OK in %s s.", new Object[]{String.format("%.2f", var5)}));
            } else {
               if (var4.contains(var2 + ", ")) {
                  var4 = var4.replaceAll(var2 + ", ", "");
               }

               var7.append("\n   - " + var4);
               var7.append(" - " + L.t("in %s s.", new Object[]{String.format("%.2f", var5)}));
            }
         } else {
            HashMap var8 = var3.getMap();
            String var9 = null;

            for (String var11 : var3.getKeys()) {
               if (var4 != null && var4.contains(var11)) {
                  var9 = var11;
               } else {
                  if (var11.equals(DurationStats.MainTest)) {
                     var7.append("\n   - " + var11 + " - ");
                  } else {
                     var7.append("\n   - " + L.t("Cross check", new Object[0]) + " - " + var11 + " - ");
                  }

                  var7.append(L.t("OK in %s s.", new Object[]{String.format("%.2f", var8.get(var11))}));
               }
            }

            if (var4 != null) {
               if (var4.contains(var2 + ", ")) {
                  var4 = var4.replaceAll(var2 + ", ", "").trim();
               }

               var7.append("\n   - " + var4);
               if (var9 != null) {
                  if (!var4.contains("dismissed")
                     && !var4.contains("exception")
                     && !var4.contains(L.t("dismissed", new Object[0]))
                     && !var4.contains(L.t("exception", new Object[0]))) {
                     var7.append(" " + L.t("ok", new Object[0]));
                  }

                  var7.append(L.t(" in %s s.", new Object[]{String.format("%.2f", var8.get(var9))}));
               }
            }
         }

         String var12 = var1.getException();
         if (var12 != null) {
            var7.append(var1.getJobID());
            if (this.taskErrorInfo == null) {
               this.taskErrorInfo = new TaskErrorInfo(var2, var12);
            }

            var7.append(" - Error: ");
            var7.append(var12.length() > 500 ? var12.substring(0, 500) : var12);
         }

         this.progressEngine.printToLogDebug(var7.toString());
      }
   }

   private void processEvolutionMessage(GPEvolutionMessage var1) {
      if (var1.messageType == 10) {
         try {
            this.lastGenerationDatabank.clearRecords(true, true, "GenEvo InitialPopulationStarted - clean lastGenerationDatabank");
         } catch (Exception var3) {
            Log.error("Cannot clean databank", var3);
         }
      }

      if (var1.messageType != 50) {
         this.progressEngine.printToLogDebug(var1.message);
      }

      this.project.updateGeneticInfo(var1, var1.messageType == 30);
   }

   private DismissStruct conditionsPass(ResultsGroup var1) {
      int var2 = 200000;
      if (this.backtestSettings.rankingConditions != null && this.backtestSettings.rankingConditions.size() > 0) {
         for (int var3 = 0; var3 < this.backtestSettings.rankingConditions.size(); var3++) {
            Condition var4 = (Condition)this.backtestSettings.rankingConditions.get(var3);
            if (var4.isUsed()) {
               try {
                  if (!var4.isMet(new Object[]{var1})) {
                     return new DismissStruct(
                        var2 + var3, String.format(DurationStats.MainTest + " %s, - dismissed: %s", var1.getName(), var4.getAsText(new Object[]{var1}))
                     );
                  }
               } catch (Exception var6) {
                  Log.error("Error while checking condition", var6);
                  return new DismissStruct(10003, String.format("Error evaluating conditions: %s", var6.getMessage()));
               }
            }
         }
      }

      return null;
   }

   private GPSettings<Node> getGPSettings() throws Exception {
      BuildMode var1 = (BuildMode)this.settings.get("BuildMode");
      GPSettings var2 = new GPSettings();
      var2.freshBloodReplaceSimilar = var1.freshBloodReplaceSimilar;
      var2.freshBloodReplaceWeakest = var1.freshBloodReplaceWeakest;
      var2.replaceWeakestPct = var1.freshBloodWeakestPct;
      var2.replaceWeakestGenerations = var1.freshBloodWeakestGenerations;
      var2.sendLastPopulation = var1.showLastGeneration;
      var2.populationSize = var1.populationSize;
      var2.elitismSize = this.getElitismFromPopulationSize(var1.populationSize);
      var2.maxGenerations = var1.maxGenerations;
      var2.decimationCoefficient = var1.decimationCoef;
      var2.numberOfIslands = var1.islands;
      var2.migrationRate = SQUtils.pctToDouble(var1.migrationRate);
      var2.migrationGenerationsModulo = var1.migrationModulo;
      var2.restartOnFinish = var1.evoRestartOnFinish;
      var2.restartOnStagnation = var1.evoRestartOnStagnation;
      var2.stagnationGenerationsToRestart = var1.evoStagnationRestartGenerations;
      var2.stagnationFitnessRestartType = var1.evoFitnessRestartType;
      var2.trainingValidationRatio = var1.evoTrainingValidationRatio;
      var2.evolutionUniquenessRetries = 1;
      var2.naturalFitness = true;
      var2.rng = new MersenneTwisterRng();
      BuildSettings var3 = this.getBuildSettings();
      var3.tradingOptions = (TradingOptions)this.settings.get("TradingOptions");
      NodeFactory var4 = new NodeFactory(var3);
      SelectedBlockGroups var5 = new SelectedBlockGroups(var3);
      var2.factory = var4;
      BuildType var6 = (BuildType)this.settings.get("WhatToBuild");
      ArrayList var7 = new ArrayList(3);
      var7.add(new NodeCrossover(SQUtils.pctToDouble(var1.crossoverProbability), 2, var3, var5));
      var7.add(new NodeMutation(SQUtils.pctToDouble(var1.mutationProbability), var3, var5));
      var7.add(new FixNonRandomBlocks(var6));
      var7.add(new FixUnusedDependentFormulas());
      var7.add(new FixCustomBlocks());
      var7.add(new FixStockpickerBlocks());
      var7.add(new FixNumberOfExitTypes(var3.getBuildSettingsMap(), var3.getReplacements()));
      var2.evolutionPipeline = new EvolutionPipeline(var7);
      var2.evaluator = new BacktestEvaluator(this.getBacktestSettings(), this, this.lastSettingsXml);
      var2.selectionStrategy = new TournamentSelection();
      return var2;
   }

   private ArrayList<Node> getInitialPopulation() throws Exception {
      if (this.initialPopulationDatabank != null && this.initialPopulationDatabank.size() != 0) {
         ArrayList var1 = new ArrayList();
         ArrayList var2 = this.initialPopulationDatabank.getRecordKeys();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            ResultsGroup var4 = this.initialPopulationDatabank.getLocked((String)var2.get(var3), "GeneticBuildEngine");
            Element var5 = null;

            try {
               var5 = var4.getStrategyXml();
            } finally {
               var4.releaseLock("GeneticBuildEngine");
            }

            if (var5 == null) {
               Log.info("Result {} doesn't contain any strategy", var4.getName());
            } else {
               Node var6 = new Node(var5);
               var1.add(var6);
            }
         }

         return var1;
      } else {
         return null;
      }
   }

   private int getElitismFromPopulationSize(int var1) {
      int var2 = var1 / 10;
      if (var2 > 5) {
         var2 /= 10;
      }

      if (var2 == 0) {
         var2 = 1;
      }

      return var2;
   }

   private BuildSettings getBuildSettings() throws Exception {
      Element var1 = (Element)this.settings.get("ConvertedBlocks");
      return new BuildSettings(
         var1.getChild("StrategyFile"),
         var1.getChild("Replacements"),
         (SettingsMap)this.settings.get("BuildSettings"),
         (ATMGenerateConfig)this.settings.get("ATMGenerateConfig")
      );
   }

   private BacktestSettings getBacktestSettings() throws Exception {
      BuildMode var1 = (BuildMode)this.settings.get("BuildMode");
      BacktestSettings var2 = new BacktestSettings();
      var2.initialCapital = (Double)this.settings.get("MoneyManagement.InitialCapital");
      var2.moneyManagementMethod = (MoneyManagementMethod)this.settings.get("MoneyManagement.Method");
      var2.riskManagementMethod = (RiskManagementMethod)this.settings.get("RiskManagement");
      var2.fitnessFunction = (IFitnessFunction)this.settings.get("FitnessFunction");
      var2.rankingConditions = (ArrayList)this.settings.get("Conditions");
      var2.dismissBadStrategies = (Integer)this.settings.get("StrategyDismissSettings");
      var2.warningsBadStrategies = (Boolean)this.settings.get("StrategyDismissWarnings");
      var2.initialGenerationConditions = var1.conditions;
      var2.tradingOptions = (TradingOptions)this.settings.get("TradingOptions");
      var2.atm = (ATM)this.settings.get("ATM");
      var2.outOfSample = (OutOfSample)this.settings.get("OutOfSample");
      var2.chartSetups = (ChartSetups)this.settings.get("ChartSetups");
      var2.useCrossChecks = (Boolean)this.settings.get("UseRobustnessTests");
      var2.crossChecks = (ArrayList)this.settings.get("CrossChecks");
      var2.slippage = (Double)this.settings.get("Slippage");
      var2.minDistance = (Double)this.settings.get("MinDistance");
      var2.commission = (CommissionsMethod)this.settings.get("Commission");
      var2.swap = (SwapMethod)this.settings.get("Swap");
      var2.fitPortfolio = (FitPortfolio)this.settings.get("FitPortfolio");
      return var2;
   }

   @Override
   public void setFinishListener(IGPFinishedListener var1) {
      this.finishedListener = var1;
   }

   @Override
   public void destroy() {
      this.gpEngine.destroy();
   }

   @Override
   public boolean getFinishSent() {
      return this.finishSent;
   }

   public void setLastEvent(String var1) {
      if (this.project != null) {
         this.project.setLastEvent(var1);
      }
   }

   @Override
   public void setStrategyNamePrefix(String var1) {
      this.strategyNamePrefix = var1;
   }
}
