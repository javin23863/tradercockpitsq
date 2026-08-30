package com.strategyquant.plugin.Task.impl.AutomaticRetest;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.RunningStatus;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.pp.ProjectPanel;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtest.AutoRetestStrategyConfigModifier;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisInfo;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.exception.TaskErrorInfo;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.IProgressStatusListener;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.StrategyMerger;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.SettingError;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "AutomaticRetestTask plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Automatic Retester")
@PluginImplementation
public class AutomaticRetestTask extends AbstractTask implements ILastEventListener, IProgressStatusListener {
   public static final Logger Log = LoggerFactory.getLogger(AutomaticRetestTask.class);
   private static final String LOCK_AUTORETEST = "AutomaticRetestTask";
   private ProjectRunInfo projectRunInfo = new ProjectRunInfo();
   private Databank inputDatabank;
   private Databank outputDatabank;
   private ArrayList<String> databankRecordKeys;
   private int lastProcessedIndex = 0;
   private AtomicInteger lastFinishedIndex = new AtomicInteger(0);
   private GridClient gridClient;
   private String jobGroupID;
   private long jobCount;
   private TaskErrorInfo taskErrorInfo = null;
   private int retestBatchSize;
   private int originalRetestBatchSize;
   private boolean singleThreadedOptimizations = true;
   private long projectStartTime;
   private boolean deleteFailedStrategies = false;
   private boolean forceRunCrossChecks = false;
   private Timer jobsCreationTimer;
   private TaskSettingsData strategyData;
   private ArrayList<ISettingTabPlugin> settingsPlugins;
   private boolean useCrossChecks = false;
   private boolean evaluateAllCrossChecks = false;
   private ArrayList<ICrossCheck> crossChecks;
   private int backtestMode = 1;
   private boolean createsSubJobs = false;
   private boolean warningsBadStrategies;
   private CustomAnalysisMethod caMethod;
   private String caInputArgs;
   private boolean caFilter;
   private int combinations;

   public AutomaticRetestTask() throws Exception {
      this(null, null);
   }

   public AutomaticRetestTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
      this.strategyData = new TaskSettingsData();
      this.settingsPlugins = SQPluginManager.getPlugins(ISettingTabPlugin.class);
   }

   public boolean beforeStart() throws Exception {
      if (!RunningStatus.canBeStarted(this.getRunningStatus())) {
         Log.error("Cannot start project in incorrect running status: " + this.getRunningStatus());
         return false;
      }

      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog("================================");
      this.progressEngine.printToLog(L.t("Starting strategies retesting...", new Object[0]));
      this.progressEngine.start();
      this.lastProcessedIndex = 0;
      this.lastFinishedIndex.set(0);
      this.projectStartTime = System.currentTimeMillis();
      this.project = ProjectEngine.get(this.projectName);
      this.project.resetTracking(false, false);
      this.project.updateTrackingInfo(this.projectRunInfo);
      this.taskErrorInfo = null;
      this.outputDatabank = (Databank)this.settingsData.getParams().get("DatabankTarget");
      this.inputDatabank = (Databank)this.settingsData.getParams().get("DatabankSource");
      this.databankRecordKeys = this.inputDatabank.getRecordKeys();
      if (this.databankRecordKeys.size() == 0) {
         this.progressEngine.printToLog(L.t("No strategies in databank, nothing to retest!", new Object[0]));
         return false;
      }

      this.setDatabankFilter(this.outputDatabank, false);
      if (this.inputDatabank.getName().equals(this.outputDatabank.getName())) {
         this.outputDatabank.clearResultsData();
      }

      this.deleteFailedStrategies = SettingsMap.getBool(this.settingsData.getParams().get("DeleteFailedStrategies"), false);
      this.forceRunCrossChecks = SettingsMap.getBool(this.settingsData.getParams().get("ForceRunCrossChecks"), false);
      this.initParams();
      this.recognizeCustomAnalysisMethod();
      this.recognizeBacktestMode();
      this.computeOptimalBatchSize();
      return true;
   }

   private void recognizeCustomAnalysisMethod() {
      CustomAnalysisInfo var1 = (CustomAnalysisInfo)this.settingsData.getParams().get("CustomAnalysis");
      if (var1 != null) {
         this.caMethod = var1.method;
         this.caFilter = var1.filter;
         this.caInputArgs = var1.inputArgs;
      }
   }

   private void initParams() throws JDOMException, IOException, Exception {
      this.useCrossChecks = SettingsMap.getBool(this.settingsData.getParams().get("UseRobustnessTests"), false);
      this.crossChecks = (ArrayList<ICrossCheck>)this.settingsData.getParams().get("CrossChecks");
      this.warningsBadStrategies = (Boolean)((Serializable)this.settingsData.getParams().get("StrategyDismissWarnings"));
      this.evaluateAllCrossChecks = SettingsMap.getBool(this.settingsData.getParams().get("CrossChecks.EvaluateAll"), false);
      this.deleteFailedStrategies = SettingsMap.getBool(this.settingsData.getParams().get("DeleteFailedStrategies"), false);
      this.forceRunCrossChecks = SettingsMap.getBool(this.settingsData.getParams().get("ForceRunCrossChecks"), false);
   }

   private void recognizeBacktestMode() {
      this.backtestMode = 1;
      this.createsSubJobs = false;
      if (this.useCrossChecks) {
         for (ICrossCheck var2 : this.crossChecks) {
            if (var2.doesCreateSubjobs()) {
               this.createsSubJobs = true;
               this.backtestMode = 0;
               break;
            }
         }
      }
   }

   private void computeOptimalBatchSize() throws Exception {
      this.retestBatchSize = 5;
      this.singleThreadedOptimizations = true;
      int var1 = SQGrid.getGridClient().getUsedComputedThreads();
      int var2 = this.inputDatabank.size();
      XmlChartCombinator var3 = new XmlChartCombinator();
      List var4 = var3.getSettingXMLVariations(this.getTaskElement(), null);
      this.combinations = var4.size();
      var2 *= this.combinations;
      if (var2 <= var1 / 2) {
         this.singleThreadedOptimizations = false;
      }

      Log.info(
         "Batch size computed to: {}, SingleOptims: {}, str to test: {}, totalCores: {}, Backtest mode: {}",
         new Object[]{this.retestBatchSize, this.singleThreadedOptimizations, var2, var1, this.backtestMode}
      );
      this.originalRetestBatchSize = this.retestBatchSize;
   }

   public void start() throws Exception {
      if (!this.beforeStart()) {
         this.progressEngine.finish();
      } else {
         if (this.progressEngine != null) {
            this.jobGroupID = UUID.randomUUID().toString();
            this.jobCount = 0L;
            this.gridClient = SQGrid.getGridClient();
            this.gridClient.registerMessageListener(this.jobGroupID, new IGridMessageListener() {
               public void messageReceived(GridMessage var1) {
                  AutomaticRetestTask.this.processMessage(var1);
               }
            });
            this.progressEngine.registerStatusListener(this);
         }

         if (this.backtestMode == 1) {
            this.createNewBatch(this.retestBatchSize);
            TimerTask var1 = new TimerTask() {
               @Override
               public void run() {
                  try {
                     AutomaticRetestTask.this.createNewBatch(AutomaticRetestTask.this.retestBatchSize);
                     AutomaticRetestTask.this.checkAllFinished();
                  } catch (Exception var2) {
                     var2.printStackTrace();
                     AutomaticRetestTask.Log.error("Timer error", var2);
                  }
               }
            };
            this.jobsCreationTimer = new Timer("RetesterTimer");
            long var2 = 200L;
            long var4 = 300L;
            this.jobsCreationTimer.scheduleAtFixedRate(var1, var2, var4);
         } else {
            this.createSerialJobs();
         }
      }
   }

   protected void checkAllFinished() {
      if (this.progressEngine.isFinished()) {
         if (this.jobsCreationTimer != null) {
            this.jobsCreationTimer.cancel();
            this.jobsCreationTimer = null;
         }
      } else {
         int var1 = this.lastFinishedIndex.get();
         if (var1 >= this.databankRecordKeys.size() * this.combinations
            && this.gridClient.countRunningJobs(this.jobGroupID) <= 1L
            && this.gridClient.countWaitingJobs(this.jobGroupID) == 0L) {
            if (this.jobsCreationTimer != null) {
               this.jobsCreationTimer.cancel();
               this.jobsCreationTimer = null;
            }

            this.gridClient.stop(this.jobGroupID);
            this.progressEngine.finish(System.currentTimeMillis() - this.projectStartTime);
            this.outputDatabank.updateBestResults();
         }
      }
   }

   private synchronized void submitNextJob() throws Exception {
      this.createNewBatch(1);
   }

   private synchronized void createSerialJobs() throws Exception {
      this.submitNextJob();
      long var1 = SQGrid.getGridClient().countRunningJobs(null);
      long var3 = SQGrid.getGridClient().countWaitingJobs(null);
      int var5 = SQGrid.getGridClient().getUsedComputedThreads();
      long var6 = var5 - var1 - var3;
      if (var6 > 8L) {
         this.submitNextJob();
      }

      if (!this.createsSubJobs) {
         if (var5 > 8) {
            this.submitNextJob();
         }

         if (var5 > 16) {
            this.submitNextJob();
         }

         if (var5 > 24) {
            this.submitNextJob();
         }

         if (var5 > 32) {
            this.submitNextJob();
         }

         if (var5 > 40) {
            this.submitNextJob();
         }

         if (var5 >= 47) {
            this.submitNextJob();
         }
      }
   }

   private synchronized boolean createNewBatch(int var1) throws Exception {
      this.progressEngine.checkPaused();
      if (this.progressEngine.isStopped()) {
         return false;
      }

      long var2 = SQGrid.getGridClient().countRunningJobs(this.jobGroupID);
      long var4 = SQGrid.getGridClient().countWaitingJobs(this.jobGroupID);
      if (var1 != 1) {
         var1 = this.retestBatchSize;
         if (var4 > var1) {
            int var20 = (int)Math.round(this.retestBatchSize * 0.8);
            int var21 = (int)Math.round(var1 * 1.2);
            if (var4 > var21 && var20 > this.originalRetestBatchSize / 3 && var20 > 1) {
               this.retestBatchSize = var20;
            }

            return true;
         }

         if (var4 == 0L) {
            int var6 = (int)Math.round(this.retestBatchSize * 1.3);
            if (var6 <= this.originalRetestBatchSize * 10) {
               this.retestBatchSize = var6;
            }

            var1 = this.retestBatchSize;
         }
      }

      this.progressEngine.checkPaused();
      ArrayList var19 = new ArrayList();

      for (int var7 = 0; var7 < var1 && this.lastProcessedIndex < this.databankRecordKeys.size(); var7++) {
         if (this.progressEngine.isStopped()) {
            return false;
         }

         String var8 = this.databankRecordKeys.get(this.lastProcessedIndex);
         this.lastProcessedIndex++;

         try {
            ResultsGroup var9 = this.inputDatabank.getLocked(var8, "AutomaticRetestTask");

            try {
               List var10 = this.getParams(var9);
               if (var10 != null) {
                  for (Map var12 : var10) {
                     String var13 = var9.getName() + this.jobCount++;
                     var19.add(new AutomaticRetestJob(var13, var12, this.progressEngine, this));
                  }
               }
            } finally {
               var9.releaseLock("AutomaticRetestTask");
            }
         } catch (RecordNotFoundException var18) {
         }
      }

      if (var19.size() > 0) {
         Log.debug("Created {} evaluation jobs", var19.size());
         this.gridClient.executeOnGrid(this.jobGroupID, var19);
         return true;
      } else {
         Log.debug("No more jobs to create");
         return false;
      }
   }

   private List<Map<String, Serializable>> getParams(ResultsGroup var1) throws Exception {
      ArrayList var2 = new ArrayList();
      XmlChartCombinator var3 = new XmlChartCombinator();
      Element var4 = XMLUtil.stringToElement(var1.getLastSettings());

      for (Element var7 : var3.getSettingXMLVariations(this.getTaskElement(), var4)) {
         HashMap var8 = new HashMap();
         AutoRetestStrategyConfigModifier.modifySettings(var4, var7);
         this.strategyData.clear();

         for (int var9 = 0; var9 < this.settingsPlugins.size(); var9++) {
            ISettingTabPlugin var10 = this.settingsPlugins.get(var9);
            if (this.shouldReadSettings(var10.getSettingName())) {
               var10.readSettings(this.projectName, this, var4, this.strategyData);
               if (this.strategyData.hasErrors()) {
                  SettingError var11 = (SettingError)this.strategyData.getErrors().get(0);
                  this.progressEngine
                     .printToLog(L.t("Strategy %s skipped. %s setting error - %s", new Object[]{var1.getName(), var11.settingName, var11.errorMsg}));
                  return null;
               }
            }
         }

         var8.putAll(this.strategyData.getParams());
         var8.put("StrategyXml", var1.getStrategyXml());
         var8.put("StrategyName", var1.getName());
         var8.put("StrategyLastSettings", var4);
         var8.put("IsRetester", true);
         var8.put("DatabankSource", this.inputDatabank);
         var8.put("DatabankTarget", this.outputDatabank);
         var8.put("StrategyDismissWarnings", this.warningsBadStrategies);
         if (var1.specialValues().containsKey(SpecialValues.IsMergedPortfolio)) {
            var8.put("PortfolioMergedStrategies", (ArrayList)StrategyMerger.getSplitResults(var1));
         }

         if (this.backtestMode == 0) {
            var8.put("SingleThreadedOptimizations", false);
         } else {
            var8.put("SingleThreadedOptimizations", this.singleThreadedOptimizations);
         }

         ArrayList var12;
         if (!this.useCrossChecks) {
            var12 = this.crossChecks;
         } else {
            var12 = ProjectConfigHelper.cloneCrossChecks((ArrayList)var8.get("CrossChecks"));
         }

         var8.put("CrossChecks", var12);
         var8.put("DateGenerated", var1.specialValues().getLong(SpecialValues.DateGenerated, -1L));
         var2.add(var8);
      }

      return var2;
   }

   private boolean shouldReadSettings(String var1) {
      switch (var1) {
         case "Data":
         case "RiskMoneyManagement":
         case "Options":
         case "Rankings":
         case "CrossChecks":
         case "ATMs":
            return true;
         default:
            return false;
      }
   }

   private void retestFinished() {
      if (this.taskErrorInfo != null) {
         File var1 = null;

         try {
            var1 = ProjectConfigHelper.getStrategyFile(this.outputDatabank, this.taskErrorInfo.strategyName, 1024, "AutomaticRetestTask");
         } catch (Exception var3) {
         }

         ProjectPanel.reportBug(this.getTaskElement(), var1, this.taskErrorInfo.strategyName, this.taskErrorInfo.exception);
      }

      this.progressEngine.unregisterStatusListener(this);
   }

   protected void processMessage(GridMessage var1) {
      try {
         if (SQProject.isMemoryProtectionUsed()) {
            MemoryUsageChecker.checkAvailableMemory();
         }

         if (var1.getMessageID() == 1) {
            try {
               int var2 = 0;
               int var3 = 0;
               boolean var4 = false;
               JobDetails var5 = var1.getJobDetails();
               DurationStats var6 = null;
               if (!var5.isSuccess()) {
                  this.printNewStrategyToLog(var5, var5.getJobID(), null, var6);
                  var2 = 10001;
               } else {
                  BacktestResult var7 = (BacktestResult)var1.getData();
                  var6 = var7.getDurationStats();
                  var3 = var7.getBacktestsInJob();
                  ResultsGroup var8 = var7.getResult();
                  if (var8 != null) {
                     String var18 = var8.getName();
                     String var10 = null;
                     var2 = var7.getDismissalReason();
                     if (var7.isDismissed()) {
                        if (var7.getDismissalMessage() != null) {
                           this.progressEngine.printToLogDebug(var7.getDismissalMessage());
                        }

                        if (this.deleteFailedStrategies
                           && !SpecialValues.FiltersResultPassed.equals(var8.specialValues().getString(SpecialValues.FiltersResultFailedReason))) {
                           String var11 = var8.specialValues().getString(SpecialValues.FiltersResultFailedReason);
                           if (var11 == null) {
                              var11 = "N/A";
                           }

                           var10 = L.t("deleted from databank, failed: %s", new Object[]{var11});
                           if (this.inputDatabank == this.outputDatabank) {
                              this.inputDatabank.remove(var8.getName(), true, true, true, true, "AutomaticRetestTask");
                           }
                        }
                     } else {
                        if (!this.warningsBadStrategies && !var8.isStockpickerStrategy()) {
                           var8.specialValues().set("StrategyProblems", 0);
                        }

                        var4 = true;
                     }

                     if (var10 == null) {
                        boolean var20 = true;
                        boolean var12 = false;
                        if (this.caMethod != null) {
                           this.caMethod.setInputArgs(this.caInputArgs);
                           var20 = this.caMethod.filterStrategy(this.project.getName(), this.project.getActiveTaskName(), this.outputDatabank.getName(), var8);
                           if (!var20 && this.caFilter) {
                              if (this.deleteFailedStrategies
                                 && !SpecialValues.FiltersResultPassed.equals(var8.specialValues().getString(SpecialValues.FiltersResultFailedReason))) {
                                 var10 = L.t("deleted from databank, failed: Custom analysis", new Object[0]);
                                 if (this.inputDatabank == this.outputDatabank) {
                                    this.inputDatabank.remove(var8.getName(), true, true, true, true, "AutomaticRetestTask");
                                    var12 = true;
                                 }
                              } else {
                                 var8.specialValues().setString(SpecialValues.FiltersResultFailedReason, BadStrategyException.getReasonAsString(10038));
                                 var2 = 10038;
                                 var7.dismiss("Failed: Custom analysis", var2);
                              }
                           }
                        }

                        if (!var12) {
                           if (!var4 || !var20 && this.caFilter) {
                              this.project.increaseStrategiesFailed();
                           } else {
                              this.project.increaseStrategiesPassed();
                           }

                           var8.removeUnsavableSettings();
                           var8.setDatabank(this.outputDatabank);
                           if (this.inputDatabank == this.outputDatabank) {
                              this.outputDatabank.update(var8.getName(), var8, true, "AutomaticRetestTask");
                           } else {
                              this.outputDatabank.add(var8, true);
                           }

                           this.outputDatabank.updateBestResults(var8);
                        }
                     }

                     this.printNewStrategyToLog(var5, var18, var10, var6);
                  } else {
                     String var9;
                     if (var7.isDismissed()) {
                        this.project.increaseStrategiesFailed();
                        var9 = var7.getDismissalMessage();
                     } else {
                        var9 = "Result is null!";
                     }

                     this.printNewStrategyToLog(var5, var5.getJobID(), var9, var6);
                  }
               }

               long var17 = this.gridClient.countRunningJobs(this.jobGroupID);
               this.project.trackJob(var5, var2, var6, var3, var17, this.gridClient.countWaitingJobs(this.jobGroupID));
               this.project.increaseTrackingInfoStep(this.databankRecordKeys.size());
               this.project.loadTrackingInfo(this.projectRunInfo, false);
               ProjectRunInfo var19 = this.project.getTrackingInfo();
               var19.infiniteProgress = false;
               var19.timeToFinish = (this.databankRecordKeys.size() - this.projectRunInfo.totalJobsDone) * this.projectRunInfo.timePerStrategy;
               var19.progressPercent = 100.0F * (float)this.projectRunInfo.totalJobsDone / this.databankRecordKeys.size();
               this.project
                  .increaseGlobalStats(this.getCustomName().hashCode(), L.t("Retested", new Object[0]), 1L, L.t("Passed", new Object[0]), var4 ? 1L : 0L);
               if (this.project.getFailedJobsPercentage() > 0.7) {
                  this.progressEngine.printToLog(L.t("GRID: Stopping retest, job failures (exceptions) exceeded 70%!", new Object[0]));
                  this.progressEngine.stop();
               }
            } catch (Exception var14) {
               Log.error("Error while processing strategy result", var14);
               this.progressEngine.printToLog(L.t("GRID: Error while processing strategy result - %s", new Object[]{var14.getMessage()}));
            }

            if (this.backtestMode == 0) {
               try {
                  this.createSerialJobs();
               } catch (Exception var13) {
                  Log.error("Error while creating batch of jobs", var13);
                  this.progressEngine.printToLog(L.t("GRID: Error while creating batch of jobs - %s", new Object[]{var13.getMessage()}));
                  this.progressEngine.stop();
               }
            }

            int var16 = this.lastFinishedIndex.incrementAndGet();
            this.checkAllFinished();
         }
      } catch (OutOfMemoryError var15) {
         this.gridClient.stop(this.jobGroupID);
         this.project.onMemoryError(var15);
      }
   }

   private void printNewStrategyToLog(JobDetails var1, String var2, String var3, DurationStats var4) {
      StringBuffer var5 = new StringBuffer("Tested ");
      var5.append(var2);
      if (var4 != null) {
         HashMap var6 = var4.getMap();

         for (String var8 : var4.getKeys()) {
            if (var8.equals(DurationStats.MainTest)) {
               var5.append("\n   - " + var8 + " - ");
            } else {
               var5.append("\n   - " + L.t("Cross check", new Object[0]) + " - " + var8 + " - ");
            }

            var5.append(L.t("OK in %s s.", new Object[]{String.format("%.2f", var6.get(var8))}));
         }
      }

      if (var3 != null) {
         var5.append("\n   - " + var3);
      }

      this.progressEngine.printToLogDebug(var5.toString());
   }

   public String getType() {
      return "AutomaticRetest";
   }

   public String getPluginFolderName() {
      return "TaskAutomaticRetest";
   }

   public String getName() {
      return L.tsq("Automatic retest");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      AutomaticRetestTask var3 = new AutomaticRetestTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public int getPreferredPosition() {
      return 1;
   }

   public String[] getSettings() {
      return new String[]{"Databanks", "AutoRetestData", "RiskMoneyManagement", "Options", "CrossChecks", "Rankings", "ATMs"};
   }

   protected int getRunningStatus() {
      return this.progressEngine.getRunningStatus();
   }

   protected Databank[] getUsedDatabanks() {
      return new Databank[]{this.inputDatabank, this.outputDatabank};
   }

   protected Databank getOutputDatabank() {
      return this.outputDatabank;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      super.logTaskFinished(var1);
      ProjectRunInfo var2 = this.project.getTrackingInfo();
      String var3 = L.t("Total", new Object[0])
         + ": "
         + var2.backtestsPerformed
         + ", "
         + L.t("Time per strategy", new Object[0])
         + ": "
         + SQUtils.formatDuration(var2.timePerStrategy)
         + ", "
         + L.t("Passed", new Object[0])
         + ": "
         + var2.strategiesPassed
         + ", "
         + L.t("Failed", new Object[0])
         + ": "
         + var2.strategiesFailed;
      var1.print(var3);
      var1.addDurationStats(var2);
      var1.addDismissalStats(var2, this);
      var1.addAcceptedStats(var2, this);
   }

   public void setLastEvent(String var1) {
      if (this.project != null) {
         this.project.setLastEvent(var1);
      }
   }

   public void onStatusChanged(int var1) {
      this.progressStatusChanged(var1);
   }

   protected void progressStatusChanged(int var1) {
      if (var1 == 4) {
         this.gridClient.stop(this.jobGroupID);
         this.progressEngine.unregisterStatusListener(this);
         if (this.jobsCreationTimer != null) {
            this.jobsCreationTimer.cancel();
            this.jobsCreationTimer = null;
         }
      } else if (var1 == 3) {
         this.retestFinished();
      }
   }
}
