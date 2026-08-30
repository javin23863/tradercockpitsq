package com.strategyquant.plugin.Task.impl.Build;

import com.strategyquant.allTimeStats.AllTimeStats;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.pp.ProjectPanel;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.correlation.FitPortfolio;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.exception.TaskErrorInfo;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.generator.StrategyGeneratorCache;
import com.strategyquant.tradinglib.gp.IGPFinishedListener;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.IProgressStatusListener;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomBuildEngine implements IBuildEngine, IGridMessageListener, IProgressStatusListener, ILastEventListener {
   public static final Logger Log = LoggerFactory.getLogger(RandomBuildEngine.class);
   private boolean TEST_ARTIFICAL_BUILD = false;
   private ProgressEngine progressEngine;
   private GridClient gridClient;
   private MersenneTwisterRng rng;
   private String jobGroupID;
   private long jobCount;
   private int jobsCountThreshold;
   private int runningStatus = 0;
   private Element taskSettings;
   private Element elReplacements;
   private Element elStrategyTemplate;
   private double initialCapital;
   private SettingsMap buildSettings;
   private MoneyManagementMethod moneyManagementMethod;
   private RiskManagementMethod riskManagementMethod;
   private IFitnessFunction fitnessFunction;
   private ArrayList<Condition> conditions;
   private TradingOptions tradingOptions;
   private ATM atm;
   private ATMGenerateConfig atmGenerateConfig;
   private ArrayList<ChartSetup> chartSetups;
   private OutOfSample outOfSample;
   private boolean useRobustnessTests;
   private final int tradingEngine;
   private ArrayList<ICrossCheck> crossChecks;
   private String projectName;
   private Databank databank;
   private ProjectRunInfo projectRunInfo = new ProjectRunInfo();
   private SettingsMap settings;
   private String lastSettingsXml;
   private double slippage;
   private double minDistance;
   private CommissionsMethod commission;
   private SwapMethod swap;
   private int dismissBadStrategies;
   private boolean warningsBadStrategies;
   private TaskErrorInfo taskErrorInfo = null;
   private int batchSize = 0;
   private IGPFinishedListener finishedListener;
   private int generatorHash;
   private SQProject project;
   private ReentrantLock lockCreatingBatch = new ReentrantLock();
   private int maxAllJobs;
   private Timer timer;
   private boolean finishSent = false;
   private int taskHash;
   private String strategyNamePrefix = "StrategyX";
   private FitPortfolio fitPortfolio;
   private CustomAnalysisMethod caMethod;
   private String caInputArgs;
   private boolean caFilter;

   public RandomBuildEngine(
      SettingsMap var1,
      int var2,
      String var3,
      ProgressEngine var4,
      Databank var5,
      Element var6,
      String var7,
      int var8,
      CustomAnalysisMethod var9,
      boolean var10,
      String var11
   ) throws JDOMException, IOException, Exception {
      this.settings = var1;
      this.taskSettings = var6;
      this.projectName = var3;
      this.project = ProjectEngine.get(var3);
      this.tradingEngine = var2;
      this.progressEngine = var4;
      this.databank = var5;
      this.lastSettingsXml = var7;
      this.taskHash = var8;
      this.caMethod = var9;
      this.caFilter = var10;
      this.caInputArgs = var11;
      this.rng = new MersenneTwisterRng();
      File var12 = new File(MainApp.getDataPath() + "artifical_test.txt");
      if (var12.exists()) {
         this.TEST_ARTIFICAL_BUILD = true;
      } else {
         this.TEST_ARTIFICAL_BUILD = false;
      }

      this.jobGroupID = UUID.randomUUID().toString().substring(0, 8);
      this.jobCount = 0L;
      this.createJobSettings();
      this.gridClient = SQGrid.getGridClient();
      this.gridClient.registerMessageListener(this.jobGroupID, this);
      var4.registerStatusListener(this);
      this.project.resetGeneticInfo(false, 0);
      TimerTask var13 = new TimerTask() {
         @Override
         public void run() {
            try {
               RandomBuildEngine.this.createNewBatch(RandomBuildEngine.this.batchSize);
            } catch (Exception var2x) {
               RandomBuildEngine.Log.error("Error creating batch jobs", var2x);
            }
         }
      };
      this.timer = new Timer("Timer");
      long var14 = 500L;
      long var16 = 200L;
      this.timer.scheduleAtFixedRate(var13, var14, var16);
   }

   protected void progressStatusChanged(int var1) {
      if (var1 == 4) {
         this.gridClient.stop(this.jobGroupID);
         this.gridClient.removeMessageListener(this.jobGroupID);
         this.buildFinished();

         try {
            SQProject var2 = ProjectEngine.get(this.projectName);
         } catch (Exception var4) {
            var4.printStackTrace();
         }
      } else if (var1 == 3) {
         this.gridClient.removeMessageListener(this.jobGroupID);
         this.buildFinished();

         try {
            SQProject var5 = ProjectEngine.get(this.projectName);
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }

   private void buildFinished() {
      if (this.taskErrorInfo != null) {
         File var1 = null;

         try {
            var1 = ProjectConfigHelper.getStrategyFile(this.databank, this.taskErrorInfo.strategyName, 1024, "RandomBuildEngine");
         } catch (Exception var3) {
         }

         ProjectPanel.reportBug(this.taskSettings, var1, this.taskErrorInfo.strategyName, this.taskErrorInfo.exception);
      }

      if (this.finishedListener != null) {
         this.finishedListener.finished(0);
      }
   }

   private void createJobSettings() throws JDOMException, IOException, Exception {
      Element var1 = (Element)this.settings.get("ConvertedBlocks");
      this.elReplacements = var1.getChild("Replacements");
      this.elStrategyTemplate = var1.getChild("StrategyFile");
      this.initialCapital = (Double)this.settings.get("MoneyManagement.InitialCapital");
      this.moneyManagementMethod = (MoneyManagementMethod)this.settings.get("MoneyManagement.Method");
      this.riskManagementMethod = (RiskManagementMethod)this.settings.get("RiskManagement");
      this.fitnessFunction = (IFitnessFunction)this.settings.get("FitnessFunction");
      this.conditions = (ArrayList<Condition>)this.settings.get("Conditions");
      this.tradingOptions = (TradingOptions)this.settings.get("TradingOptions");
      this.atm = (ATM)this.settings.get("ATM");
      this.atmGenerateConfig = (ATMGenerateConfig)this.settings.get("ATMGenerateConfig");
      this.buildSettings = (SettingsMap)this.settings.get("BuildSettings");
      this.outOfSample = (OutOfSample)this.settings.get("OutOfSample");
      this.chartSetups = (ArrayList<ChartSetup>)this.settings.get("ChartSetups");
      this.slippage = (Double)this.settings.get("Slippage");
      this.minDistance = (Double)this.settings.get("MinDistance");
      this.commission = (CommissionsMethod)this.settings.get("Commission");
      this.swap = (SwapMethod)this.settings.get("Swap");
      this.useRobustnessTests = (Boolean)this.settings.get("UseRobustnessTests");
      this.crossChecks = (ArrayList<ICrossCheck>)this.settings.get("CrossChecks");
      this.dismissBadStrategies = (Integer)this.settings.get("StrategyDismissSettings");
      this.warningsBadStrategies = (Boolean)this.settings.get("StrategyDismissWarnings");
      this.generatorHash = StrategyGeneratorCache.getHash(this.elStrategyTemplate, this.elReplacements, this.buildSettings, this.atmGenerateConfig);
      this.fitPortfolio = (FitPortfolio)this.settings.get("FitPortfolio");
   }

   @Override
   public void testRun() throws Exception {
      try {
         this.progressEngine.printToLog(L.t("Loading data and starting test run...", new Object[0]));
         this.progressEngine.printToLog(L.t("Test run finished ok", new Object[0]));
      } catch (Exception var2) {
         Log.error("Build task cannot start, it failed during test run!", var2);
         throw new Exception(L.t("Build task cannot start, it failed during test run!", new Object[0]) + " " + var2.getMessage());
      }
   }

   @Override
   public void start() throws Exception {
      if (this.runningStatus == 0) {
         this.runningStatus = 1;
         this.progressEngine.printToLog(L.t("Starting strategies generation...", new Object[0]));
         this.computeOptimalBatchSize();
         this.createNewBatch(this.batchSize);
      }
   }

   private void computeOptimalBatchSize() {
      if (this.tradingEngine != 1316847364 && this.tradingEngine != -1816889229) {
         this.batchSize = 15;
         this.jobsCountThreshold = SQGrid.getGridClient().getRecommendedBatchSize() * 2;
         this.maxAllJobs = SQGrid.getGridClient().getUsedComputedThreads() * 4;
      } else {
         this.batchSize = 8;
         this.jobsCountThreshold = (int)(SQGrid.getGridClient().getRecommendedBatchSize() * 1.2);
         this.maxAllJobs = SQGrid.getGridClient().getUsedComputedThreads() * 4;
      }
   }

   private void processMessage(GridMessage var1) {
      if (!this.progressEngine.isFinished()) {
         if (var1.getMessageID() == 1) {
            int var2 = 0;
            JobDetails var3 = var1.getJobDetails();
            if (var3 != null && var3.getException() != null && !this.progressEngine.isStopped() && var3.getException().contains("OutOfMemoryError")) {
               try {
                  this.gridClient.stop(this.jobGroupID);
                  this.project.onMemoryError(null);
                  return;
               } catch (Exception var8) {
                  Log.error("Cannot send Error message to grid client", var8);
               }
            }

            BacktestResult var4 = (BacktestResult)var1.getData();
            if (var4 != null) {
               var2 = this.processBuildResult(var3, var4);

               try {
                  if (var2 == 0 && var4.getResult().mainResult().containsKey("ReplacedWorseStrategy")) {
                     var2 = var4.getResult().mainResult().getInt("ReplacedWorseStrategy");
                  }
               } catch (Exception var7) {
               }

               this.project
                  .trackJob(
                     var3,
                     var2,
                     var4.getDurationStats(),
                     var4.getBacktestsInJob(),
                     this.gridClient.countRunningJobs(this.jobGroupID),
                     this.gridClient.countWaitingJobs(this.jobGroupID)
                  );
            } else {
               this.printNewStrategyToLog(var3, var3.getJobID(), var2, null, null);
               var2 = 10001;
               this.project.trackJob(var3, var2, null, 0, this.gridClient.countRunningJobs(this.jobGroupID), this.gridClient.countWaitingJobs(this.jobGroupID));
            }

            this.project.loadTrackingInfo(this.projectRunInfo, false);
            this.project.increaseGlobalStats(this.taskHash, L.t("Generated", new Object[0]), 1L, L.t("Accepted", new Object[0]), var2 == 0 ? 1L : 0L);
            if (this.project.getFailedJobsPercentage() > 0.7) {
               this.progressEngine.printToLog(L.t("GRID: Stopping build, job failures (exceptions) exceeded 70%!", new Object[0]));
               if (!this.finishSent) {
                  this.progressEngine.stop();
                  this.finishSent = true;
               }

               return;
            }

            if (BuildStopConditionsChecker.shouldStop(this.databank, this.settings, this.progressEngine, this.project.getTrackingInfo())) {
               this.gridClient.stop(this.jobGroupID);
               if (!this.finishSent) {
                  this.progressEngine.finish();
                  if (this.timer != null) {
                     this.timer.cancel();
                  }

                  this.finishSent = true;
               }

               return;
            }

            try {
               this.createNewBatch(this.batchSize);
            } catch (Exception var6) {
               Log.error("Cannot create new batch of jobs", var6);
            }
         }
      }
   }

   private int processBuildResult(JobDetails var1, BacktestResult var2) {
      int var3 = 0;
      if (SQProject.isMemoryProtectionUsed()) {
         try {
            MemoryUsageChecker.checkAvailableMemory();
         } catch (Error var6) {
            this.project.onMemoryError(var6);
            if (var2.getResult() != null) {
               var2.getResult().clear();
            }

            return 0;
         }
      }

      AllTimeStats.getInstance().increaseStrategiesGenerated();
      ResultsGroup var4 = var2.getResult();
      if (!var2.isDismissed()) {
         if (this.progressEngine.isStopped()) {
            var4.clear();
            return var3;
         }

         var4.removeUnsavableSettings();
         if (!this.warningsBadStrategies && !var4.isStockpickerStrategy()) {
            var4.specialValues().set("StrategyProblems", 0);
         }

         try {
            boolean var5 = true;
            if (this.caMethod != null) {
               this.caMethod.setInputArgs(this.caInputArgs);
               var5 = this.caMethod.filterStrategy(this.project.getName(), this.project.getActiveTaskName(), this.databank.getName(), var4);
            }

            if (!var5 && this.caFilter) {
               var4.specialValues().setString(SpecialValues.FiltersResultFailedReason, BadStrategyException.getReasonAsString(10038));
               var3 = 10038;
               this.printNewStrategyToLog(var1, var4.getName(), var3, var2.getDurationStats(), BadStrategyException.getReasonAsString(10038));
               var4.clear();
               var4 = null;
            } else if (this.databank.add(var4, true)) {
               this.printNewStrategyToLog(var1, var4.getName(), var3, var2.getDurationStats(), L.t("saved to databank", new Object[0]));
               this.databank.updateBestResults(var4);
               AllTimeStats.getInstance().increaseStrategiesAccepted();
            } else {
               this.printNewStrategyToLog(
                  var1, var4.getName(), var3, var2.getDurationStats(), L.t("dismissed, all strategies in databank are better or too similar", new Object[0])
               );
               var3 = 10006;
               var4.clear();
               var4 = null;
            }
         } catch (Exception var7) {
            Log.error("Cannot add result to databank", var7);
            if (var4 != null) {
               var4.clear();
            }
         } catch (OutOfMemoryError var8) {
            var4.clear();
            this.project.onMemoryError(var8);
         }
      } else {
         var3 = var2.getDismissalReason();
         this.printNewStrategyToLog(var1, var1.getJobID(), var3, var2.getDurationStats(), var2.getDismissalMessage());
         if (var4 != null) {
            var4.clear();
            Object var9 = null;
         }

         if (var2.getDismissalMessage().contains("OutOfMemoryError")) {
            this.project.onMemoryError(null);
         }
      }

      return var3;
   }

   private void printNewStrategyToLog(JobDetails var1, String var2, int var3, DurationStats var4, String var5) {
      if (!this.progressEngine.isStopped()) {
         HashMap var6 = null;
         if (var4 != null) {
            var6 = var4.getMap();
         }

         StringBuilder var7 = new StringBuilder(var2);
         String var8 = null;
         if (var4 != null) {
            for (String var10 : var4.getKeys()) {
               if (var5 != null && var5.contains(var10)) {
                  var8 = var10;
               } else {
                  if (var10.equals(DurationStats.MainTest)) {
                     var7.append("\n   - " + var10 + " - ");
                  } else {
                     var7.append("\n   - " + L.t("Cross check", new Object[0]) + " - " + var10 + " - ");
                  }

                  if (var6 != null) {
                     var7.append(L.t("OK in %s s.", new Object[]{String.format("%.2f", var6.get(var10))}));
                  } else {
                     var7.append(L.t("ok", new Object[0]));
                  }
               }
            }
         }

         if (var5 != null) {
            if (var5.contains(var2 + ", ")) {
               var5 = var5.replaceAll(var2 + ", ", "").trim();
            }

            var7.append("\n   - " + var5);
            if (var8 != null) {
               if (!var5.contains("dismissed")
                  && !var5.contains("exception")
                  && !var5.contains(L.t("dismissed", new Object[0]))
                  && !var5.contains(L.t("exception", new Object[0]))) {
                  var7.append(" " + L.t("ok", new Object[0]));
               }

               if (var6 != null) {
                  var7.append(L.t(" in %s s.", new Object[]{String.format("%.2f", var6.get(var8))}));
               }
            }
         }

         String var11 = var1.getException();
         if (var11 != null) {
            var7.append(var1.getJobID());
            if (this.taskErrorInfo == null) {
               this.taskErrorInfo = new TaskErrorInfo(var2, var11);
            }

            var7.append(" - " + L.t("Error", new Object[0]) + ": ");
            var7.append(var11.length() > 500 ? var11.substring(0, 500) : var11);
         }

         if (var6 == null) {
            var7.append(L.t(" finished in %s s.", new Object[]{String.format("%.2f", var1.getDuration() / 1000.0)}));
         }

         this.progressEngine.printToLogDebug(var7.toString());
      }
   }

   private void createNewBatch(int var1) throws Exception {
      if (var1 != 0) {
         if (this.progressEngine.checkRunning()) {
            boolean var2 = this.lockCreatingBatch.tryLock();
            if (var2) {
               try {
                  this.progressEngine.checkPaused();
                  long var3 = this.gridClient.countAllJobs(this.jobGroupID);
                  long var5 = this.gridClient.countAllJobs(null);
                  if (var3 < this.jobsCountThreshold && (var3 == 0L || var5 < this.maxAllJobs) && !this.progressEngine.isStopped()) {
                     ArrayList var7 = new ArrayList();

                     for (int var8 = 0; var8 < var1; var8++) {
                        String var9;
                        if (this.strategyNamePrefix != null) {
                           var9 = String.format("%s0.%d", this.strategyNamePrefix, this.jobCount++);
                        } else {
                           var9 = String.format("Strategy 0.%d", this.jobCount++);
                        }

                        if (this.TEST_ARTIFICAL_BUILD) {
                           var7.add(new ArtificalBuilderJob(var9, this.getJobParams(var9), this));
                        } else {
                           var7.add(new BuilderJob(var9, this.getJobParams(var9), this, this.lastSettingsXml));
                        }
                     }

                     Log.debug("Created {} evaluation jobs", var7.size());
                     this.progressEngine.printToLogDebug(L.t("Created new batch of strategies, evaluating them...", new Object[0]));
                     this.gridClient.executeOnGrid(this.jobGroupID, var7);
                  }
               } finally {
                  this.lockCreatingBatch.unlock();
               }
            }
         }
      }
   }

   private Map<String, Serializable> getJobParams(String var1) throws Exception {
      HashMap var2 = new HashMap();
      var2.put("StrategyName", var1);
      var2.put("Replacements", this.elReplacements);
      var2.put("StrategyTemplate", this.elStrategyTemplate);
      var2.put("GeneratorHash", this.generatorHash);
      var2.put("MoneyManagement.InitialCapital", this.initialCapital);
      var2.put("RandomSeed", (long)this.rng.nextInt(Integer.MAX_VALUE));
      var2.put("MoneyManagement.Method", this.moneyManagementMethod);
      var2.put("RiskManagement", this.riskManagementMethod);
      var2.put("FitnessFunction", this.fitnessFunction);
      var2.put("TradingOptions", this.tradingOptions.getClone());
      var2.put("ATM", this.atm == null ? null : this.atm.getClone());
      var2.put("ATMGenerateConfig", this.atmGenerateConfig);
      var2.put("Conditions", this.conditions);
      var2.put("DismissBadStrategies", this.dismissBadStrategies);
      var2.put("StrategyDismissWarnings", this.warningsBadStrategies);
      var2.put("BuildSettings", this.buildSettings.clone());
      var2.put("OutOfSample", this.outOfSample);
      var2.put("ChartSetups", this.chartSetups);
      var2.put("UseRobustnessTests", this.useRobustnessTests);
      if (this.useRobustnessTests) {
         var2.put("CrossChecks", ProjectConfigHelper.cloneCrossChecks(this.crossChecks));
      } else {
         var2.put("CrossChecks", this.crossChecks);
      }

      var2.put("Slippage", this.slippage);
      var2.put("MinDistance", this.minDistance);
      var2.put("Commission", this.commission);
      var2.put("Swap", this.swap);
      var2.put("FitPortfolio", this.fitPortfolio == null ? null : this.fitPortfolio.getClone());
      return var2;
   }

   @Override
   public void setFinishListener(IGPFinishedListener var1) {
      this.finishedListener = var1;
   }

   public void onStatusChanged(int var1) {
      this.progressStatusChanged(var1);
   }

   public void messageReceived(GridMessage var1) {
      this.processMessage(var1);
   }

   @Override
   public void destroy() {
      this.gridClient.removeMessageListener(this.jobGroupID);
      this.progressEngine.unregisterStatusListener(this);
      this.timer.cancel();
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
