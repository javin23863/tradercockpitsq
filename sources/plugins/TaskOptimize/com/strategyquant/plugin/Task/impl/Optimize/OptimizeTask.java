package com.strategyquant.plugin.Task.impl.Optimize;

import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.gridlib.client.RunningStatus;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.backtest.BacktestDataInitializer;
import com.strategyquant.tradinglib.backtest.LoadDataProgressEngine;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.ConditionsChecker;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisInfo;
import com.strategyquant.tradinglib.databank.OptimizeDatabankFilter;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.engine.stockpicker.StockpickerBacktestEngine;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.optimization.IEngineLogListener;
import com.strategyquant.tradinglib.optimization.INewResultListener;
import com.strategyquant.tradinglib.optimization.ISequentialOptimizationCompleteListener;
import com.strategyquant.tradinglib.optimization.IStepsListener;
import com.strategyquant.tradinglib.optimization.OptimizationEngine;
import com.strategyquant.tradinglib.optimization.OptimizationParams;
import com.strategyquant.tradinglib.optimization.OptimizationSettings;
import com.strategyquant.tradinglib.optimization.Parameter;
import com.strategyquant.tradinglib.optimization.ParametersSettings;
import com.strategyquant.tradinglib.optimization.SequentialOptimizationEngine;
import com.strategyquant.tradinglib.optimization.SequentialOptimizationSettings;
import com.strategyquant.tradinglib.optimization.WalkForwardParametersStability;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.robustnesstests.SequentialOptimizationResults;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import com.strategyquant.tradinglib.taskImpl.TaskException;
import com.strategyquant.tradinglib.util.RngUtils;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tamas Takacs")
@Name(name = "OptimizeTask plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class OptimizeTask extends AbstractTask implements ILastEventListener {
   public static final Logger Log = LoggerFactory.getLogger("OptimizeTask");
   private static final String LOCK_OPTIMIZETASK = "OptimizeTask";
   private OptimizationSettings optimizationSettings;
   private Databank inputDatabank;
   private Databank outputDatabank;
   private ArrayList<String> strategies = new ArrayList<>();
   private IFitnessFunction fitnessFunction;
   private TradingOptions tradingOptions;
   private ATM atm;
   private ParametersSettings paramSettings;
   private Element taskSettings;
   private int totalSteps;
   private int progressPercent = 0;
   private int lastProcessedIndex = -1;
   private int optimizedStrategies;
   private CustomAnalysisMethod caMethod;
   private String caInputArgs;
   private boolean caFilter;
   private String lastSettingsXml;
   protected ConditionsChecker conditionsChecker = new ConditionsChecker();
   protected int dismissalReason = 0;
   protected String dismissalMessage = "";
   private SequentialOptimizationEngine engine;

   public OptimizeTask() throws Exception {
      this(null, null);
   }

   public OptimizeTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
      this.optimizationSettings = new OptimizationSettings(var1);
   }

   public String getType() {
      return "Optimize";
   }

   public String getName() {
      return L.tsq("Optimize strategies");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      OptimizeTask var3 = new OptimizeTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   private boolean beforeStart() throws Exception {
      try {
         if (!MainApp.v571hfnsHw().gDmtOfRJBr()) {
            Log.info(MainApp.OnlyInProVersionTitle);
            return false;
         }

         this.checkSpecialTrialLimitation();
         ChartSetups var1 = (ChartSetups)this.settingsData.getParams().get("ChartSetups");
         this.optimizationSettings = (OptimizationSettings)this.settingsData.getParams().get("OptimizationSettings");
         this.project = ProjectEngine.get(this.projectName);

         try {
            this.optimizationSettings.inputDatabank = this.optimizationSettings.inputDatabank != null
               ? this.optimizationSettings.inputDatabank
               : (Databank)this.project.getDatabanks().get("Results");
         } catch (Exception var3) {
            throw new TaskException(7, var3);
         }

         ProjectConfigHelper.checkAdditionalChartsAndCIndysExpected(this.projectName, this.optimizationSettings.inputDatabank, var1.getMainSetup(), null);
         ProjectConfigHelper.checkVolumeProfileBlocksAllowed(this.optimizationSettings.inputDatabank, null);
         if (!RunningStatus.canBeStarted(this.getRunningStatus())) {
            Log.error("Cannot start project in incorrect running status: " + this.getRunningStatus());
            return false;
         }

         ProjectResources.addResources(this);
         this.taskSettings = this.getTaskElement();
         this.lastSettingsXml = XMLUtil.elementToString(this.taskSettings);
         this.progressEngine.setLogPrefix(this.taskLogPrefix);
         this.progressEngine.printToLog("================================");
         this.progressEngine.printToLog(L.t("Starting optimization...", new Object[0]));
         this.progressEngine.start();
         if (this.somethingToOptimize()) {
            this.initializeBacktestData();
            this.recognizeCustomAnalysisMethod();
            if (this.progressEngine.isStopped()) {
               this.progressEngine.printToLog(L.t("Project stopped", new Object[0]));
               return false;
            } else {
               this.optimizedStrategies = 0;
               return true;
            }
         } else {
            this.progressEngine.printToLog(L.t("Nothing to optimize", new Object[0]));
            this.progressEngine.finish();
            return false;
         }
      } catch (TaskStoppedException var4) {
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

   private boolean somethingToOptimize() throws Exception {
      if (this.optimizationSettings.singleStrategy) {
         ResultsGroup var3 = this.getStrategyToOptimize(this.optimizationSettings.strategyFile);
         if (var3 == null) {
            return false;
         }

         ProjectConfigHelper.checkVolumeProfileBlocksAllowed(var3);
         return true;
      } else {
         try {
            this.inputDatabank = this.optimizationSettings.inputDatabank;
         } catch (Exception var2) {
            return false;
         }

         if (this.inputDatabank == null) {
            return false;
         }

         int var1 = this.inputDatabank.getRecordsSizeNoLock();
         return var1 > 0;
      }
   }

   private void initializeBacktestData() throws Exception {
      ArrayList var1 = (ArrayList)this.settingsData.getParams().get("ChartSetups");
      BacktestDataInitializer var2 = new BacktestDataInitializer(new LoadDataProgressEngine(this.progressEngine), var1, false, null);
      var2.prepareData();
   }

   public void start() throws Exception {
      if (this.beforeStart()) {
         this.project.resetTracking(false, false);
         this.outputDatabank = (Databank)this.settingsData.getParams().get("DatabankTarget");
         this.outputDatabank = this.outputDatabank != null ? this.outputDatabank : (Databank)this.project.getDatabanks().get("Results");
         this.progressEngine.reset();
         this.progressEngine.start();
         this.strategies.clear();

         try {
            this.inputDatabank = this.optimizationSettings.inputDatabank;

            for (int var1 = 0; var1 < this.inputDatabank.getRecords().size(); var1++) {
               ResultsGroup var2 = (ResultsGroup)this.inputDatabank.getRecords().get(var1);
               this.strategies.add(var2.getName());
            }
         } catch (Exception var10) {
            Log.info("Input databank not set");
            this.inputDatabank = null;
         }

         this.fitnessFunction = (IFitnessFunction)this.settingsData.getParams().get("FitnessFunction");
         this.tradingOptions = (TradingOptions)this.settingsData.getParams().get("TradingOptions");
         this.atm = (ATM)this.settingsData.getParams().get("ATM");
         this.setDatabankFilter(this.outputDatabank);
         this.progressPercent = 0;
         this.lastProcessedIndex = -1;

         try {
            if (this.optimizationSettings.singleStrategy) {
               ResultsGroup var12 = this.getStrategyToOptimize(this.optimizationSettings.strategyFile);
               if (var12 == null) {
                  this.progressEngine.printToLog(L.t("Cannot load file to optimize, please check its path", new Object[0]));
               } else {
                  OptimizeTask.StrategyData var14 = this.getStrategyData(var12);
                  this.optimizedStrategies++;
                  this.optimizeStrategy(var14);
               }
            } else {
               if (this.inputDatabank == null) {
                  throw new Exception(L.t("Input databank not set.", new Object[0]));
               }

               for (int var11 = this.lastProcessedIndex + 1; var11 < this.strategies.size() && !this.progressEngine.isStopped(); var11++) {
                  ResultsGroup var13 = this.inputDatabank.getLocked(this.strategies.get(var11), "OptimizeTask");
                  OptimizeTask.StrategyData var3 = null;

                  try {
                     var3 = this.getStrategyData(var13);
                  } finally {
                     var13.releaseLock("OptimizeTask");
                  }

                  this.optimizedStrategies++;
                  this.optimizeStrategy(var3);
                  this.lastProcessedIndex++;
               }
            }

            this.progressEngine.finish();
         } catch (Exception var9) {
            this.progressEngine.error();
            this.progressEngine.printToLog(var9.getMessage());
            Log.error("Optimization error. ", var9);
            this.project.getTrackingInfo().error = var9.getMessage();
            this.project.getTrackingInfo().errorInfo = var9.getMessage();
            this.project.getTrackingInfo().errorType = 50;
         }
      }
   }

   private ResultsGroup getStrategyToOptimize(File var1) {
      String var2 = this.optimizationSettings.strategyFile.getName();
      String var3 = SQUtils.stripExtension(var2);
      ResultsGroup var4 = null;

      try {
         var4 = this.outputDatabank.getLocked(var3, "OptimizeTask");
      } catch (Exception var10) {
         Log.debug("Record not found in databank,", var10);
      } finally {
         if (var4 != null) {
            var4.releaseLock("OptimizeTask");
         }
      }

      if (var4 != null) {
         return var4;
      }

      if (var1.exists()) {
         try {
            return (ResultsGroup)Program.get("Loader").call("loadFile", new Object[]{var1.getAbsolutePath()});
         } catch (Exception var12) {
            var12.printStackTrace();
         }
      }

      return null;
   }

   private OptimizeTask.StrategyData getStrategyData(ResultsGroup var1) throws Exception {
      OptimizeTask.StrategyData var2 = new OptimizeTask.StrategyData();
      var2.strategyName = var1.getName();
      var2.elStrategyXml = var1.getStrategyXml();
      var2.dateGenerated = var1.specialValues().getLong(SpecialValues.DateGenerated, -1L);
      return var2;
   }

   protected void setDatabankFilter(Databank var1) {
      try {
         int var2 = 0;
         if (this.optimizationSettings.storeAll) {
            var2 = (Integer)((Serializable)this.settingsData.getParams().get("MaxStrategies"));
            var2 = Math.max(var2, 1);
         } else {
            var2 = 1;
         }

         var1.applyFilter(new OptimizeDatabankFilter(var2));
      } catch (Exception var3) {
         Log.error(
            "Project '" + this.projectName + "', Task '" + this.getCustomName() + "' - Cannot set databank filter do databank '" + var1.getName() + "'.", var3
         );
      }
   }

   private void optimizeStrategy(OptimizeTask.StrategyData var1) throws Exception {
      try {
         SettingsMap var2 = null;
         ChartSetups var3 = (ChartSetups)this.settingsData.getParams().get("ChartSetups");
         int var4 = var3.getMainSetup().getBacktestEngine();
         String var5 = var3.getMainSetup().getSymbol();
         if (var4 == -1816889229 && DataManager.isGroupAlias(var5)) {
            BasketDto var16 = BasketOfStocksManager.getInstance().getBasket(var5);
            if (var16 == null) {
               throw new Exception(L.t("Group with name '{0}' not found.", new Object[]{var5}));
            }

            List var7 = BasketOfStocksManager.getInstance().getStocks(var16.getId());

            for (int var8 = 0; var8 < var7.size(); var8++) {
               StockDto var9 = (StockDto)var7.get(var8);
               var2 = this.prepareSettings(var1);
               var3 = (ChartSetups)var2.get("ChartSetups");
               ChartSetup var10 = var3.getMainSetup();
               var10.getMainChart().setSymbol(var9.getTicker());
               StockpickerBacktestEngine.createTempStockGroup(var10);
               String var11 = var1.strategyName + "_" + var9.getTicker();
               var2.set("StrategyName", var11);
               if (this.optimizationSettings.type == 4) {
                  this.runSequentialOptimization(var2, var11, var1);
               } else {
                  this.runStandardOptimization(var2, var11, var1);
               }
            }
         } else {
            var2 = this.prepareSettings(var1);
            String var6 = var1.strategyName;
            if (this.optimizationSettings.type == 4) {
               this.runSequentialOptimization(var2, var6, var1);
            } else {
               this.runStandardOptimization(var2, var6, var1);
            }
         }
      } catch (Exception var12) {
         if (this.optimizationSettings.singleStrategy) {
            throw var12;
         }

         this.progressEngine.printToLog(var1.strategyName + " Error " + var12.getMessage());
         Log.error("Optimization error in strategy " + var1.strategyName, var12);
      }
   }

   private void runStandardOptimization(SettingsMap var1, final String var2, OptimizeTask.StrategyData var3) throws Exception {
      OptimizationEngine var4 = new OptimizationEngine(this.projectName, this.progressEngine, null, "OptimTask", this, this.lastSettingsXml);
      var4.initialize(var1);
      this.totalSteps = var4.getTotalStepsCount();
      this.updateProjectInfo(0);
      var4.setNewResultListener(new INewResultListener() {
         public void newResult(ResultsGroup var1) throws Exception {
            OptimizeTask.this.processResult(var1, var2);
         }
      });
      var4.setLogListener(new IEngineLogListener() {
         public void processMessage(String var1) {
            OptimizeTask.this.progressEngine.printToLogDebug(var1);
         }
      });
      var4.setStepsListener(new IStepsListener() {
         public void step(int var1) {
            OptimizeTask.this.progressPercent = (int)((double)var1 / OptimizeTask.this.totalSteps * 100.0);
            OptimizeTask.this.updateProjectInfo(var1);
         }
      });
      var4.optimize();
      this.updateIndexByFitness(var1);
   }

   private void updateIndexByFitness(SettingsMap var1) {
      if (this.shouldUpdateFitnessByIndex()) {
         Log.info("Updating fitness by index in Result databank '{}'", this.outputDatabank.getName());
         ArrayList var2 = (ArrayList)this.outputDatabank.getRecords().clone();
         RngUtils.sortCandidatesByIndexFitness(var2, this.fitnessFunction);
         this.outputDatabank.refreshGrid();
      }
   }

   private boolean shouldUpdateFitnessByIndex() {
      if (this.optimizationSettings.type != 1) {
         return false;
      }

      String var1 = "fitnessSimple";
      return (Boolean)this.settingsData.getParams().getOrDefault("UseFitnessByIndex", false);
   }

   private void runSequentialOptimization(final SettingsMap var1, final String var2, OptimizeTask.StrategyData var3) throws Exception {
      SequentialOptimizationSettings var4 = new SequentialOptimizationSettings();
      var4.distributionDown = this.paramSettings.distributionDown;
      var4.distributionUp = this.paramSettings.distributionUp;
      var4.steps = this.paramSettings.maxSteps;
      var4.stableAreaCount = this.optimizationSettings.stableAreaCount;
      var4.stabilityRangePct = this.optimizationSettings.stabilityRangePct;
      var4.pctToPass = this.optimizationSettings.pctToPass;
      var4.applyToStrategy = true;
      var4.paramTypes = this.paramSettings.paramTypes;
      var4.symmetry = this.paramSettings.symmetry;
      var4.manualParams = this.paramSettings.manualMode ? this.paramSettings.params : null;
      this.engine = new SequentialOptimizationEngine(var4, this.progressEngine, this, this.lastSettingsXml);
      this.engine.setNewResultListener(new INewResultListener() {
         public void newResult(ResultsGroup var1) throws Exception {
            if (var1.getName().equals(var2)) {
               OptimizeTask.this.processResult(var1, var2);
            }
         }
      });
      this.engine.setLogListener(new IEngineLogListener() {
         public void processMessage(String var1) {
            OptimizeTask.this.progressEngine.printToLogDebug(var1);
         }
      });
      this.engine.setStepsListener(new IStepsListener() {
         public void step(int var1) {
            OptimizeTask.this.progressPercent = (int)((double)var1 / OptimizeTask.this.totalSteps * 100.0);
            OptimizeTask.this.updateProjectInfo(var1);
         }
      });
      this.engine.initialize(var2, var3.elStrategyXml.clone(), var1);
      this.totalSteps = this.engine.getTotalStepsCount();
      this.updateProjectInfo(0);
      this.engine.optimize(true, false, null, new ISequentialOptimizationCompleteListener() {
         public void onFinish(boolean var1x, StrategyBase var2x, SequentialOptimizationResults var3x, ResultsGroup var4x) {
            OptimizeTask.this.onFinishSeqOpt(var1x, var2, var1, var2x, var3x, var4x);
         }
      });
   }

   private void onFinishSeqOpt(boolean var1, String var2, SettingsMap var3, StrategyBase var4, SequentialOptimizationResults var5, ResultsGroup var6) {
      String var7 = var2 + " - Sequential optimized";
      if (var1 && var6 != null) {
         try {
            var6.setName(var7);
            var6.mainResult().setSequentialOptimizationResults(var5);
            var6.mainResult().addStrategy(var4);
            var6.mainResult().addStrategyXml(var4.getStrategyXml());
            if (this.checkSequentialOptConditions(var6, var3)) {
               this.outputDatabank.add(var6, true);
            } else {
               this.progressEngine.printToLogDebug(String.format("%s, " + L.t("dismissed", new Object[0]) + ": %s", var7, this.dismissalMessage));
            }
         } catch (Exception var27) {
            Log.error("Updating sequential optimized strategy failed", var27);
         }
      } else {
         String var8 = this.engine.getDismissalMessage();
         this.progressEngine
            .printToLog(L.t("Sequential optimization evaluated as unstable, parameters not changed", new Object[0]) + (var8 != null ? " - " + var8 : ""));
         ResultsGroup var9 = null;
         if (this.optimizationSettings.singleStrategy) {
            try {
               var9 = this.getStrategyToOptimize(this.optimizationSettings.strategyFile).clone();
            } catch (Exception var31) {
               Log.error("Getting original strategy from file failed", var31);
            }
         } else {
            try {
               var9 = this.outputDatabank.getLocked(var2, "OptimizeTask");
            } catch (Exception var30) {
               Log.debug("Record not found in databank,", var30);
            } finally {
               if (var9 != null) {
                  var9.releaseLock("OptimizeTask");
               }
            }

            if (var9 == null) {
               ResultsGroup var10 = null;

               try {
                  var10 = this.inputDatabank.getLocked(var2, "OptimizeTask");
                  var9 = var10.clone();
               } catch (Exception var29) {
                  Log.error("Getting original strategy from databank failed", var29);
               } finally {
                  if (var10 != null) {
                     var10.releaseLock("OptimizeTask");
                  }
               }
            }
         }

         if (var9 != null) {
            try {
               var9.setName(var7);
               var9.mainResult().addStrategy(var4);
               var9.mainResult().addStrategyXml(var4.getStrategyXml());
               var9.mainResult().setSequentialOptimizationResults(var5);
               if (this.checkSequentialOptConditions(var9, var3)) {
                  this.outputDatabank.add(var9, true);
               } else {
                  this.progressEngine.printToLogDebug(String.format("%s, " + L.t("dismissed", new Object[0]) + ": %s", var7, this.dismissalMessage));
               }
            } catch (Exception var28) {
               Log.error("Updating sequential optimization result failed", var28);
            }
         }
      }
   }

   protected boolean checkSequentialOptConditions(ResultsGroup var1, SettingsMap var2) {
      OptimizationSettings var3 = (OptimizationSettings)var2.get("OptimizationSettings");
      ArrayList var4 = var3.conditions;
      if (var4 != null && var4.size() > 0) {
         for (int var5 = 0; var5 < var4.size(); var5++) {
            Condition var6 = (Condition)var4.get(var5);
            if (var6.isUsed()) {
               try {
                  boolean var7 = this.conditionsChecker.check(var1, var6);
                  if (!var7) {
                     this.dismissalReason = 200000 + var5;
                     this.dismissalMessage = this.conditionsChecker.dismissalMessage;
                     return false;
                  }
               } catch (CrossCheckDataNotExistException var8) {
               } catch (Exception var9) {
                  Log.error("Error while checking condition", var9);
                  this.dismissalReason = 10003;
                  this.dismissalMessage = String.format("Error evaluating conditions: %s", var9.getMessage());
                  return false;
               }
            }
         }
      }

      return true;
   }

   protected void processResult(ResultsGroup var1, String var2) {
      boolean var3 = false;

      try {
         try {
            if (SQProject.isMemoryProtectionUsed()) {
               MemoryUsageChecker.checkAvailableMemory();
            }

            if (var1.getOptimizationProfile() != null) {
            }

            boolean var4 = true;
            var1.removeUnsavableSettings();
            if (var1.getName().equals(var2)) {
               if (!this.optimizationSettings.dontSaveOriginalStr) {
                  if (this.outputDatabank.contains(var2)) {
                     this.outputDatabank.update(var2, var1, true, "OptimizeTask");
                     this.outputDatabank.updateBestResults(var1);
                  } else {
                     var4 = true;
                     if (this.caMethod != null) {
                        this.caMethod.setInputArgs(this.caInputArgs);
                        var4 = this.caMethod.filterStrategy(this.project.getName(), this.project.getActiveTaskName(), this.outputDatabank.getName(), var1);
                        if (!var4) {
                           var1.specialValues().setString(SpecialValues.FiltersResultFailedReason, BadStrategyException.getReasonAsString(10038));
                        }
                     }

                     this.outputDatabank.add(var1, true);
                     this.outputDatabank.updateBestResults(var1);
                     String var19 = var1.specialValues().getString(SpecialValues.FiltersResultFailedReason);
                     if (var19 == null || var19.equals(SpecialValues.FiltersResultPassed)) {
                        var3 = true;
                        return;
                     }
                  }

                  return;
               }

               return;
            }

            if (this.optimizationSettings.deleteFailedStr) {
               String var16 = var1.specialValues().getString(SpecialValues.FiltersResultFailedReason);
               if (!var16.equals(SpecialValues.FiltersResultPassed)) {
                  this.progressEngine.printToLogDebug(L.t("%s\n   - deleted from databank, failed: %s", new Object[]{var1.getName(), var16}));
                  return;
               }
            }

            var4 = true;
            if (this.caMethod != null) {
               var4 = this.caMethod.filterStrategy(this.project.getName(), this.project.getActiveTaskName(), this.outputDatabank.getName(), var1);
               if (!var4) {
                  var1.specialValues().setString(SpecialValues.FiltersResultFailedReason, BadStrategyException.getReasonAsString(10038));
               }
            }

            if ((this.optimizationSettings.type == 1 || this.optimizationSettings.type == 4) && !this.optimizationSettings.storeAll) {
               boolean var17 = this.outputDatabank.add(var1, true);
               if (!var17) {
                  var1.clear();
               }
            } else if (this.optimizationSettings.type != 1 && this.optimizationSettings.type != 4) {
               WalkForwardParametersStability.compute(var1, this.paramSettings.params);
               WalkForwardParametersStability.compute(var1, this.paramSettings.params);
               this.outputDatabank.add(var1, true, true);
            } else if (var4 || !this.caFilter) {
               this.outputDatabank.add(var1, true, false);
            }

            String var18 = var1.specialValues().getString(SpecialValues.FiltersResultFailedReason);
            if (var18 == null || var18.equals(SpecialValues.FiltersResultPassed)) {
               var3 = true;
            }

            this.outputDatabank.updateBestResults(var1);
         } catch (Exception var10) {
            String var5 = "Error while processing strategy '" + var1.getName() + "'";
            if (var1 != null) {
               var1.clear();
            }

            Log.error(var5, var10);
            this.progressEngine.printToLog(var5 + " - " + var10.getMessage());
         } catch (OutOfMemoryError var11) {
            this.project.onMemoryError(var11);
            if (var1 != null) {
               var1.clear();
               return;
            }
         }
      } finally {
         this.project.increaseGlobalStats(this.getCustomName().hashCode(), L.t("Optimized", new Object[0]), 1L, L.t("Passed", new Object[0]), var3 ? 1L : 0L);
      }
   }

   private SettingsMap prepareSettings(OptimizeTask.StrategyData var1) throws Exception {
      try {
         SettingsMap var2 = new SettingsMap();

         for (String var4 : this.settingsData.getParams().keySet()) {
            var2.set(var4, this.settingsData.getParams().get(var4));
         }

         var2.set("TestPrecision", 1);
         var2.set("StrategyXml", var1.elStrategyXml);
         var2.set("StrategyLastSettings", this.taskSettings);
         this.paramSettings = new ParametersSettings();
         this.paramSettings.setFromXML(this.taskSettings, var1.elStrategyXml);
         StrategyBase var13 = StrategyBase.createXmlStrategy(var1.elStrategyXml);
         var13.transformToVariables(this.paramSettings.symmetry, this.paramSettings.paramTypes);
         if (this.paramSettings.manualMode) {
            OptimizationParams var14 = this.paramSettings.params;

            for (int var5 = 0; var5 < var14.size(); var5++) {
               Parameter var6 = (Parameter)var14.get(var5);
               Variable var7 = var13.variables().get(var6.getName());
               if (var7 != null) {
                  if (!"ParamTypeTradingOptions".equals(var7.getParamType())) {
                     if (var6.getType() == 2) {
                        var7.setValue(var6.getOriginalValue() == 1.0);
                     } else if (var6.getType() == 5) {
                        var7.setValue((int)var6.getOriginalValue());
                     } else {
                        var7.setValue(var6.getOriginalValue());
                     }
                  } else {
                     String[] var8 = var7.getId().split("\\.");
                     Serializable var9 = null;
                     if (var6.getType() == 2) {
                        var9 = var6.getOriginalValue() == 1.0;
                     } else if (var6.getType() != 5 && var6.getType() != 1) {
                        var9 = var6.getOriginalValue();
                     } else {
                        var9 = (int)var6.getOriginalValue();
                     }

                     for (int var10 = 0; var10 < this.tradingOptions.size(); var10++) {
                        TradingOption var11 = (TradingOption)this.tradingOptions.get(var10);
                        if (var11.getClass().getSimpleName().equals(var8[0])) {
                           var11.setParameterValue(var8[1], var9.toString());
                        }
                     }
                  }
               } else {
                  Log.error("Unable to set original value of parameter " + var6.getName() + " - Variable not found");
               }
            }
         }

         var2.set("StrategyObject", var13);
         var2.set("StrategyName", var1.strategyName);
         var2.set("FitnessFunction", this.fitnessFunction);
         var2.set("TradingOptions", this.tradingOptions.getClone());
         var2.set("ATM", this.atm == null ? null : this.atm.getClone());
         OptimizationSettings var15 = this.optimizationSettings.clone();
         var15.parameters = this.paramSettings.params;
         var15.optimizationMethod = this.paramSettings.method;
         Element var16 = XMLUtil.getChildElem(this.taskSettings, "Rankings");
         Element var17 = var16.getChild("WalkForward");
         if (var17 == null) {
            throw new Exception(L.t("Walk-Forward Optimization is missing Robustness settings.", new Object[0]));
         }

         this.optimizationSettings.dontSaveOriginalStr = XMLUtil.getBoolean(var17, "DontSaveOriginalStr", false);
         Element var18 = XMLUtil.getChildElem(var17, "Conditions", "<Conditions/>");
         var15.elConditions = var18;
         var15.conditions = ProjectConfigHelper.getConditions(var18);
         if (this.optimizationSettings.type == 3 || this.optimizationSettings.type == 2) {
            var15.thresholdPct = XMLUtil.getIntAttr(var18, "thresholdPct", 80);
            var15.robCombRows = XMLUtil.getIntAttr(var18, "robCombRows", 3);
            var15.robCombCols = XMLUtil.getIntAttr(var18, "robCombCols", 3);
            var15.robMinComb = XMLUtil.getIntAttr(var18, "robMinComb", 7);
            this.optimizationSettings.deleteFailedStr = XMLUtil.getBoolean(var17, "DeleteFailedStr", false);
         }

         var2.set("OptimizationSettings", var15);
         var2.set("Slippage", (Double)((Serializable)this.settingsData.getParams().get("Slippage")));
         var2.set("MinDistance", (Double)((Serializable)this.settingsData.getParams().get("MinDistance")));
         var2.set("Commission", (CommissionsMethod)this.settingsData.getParams().get("Commission"));
         var2.set("Swap", (SwapMethod)this.settingsData.getParams().get("Swap"));
         var2.set("DateGenerated", var1.dateGenerated);
         return var2;
      } catch (Exception var12) {
         throw new Exception(L.t("Error while preparing settings - %s", new Object[]{var12.getMessage()}), var12);
      }
   }

   protected int getRunningStatus() {
      return this.progressEngine.getRunningStatus();
   }

   private void updateProjectInfo(int var1) {
      try {
         ProjectRunInfo var2 = this.project.getTrackingInfo();
         var2.totalSteps = this.totalSteps;
         var2.step = var1;
         var2.progressPercent = this.progressPercent;
         var2.taskRunningTime = this.project.getCurrentTaskRunningTime();
         if (var2.step == 0) {
            var2.timeToFinish = 0L;
         } else {
            var2.timeToFinish = var2.taskRunningTime / var2.step * (var2.totalSteps - var2.step);
         }

         var2.infiniteProgress = false;
         var2.progressPercent = 100.0F * var2.step / var2.totalSteps;
      } catch (Exception var3) {
         Log.error("Cannot update project info. ", var3);
      }
   }

   public String getPluginFolderName() {
      return "TaskOptimize";
   }

   public int getPreferredPosition() {
      return 2;
   }

   public String[] getSettings() {
      return new String[]{"Data", "Options", "Notes", "RiskMoneyManagement", "Rankings", "Optimization", "ATMs"};
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
      var1.print(L.t("Total steps", new Object[0]) + ": " + var2.totalSteps);
   }

   public void setLastEvent(String var1) {
      if (this.project != null) {
         this.project.setLastEvent(var1);
      }
   }

   class StrategyData {
      String strategyName;
      Element elStrategyXml;
      long dateGenerated;
   }
}
