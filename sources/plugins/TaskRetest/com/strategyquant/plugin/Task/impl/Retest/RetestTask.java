package com.strategyquant.plugin.Task.impl.Retest;

import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.dataseries.DoubleListCache;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.RunningStatus;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.pp.ProjectPanel;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.backtest.BacktestDataInitializer;
import com.strategyquant.tradinglib.backtest.LoadDataProgressEngine;
import com.strategyquant.tradinglib.backtestrunner.BacktestResult;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisInfo;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.exception.TaskErrorInfo;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.IProgressStatusListener;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.StrategyMerger;
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

@Author(name = "Tamas Takacs")
@Name(name = "RetestTask plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class RetestTask extends AbstractTask implements IProgressStatusListener, ILastEventListener {
   public static final Logger Log = LoggerFactory.getLogger(RetestTask.class);
   private static final String LOCK_RETEST_TASK = "RetestTask";
   private ProjectRunInfo projectRunInfo = new ProjectRunInfo();
   private Databank inputDatabank;
   private Databank outputDatabank;
   private ArrayList<String> databankRecordKeys;
   private int lastProcessedIndex = 0;
   private AtomicInteger lastFinishedIndex = new AtomicInteger(0);
   private GridClient gridClient;
   private String jobGroupID;
   private long jobCount;
   private double slippage;
   private double minDistance;
   private CommissionsMethod commission;
   private SwapMethod swap;
   private int dismissBadStrategies;
   private boolean warningsBadStrategies;
   private TaskErrorInfo taskErrorInfo = null;
   private TradingOptions tradingOptions;
   private ATM atm;
   private ArrayList<Condition> conditions;
   private int retestBatchSize;
   private int originalRetestBatchSize;
   private long projectStartTime;
   private boolean useCrossChecks = false;
   private boolean evaluateAllCrossChecks = false;
   private ArrayList<ICrossCheck> crossChecks;
   private boolean deleteFailedStrategies = false;
   private boolean forceRunCrossChecks = false;
   private boolean singleThreadedOptimizations = true;
   private String lastSettingsXml;
   private Timer jobsCreationTimer;
   private int backtestMode = 1;
   private boolean createsSubJobs = false;
   private CustomAnalysisMethod caMethod;
   private String caInputArgs;
   private boolean caFilter;
   private String currentBasketDatabankRecordKey = null;
   private int currentBasketStockIndex = 0;
   private List<StockDto> currentBasketStocks = null;

   public RetestTask() throws Exception {
      this(null, null);
   }

   public RetestTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public boolean beforeStart() throws Exception {
      try {
         if (!RunningStatus.canBeStarted(this.getRunningStatus())) {
            Log.error("Cannot start project in incorrect running status: " + this.getRunningStatus());
            return false;
         }

         DoubleListCache.resetStats();
         this.progressEngine.setLogPrefix(this.taskLogPrefix);
         this.progressEngine.printToLog("================================");
         this.progressEngine.printToLog(L.t("Starting strategies retesting...", new Object[0]));
         this.progressEngine.start();
         this.project = ProjectEngine.get(this.projectName);
         this.project.resetTracking(false, false);
         this.projectRunInfo.reset(false, false);
         this.taskErrorInfo = null;
         this.inputDatabank = (Databank)this.settingsData.getParams().get("DatabankSource");
         this.outputDatabank = (Databank)this.settingsData.getParams().get("DatabankTarget");
         this.loadRecordKeys();
         if (this.databankRecordKeys.size() == 0) {
            this.progressEngine.printToLog(L.t("No strategies to retest", new Object[0]));
            return false;
         }

         ChartSetups var1 = (ChartSetups)this.settingsData.getParams().get("ChartSetups");
         ProjectConfigHelper.checkAdditionalChartsAndCIndysExpected(this.projectName, this.inputDatabank, var1.getMainSetup(), this.databankRecordKeys);
         ProjectConfigHelper.checkVolumeProfileBlocksAllowed(this.inputDatabank, this.databankRecordKeys);
         if (this.inputDatabank == this.outputDatabank) {
            this.progressEngine.printToLog(L.t("Clearing results data", new Object[0]));
            if (this.settingsData.getParams().containsKey("StrategiesToRetest")) {
               this.outputDatabank.clearResultsData(this.databankRecordKeys);
            } else {
               this.outputDatabank.clearResultsData();
            }

            this.progressEngine.printToLog(L.t("Results data cleared", new Object[0]));
         }

         this.lastProcessedIndex = 0;
         this.lastFinishedIndex.set(0);
         this.projectStartTime = System.currentTimeMillis();
         ProjectResources.addResources(this);
         Element var2 = this.getTaskElement();
         this.lastSettingsXml = XMLUtil.elementToString(var2);
         this.project.updateTrackingInfo(this.projectRunInfo);
         this.initParams();
         this.recognizeCustomAnalysisMethod();
         this.recognizeBacktestMode();
         this.computeOptimalBatchSize();
         this.initializeBacktestData();
         if (this.progressEngine.isStopped()) {
            this.progressEngine.printToLog(L.t("Project stopped", new Object[0]));
            return false;
         } else {
            return true;
         }
      } catch (TaskStoppedException var3) {
         return false;
      }
   }

   private void recognizeCustomAnalysisMethod() {
      CustomAnalysisInfo var1 = (CustomAnalysisInfo)this.settingsData.getParams().get("CustomAnalysis");
      if (var1 != null) {
         this.caMethod = var1.method;
         this.caFilter = var1.filter;
         this.caInputArgs = var1.inputArgs;
      }
   }

   private void loadRecordKeys() {
      if (this.settingsData.getParams().containsKey("StrategiesToRetest")) {
         this.databankRecordKeys = (ArrayList<String>)this.settingsData.getParams().get("StrategiesToRetest");
      } else {
         this.databankRecordKeys = this.inputDatabank.getRecordKeys();
      }
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

   private void initializeBacktestData() throws Exception {
      ArrayList var1 = (ArrayList)this.settingsData.getParams().get("ChartSetups");
      BacktestDataInitializer var2 = new BacktestDataInitializer(new LoadDataProgressEngine(this.progressEngine), var1, this.useCrossChecks, this.crossChecks);
      var2.prepareData();
   }

   private void computeOptimalBatchSize() {
      this.retestBatchSize = 5;
      this.singleThreadedOptimizations = true;
      int var1 = SQGrid.getGridClient().getUsedComputedThreads();
      int var2 = this.inputDatabank.size();
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
                  RetestTask.this.processMessage(var1);
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
                     RetestTask.this.createNewBatch(RetestTask.this.retestBatchSize);
                     RetestTask.this.checkAllFinished();
                  } catch (Exception var2) {
                     var2.printStackTrace();
                     RetestTask.Log.error("Timer error", var2);
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

   private synchronized void submitNextJob() throws Exception {
      this.createNewBatch(1);
   }

   private synchronized boolean createNewBatch(int var1) throws Exception {
      long var2 = SQGrid.getGridClient().countRunningJobs(this.jobGroupID);
      long var4 = SQGrid.getGridClient().countWaitingJobs(this.jobGroupID);
      if (var1 != 1) {
         var1 = this.retestBatchSize;
         if (var4 > var1) {
            int var31 = (int)Math.round(this.retestBatchSize * 0.8);
            int var33 = (int)Math.round(var1 * 1.2);
            if (var4 > var33 && var31 > this.originalRetestBatchSize / 3 && var31 > 1) {
               this.retestBatchSize = var31;
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
      ArrayList var30 = new ArrayList();
      if (this.currentBasketDatabankRecordKey != null && this.currentBasketStocks != null && this.currentBasketStockIndex < this.currentBasketStocks.size()) {
         try {
            ResultsGroup var7 = this.inputDatabank.getLocked(this.currentBasketDatabankRecordKey, "RetestTask");

            try {
               RetestJob var8 = this.createJobForStock(var7, this.currentBasketStocks.get(this.currentBasketStockIndex));
               if (var8 != null) {
                  var30.add(var8);
               }
            } finally {
               if (var7 != null) {
                  var7.releaseLock("RetestTask");
               }
            }

            this.updateBasketIndex(this.currentBasketStockIndex + 1);
         } catch (RecordNotFoundException var29) {
            this.updateBasketIndex(this.currentBasketStockIndex + 1);
         }
      }

      if (var30.isEmpty()) {
         for (int var32 = 0; var32 < var1 && this.lastProcessedIndex < this.databankRecordKeys.size(); var32++) {
            if (this.progressEngine.isStopped()) {
               return false;
            }

            String var34 = this.databankRecordKeys.get(this.lastProcessedIndex);
            this.lastProcessedIndex++;

            try {
               ResultsGroup var9 = this.inputDatabank.getLocked(var34, "RetestTask");

               try {
                  ChartSetups var11 = (ChartSetups)this.settingsData.getParams().get("ChartSetups");
                  int var12 = var11.getMainSetup().getBacktestEngine();
                  String var13 = var11.getMainSetup().getSymbol();
                  if (var12 == -1816889229 && DataManager.isGroupAlias(var13)) {
                     BasketDto var35 = BasketOfStocksManager.getInstance().getBasket(var13);
                     if (var35 == null) {
                        throw new Exception(L.t("Group with name '{0}' not found.", new Object[]{var13}));
                     }

                     List var15 = BasketOfStocksManager.getInstance().getStocks(var35.getId());
                     if (!var15.isEmpty()) {
                        RetestJob var16 = this.createJobForStock(var9, (StockDto)var15.get(0));
                        if (var16 == null) {
                           this.progressEngine.printToLog(L.t("Result '%s' doesn't contain strategy, skipping!", new Object[]{var9.getName()}));
                        } else {
                           var30.add(var16);
                           if (var15.size() > 1) {
                              this.currentBasketDatabankRecordKey = var34;
                              this.currentBasketStockIndex = 1;
                              this.currentBasketStocks = var15;
                           }
                        }
                     }
                  } else {
                     Map var10 = this.getParams(var9);
                     if (var10 == null) {
                        this.progressEngine.printToLog(L.t("Result '%s' doesn't contain strategy, skipping!", new Object[]{var9.getName()}));
                     } else {
                        String var14 = var9.getName() + this.jobCount++;
                        var30.add(new RetestJob(var14, var10, this.progressEngine, this, this.lastSettingsXml));
                     }
                  }
               } finally {
                  if (var9 != null) {
                     var9.releaseLock("RetestTask");
                  }
               }
            } catch (RecordNotFoundException var27) {
            }
         }
      }

      if (var30.size() > 0) {
         this.gridClient.executeOnGrid(this.jobGroupID, var30);
         return true;
      } else {
         return false;
      }
   }

   private RetestJob createJobForStock(ResultsGroup var1, StockDto var2) throws Exception {
      Map var3 = this.getParams(var1);
      if (var3 == null) {
         return null;
      }

      ChartSetups var4 = (ChartSetups)var3.get("ChartSetups");
      ChartSetup var5 = var4.getMainSetup();
      var5.getMainChart().setSymbol(var2.getTicker());
      var3.put("ChartSetups", var4.getClone());
      var3.put("StrategyName", var1.getName() + "_" + var2.getTicker());
      String var6 = var1.getName() + "_" + var2.getTicker() + this.jobCount++;
      return new RetestJob(var6, var3, this.progressEngine, this, this.lastSettingsXml);
   }

   private void updateBasketIndex(int var1) {
      if (this.currentBasketStocks != null && var1 < this.currentBasketStocks.size()) {
         this.currentBasketStockIndex = var1;
      } else {
         this.currentBasketDatabankRecordKey = null;
         this.currentBasketStockIndex = 0;
         this.currentBasketStocks = null;
      }
   }

   private Map<String, Serializable> getParams(ResultsGroup var1) throws Exception {
      Element var2 = var1.getStrategyXml();
      if (var2 == null) {
         return null;
      }

      HashMap var3 = new HashMap();
      var3.putAll(this.settingsData.getParams());
      var3.put("StrategyXml", var2);
      var3.put("StrategyName", var1.getName());
      var3.put("StrategyNote", var1.specialValues().getString(SpecialValues.Note, null));
      var3.put("Slippage", this.slippage);
      var3.put("MinDistance", this.minDistance);
      var3.put("Commission", this.commission);
      var3.put("Swap", this.swap);
      var3.put("DismissBadStrategies", this.dismissBadStrategies);
      var3.put("StrategyDismissWarnings", this.warningsBadStrategies);
      var3.put("TradingOptions", this.tradingOptions.getClone());
      var3.put("ATM", this.atm == null ? null : this.atm.getClone());
      var3.put("Conditions", this.conditions);
      var3.put("CrossChecks.EvaluateAll", this.evaluateAllCrossChecks);
      var3.put("DeleteFailedStrategies", this.deleteFailedStrategies);
      var3.put("ForceRunCrossChecks", this.forceRunCrossChecks);
      var3.put("IsRetester", true);
      if (this.backtestMode == 0) {
         var3.put("SingleThreadedOptimizations", false);
      } else {
         var3.put("SingleThreadedOptimizations", this.singleThreadedOptimizations);
      }

      ArrayList var4;
      if (!this.useCrossChecks) {
         var4 = this.crossChecks;
      } else {
         var4 = ProjectConfigHelper.cloneCrossChecks(this.crossChecks);
      }

      var3.put("CrossChecks", var4);
      if (var1.specialValues().containsKey(SpecialValues.IsMergedPortfolio)) {
         var3.put("PortfolioMergedStrategies", (ArrayList)StrategyMerger.getSplitResults(var1));
      }

      var3.put("DateGenerated", var1.specialValues().getLong(SpecialValues.DateGenerated, -1L));
      return var3;
   }

   private void initParams() throws JDOMException, IOException, Exception {
      this.slippage = (Double)((Serializable)this.settingsData.getParams().get("Slippage"));
      this.minDistance = (Double)((Serializable)this.settingsData.getParams().get("MinDistance"));
      this.commission = (CommissionsMethod)this.settingsData.getParams().get("Commission");
      this.swap = (SwapMethod)this.settingsData.getParams().get("Swap");
      if (this.settingsData.getParams().get("StrategyDismissSettings") != null) {
         this.dismissBadStrategies = (Integer)((Serializable)this.settingsData.getParams().get("StrategyDismissSettings"));
      } else {
         this.dismissBadStrategies = 0;
      }

      if (this.settingsData.getParams().get("StrategyDismissWarnings") != null) {
         this.warningsBadStrategies = (Boolean)((Serializable)this.settingsData.getParams().get("StrategyDismissWarnings"));
      } else {
         this.warningsBadStrategies = false;
      }

      this.tradingOptions = (TradingOptions)this.settingsData.getParams().get("TradingOptions");
      this.atm = (ATM)this.settingsData.getParams().get("ATM");
      this.conditions = (ArrayList<Condition>)this.settingsData.getParams().get("Conditions");
      this.useCrossChecks = SettingsMap.getBool(this.settingsData.getParams().get("UseRobustnessTests"), false);
      this.crossChecks = (ArrayList<ICrossCheck>)this.settingsData.getParams().get("CrossChecks");
      this.evaluateAllCrossChecks = SettingsMap.getBool(this.settingsData.getParams().get("CrossChecks.EvaluateAll"), false);
      this.deleteFailedStrategies = SettingsMap.getBool(this.settingsData.getParams().get("DeleteFailedStrategies"), false);
      this.forceRunCrossChecks = SettingsMap.getBool(this.settingsData.getParams().get("ForceRunCrossChecks"), false);
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

   private void retestFinished() {
      if (this.taskErrorInfo != null) {
         File var1 = null;

         try {
            var1 = ProjectConfigHelper.getStrategyFile(this.outputDatabank, this.taskErrorInfo.strategyName, 1024, "RetestTask");
         } catch (Exception var3) {
         }

         ProjectPanel.reportBug(this.getTaskElement(), var1, this.taskErrorInfo.strategyName, this.taskErrorInfo.exception);
      }

      this.progressEngine.unregisterStatusListener(this);
   }

   public void onStatusChanged(int var1) {
      this.progressStatusChanged(var1);
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
                              this.inputDatabank.remove(var8.getName(), true, true, true, true, "RetestTask");
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
                              if (this.deleteFailedStrategies) {
                                 var10 = L.t("deleted from databank, failed: Custom analysis", new Object[0]);
                                 if (this.inputDatabank == this.outputDatabank) {
                                    this.inputDatabank.remove(var8.getName(), true, true, true, true, "RetestTask");
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

                           var8.setLastSettings(this.lastSettingsXml);
                           var8.removeUnsavableSettings();
                           var8.setDatabank(this.outputDatabank);
                           if (this.inputDatabank == this.outputDatabank) {
                              this.outputDatabank.update(var8.getName(), var8, true, "RetestTask");
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
                        var9 = var7.getDismissalMessage();
                        this.project.increaseStrategiesFailed();
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

   protected void checkAllFinished() {
      if (this.progressEngine.isFinished()) {
         if (this.jobsCreationTimer != null) {
            this.jobsCreationTimer.cancel();
            this.jobsCreationTimer = null;
         }
      } else {
         int var1 = this.lastFinishedIndex.get();
         boolean var2 = this.lastProcessedIndex >= this.databankRecordKeys.size() && this.currentBasketDatabankRecordKey == null;
         if (var1 >= this.databankRecordKeys.size()
            && this.gridClient.countRunningJobs(this.jobGroupID) <= 1L
            && this.gridClient.countWaitingJobs(this.jobGroupID) == 0L
            && var2) {
            if (this.jobsCreationTimer != null) {
               this.jobsCreationTimer.cancel();
               this.jobsCreationTimer = null;
            }

            this.gridClient.stop(this.jobGroupID);
            this.progressEngine.finish(System.currentTimeMillis() - this.projectStartTime);
            this.outputDatabank.updateBestResults();
            this.project.bestResults.resetLastData();
            this.project.publisher.resetLastData("best-results-channel");
         }
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
      return "Retest";
   }

   public String getPluginFolderName() {
      return "TaskRetest";
   }

   public String getName() {
      return L.tsq("Retest strategies");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      RetestTask var3 = new RetestTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public int getPreferredPosition() {
      return 1;
   }

   public String[] getSettings() {
      return new String[]{"Data", "Notes", "RiskMoneyManagement", "Rankings", "Options", "CrossChecks", "Databanks", "ATMs"};
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
      String var3 = L.t("Total tested", new Object[0])
         + ": "
         + var2.totalJobsDone
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

   public String getProduct() {
      return "SQUANTAlgoWizardAlgoWizardStandaloneBACKTESTNODE";
   }
}
