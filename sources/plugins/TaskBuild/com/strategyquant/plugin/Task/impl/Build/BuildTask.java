package com.strategyquant.plugin.Task.impl.Build;

import com.strategyquant.allTimeStats.AllTimeStats;
import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.dataseries.DoubleListCache;
import com.strategyquant.gridlib.client.RunningStatus;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.random.MersenneTwisterRng;
import com.strategyquant.lib.volumeProfile.VolumeProfileBlocks;
import com.strategyquant.lib.volumeProfile.VolumeProfileSubscription;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.backtest.BacktestDataInitializer;
import com.strategyquant.tradinglib.backtest.LoadDataProgressEngine;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisInfo;
import com.strategyquant.tradinglib.databank.DefaultDatabankFilter;
import com.strategyquant.tradinglib.databank.ImproveDatabankFilter;
import com.strategyquant.tradinglib.databank.OptimizeDatabankFilter;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.generator.StrategyGeneratorCache;
import com.strategyquant.tradinglib.generator.StrategyTemplateGenerator;
import com.strategyquant.tradinglib.gp.IGPFinishedListener;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.task.settings.blocks.ConfigConverter;
import com.strategyquant.tradinglib.task.settings.buildmode.BuildMode;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tamas Takacs")
@Name(name = "BuildTask plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class BuildTask extends AbstractTask {
   public static final Logger Log = LoggerFactory.getLogger(BuildTask.class);
   private BuildMode buildMode;
   private String buildType;
   private Databank initialPopulationDatabank;
   private Databank outputDatabank;
   private Databank lastGenerationDatabank;
   private IBuildEngine buildEngine = null;
   private static final MersenneTwisterRng rng = new MersenneTwisterRng();
   private ConfigConverter configConverter;
   private ArrayList<String> strategiesToImprove = new ArrayList<>();
   private static final String LOCK_IMPROVETASK = "BuildImproveTask";
   private AtomicBoolean buildRunning = new AtomicBoolean();
   private boolean improveDatabank = false;
   private String improveDatabankName = null;
   private CustomAnalysisMethod caMethod;
   private String caInputArgs;
   private boolean caFilter;
   private int tradingEngine;

   public BuildTask() throws Exception {
      this(null, null);
   }

   public BuildTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
      this.configConverter = new ConfigConverter(this.getCustomName());
   }

   private void initParams() throws JDOMException, IOException, Exception {
      this.buildMode = (BuildMode)this.settingsData.getParams().get("BuildMode");
      this.buildType = (String)this.settingsData.getParams().get("BuildType");
   }

   public boolean beforeStart() throws Exception {
      try {
         this.initParams();
         DoubleListCache.resetStats();
         this.project = ProjectEngine.get(this.projectName);
         this.project.resetTracking(true, false);
         String var1 = (String)this.settingsData.getParams().get("StopConditionType");
         this.improveDatabank = false;
         Element var2 = (Element)this.settingsData.getParams().get("BlocksConfig");
         Element var3 = var2.getChild("WhatToBuild");
         String var4 = var3.getChild("StrategyType").getAttributeValue("type");
         if (var4.equals("improve")) {
            String var5 = var3.getChild("StrategyType").getAttributeValue("improveType");
            ChartSetups var6 = (ChartSetups)this.settingsData.getParams().get("ChartSetups");
            if (var5.equals("databank")) {
               this.improveDatabank = true;
               this.improveDatabankName = var3.getChild("StrategyType").getAttributeValue("improveDatabank");
               if (this.improveDatabankName != null && !this.project.getDatabanks().containsKey(this.improveDatabankName)) {
                  throw new Exception(
                     L.t(
                        "Build project misconfiguration - you are improving all strategies in databank '%s' that doesn't exist!",
                        new Object[]{this.improveDatabankName}
                     )
                  );
               }

               Databank var7;
               if (this.improveDatabankName != null) {
                  var7 = (Databank)this.project.getDatabanks().get(this.improveDatabankName);
               } else {
                  var7 = (Databank)this.project.getDatabanks().get("Strategies to improve");
               }

               ProjectConfigHelper.checkAdditionalChartsAndCIndysExpected(this.projectName, var7, var6.getMainSetup(), null);
               ProjectConfigHelper.checkVolumeProfileBlocksAllowed(var7, null);
            } else {
               String var20 = var3.getChild("StrategyType").getAttributeValue("strategyFile");
               if (var20 == null || var20.isEmpty()) {
                  throw new Exception(L.t("Improve misconfiguration - No source strategy for improve configured", new Object[0]));
               }

               IProgram var8 = Program.get("Loader");
               ResultsGroup var9 = (ResultsGroup)var8.call("loadFile", new Object[]{var20});
               if (var9 != null) {
                  ProjectConfigHelper.checkAdditionalChartsAndCIndysExpected(this.projectName, var9, var6.getMainSetup());
                  ProjectConfigHelper.checkVolumeProfileBlocksAllowed(var9);
               }
            }
         } else if (var4.equals("template") && !VolumeProfileSubscription.getInstance().isActive()) {
            Element var15 = StrategyTemplateGenerator.getConfiguredTemplate(var2, this.getCustomName());
            if (var15 != null) {
               String var17 = var3.getChild("StrategyType").getAttributeValue("templateFile");
               String var21 = var17 != null ? SQUtils.stripExtension(new File(var17).getName()) : this.getCustomName();
               VolumeProfileBlocks.checkStrategyAllowed(var15, var21);
            }
         }

         if (!this.project.isAppProject() && var1.equals("never")) {
            throw new Exception(
               L.t("Custom project misconfiguration - Build task is configured to never stop. Please change its settings in Rankings.", new Object[0])
            );
         }

         if (this.improveDatabank && !var1.equals("passed-count") && !var1.equals("time-limit")) {
            throw new Exception(
               L.t(
                  "Build project misconfiguration - when you are improving all strategies in databank you have to specify total number of strategies generated or time stop - options 2. or 4. in Settings -> Rankings.",
                  new Object[0]
               )
            );
         }

         ChartSetups var16 = (ChartSetups)this.settingsData.getParams().get("ChartSetups");
         if (var16 != null && var16.size() > 0) {
            ChartSetup var18 = (ChartSetup)var16.get(0);
            String var22 = var18.getMainChart().getSymbol();
            String var23 = var18.getMainChart().getSession();
            if (!"No Session".equals(var23)) {
               DataInfo var24 = DataManager.getDataInfo("History", var22);
               if (var24 != null) {
                  String var10 = var24.timeframe;
                  boolean var11 = TimeframeManager.isTimeFrameLessThen(var10, "D1");
                  if (!var11) {
                     throw new IllegalArgumentException(
                        L.t(
                           "Invalid session - timeframe of your tested symbol can't be used with selected session. Use 'no session' in this case.",
                           new Object[0]
                        )
                     );
                  }
               }
            }
         }

         this.outputDatabank = (Databank)this.settingsData.getParams().get("DatabankTarget");

         try {
            this.initialPopulationDatabank = (Databank)this.settingsData.getParams().get("DatabankSource");
         } catch (Exception var13) {
            if (!var13.getMessage().contains(L.t("No input databank set", new Object[0]))) {
               throw var13;
            }
         }

         try {
            this.lastGenerationDatabank = (Databank)this.settingsData.getParams().get("DatabankLastGeneration");
         } catch (Exception var12) {
            if (!var12.getMessage().contains(L.t("No input databank set", new Object[0]))) {
               throw var12;
            }
         }

         boolean var19 = SettingsMap.getBool(this.settingsData.getParams().get("DismissTooSimilarStrategies"), true);
         this.setDatabankFilter(this.outputDatabank, var19);
         if (!RunningStatus.canBeStarted(this.getRunningStatus())) {
            Log.error("Cannot start project in incorrect running status: " + this.getRunningStatus());
            return false;
         } else {
            this.progressEngine.setLogPrefix(this.taskLogPrefix);
            this.progressEngine.start();
            this.initializeBacktestData();
            this.recognizeCustomAnalysisMethod();
            StrategyGeneratorCache.clearTooFullCache();
            AllTimeStats.getInstance().buildTaskStart();
            if (this.progressEngine.isStopped()) {
               this.progressEngine.printToLog(L.t("Project stopped", new Object[0]));
               return false;
            } else {
               return true;
            }
         }
      } catch (TaskStoppedException var14) {
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

   protected void setDatabankFilter(Databank var1, boolean var2) {
      try {
         int var3 = 100000;
         if (this.settingsData.getParams().containsKey("MaxStrategies")) {
            var3 = (Integer)((Serializable)this.settingsData.getParams().get("MaxStrategies"));
         }

         if (this.getType().equals("Optimize")) {
            var1.applyFilter(new OptimizeDatabankFilter(var3));
         } else if (!this.getType().equals("Retest") && !this.getType().equals("AutomaticRetest")) {
            if (this.improveDatabank) {
               var1.applyFilter(new ImproveDatabankFilter(var3, var2));
            } else {
               var1.applyFilter(new DefaultDatabankFilter(var3, var2));
            }
         }

         Log.debug("Ranking settings set for databank '{}'. ", var1.getName());
      } catch (Exception var4) {
         Log.error(
            "Project '" + this.projectName + "', Task '" + this.getCustomName() + "' - Cannot set databank filter do databank '" + var1.getName() + "'.", var4
         );
      }
   }

   private void initializeBacktestData() throws Exception {
      ArrayList var1 = (ArrayList)this.settingsData.getParams().get("ChartSetups");
      this.tradingEngine = 0;
      if (var1 != null && var1.size() > 0) {
         this.tradingEngine = ((ChartSetup)var1.get(0)).getBacktestEngine();
      }

      boolean var2 = (Boolean)((Serializable)this.settingsData.getParams().get("UseRobustnessTests"));
      ArrayList var3 = (ArrayList)this.settingsData.getParams().get("CrossChecks");
      BacktestDataInitializer var4 = new BacktestDataInitializer(new LoadDataProgressEngine(this.progressEngine), var1, var2, var3);
      var4.prepareData();
   }

   public void start() throws Exception {
      if (this.beforeStart()) {
         if (!this.improveDatabank) {
            this.buildOneStrategy(null, null, null);
         } else {
            this.strategiesToImprove.clear();
            Databank var1;
            if (this.improveDatabankName != null) {
               var1 = (Databank)this.project.getDatabanks().get(this.improveDatabankName);
            } else {
               var1 = (Databank)this.project.getDatabanks().get("Strategies to improve");
            }

            for (int var2 = 0; var2 < var1.getRecords().size(); var2++) {
               ResultsGroup var3 = (ResultsGroup)var1.getRecords().get(var2);
               this.strategiesToImprove.add(var3.getName());
            }

            String var12 = UUID.randomUUID().toString();

            for (int var13 = 0; var13 < this.strategiesToImprove.size() && !this.progressEngine.isStoppedNotFinished(); var13++) {
               ResultsGroup var4 = var1.getLocked(this.strategiesToImprove.get(var13), "BuildImproveTask");

               Element var5;
               String var6;
               try {
                  var5 = var4.getStrategyXml();
                  var6 = var4.getName();
               } finally {
                  var4.releaseLock("BuildImproveTask");
               }

               try {
                  this.buildOneStrategy(var5, var12, var6);
               } catch (Exception var10) {
                  Log.info("Exception when mass-improving strategies", var10);
                  this.progressEngine.printToLog(L.t("Exception improving strategy", new Object[0]) + var6 + " : " + var10.getMessage());
               }
            }
         }
      }
   }

   private void buildOneStrategy(Element var1, String var2, String var3) throws Exception {
      if (var1 != null && var2 != null) {
         String var4 = MainApp.getDataPath() + "tests/tmp/" + var2 + ".xml";
         XMLUtil.xmlToFile(var1, new File(var4));
         Element var5 = (Element)this.settingsData.getParams().get("BlocksConfig");
         Element var6 = var5.getChild("WhatToBuild");
         var6.getChild("StrategyType").setAttribute("strategyFile", var4);
         if (!this.progressEngine.checkRunning()) {
            this.project.resetTracking(true, false);
            this.progressEngine.start();
         }
      }

      this.buildEngine = this.getBuildEngineByMode(this.getTaskElement());
      if (var1 != null && var2 != null) {
         this.buildEngine.setStrategyNamePrefix(var3 + " - Improved ");
      } else {
         this.buildEngine.setStrategyNamePrefix("Strategy ");
      }

      this.buildEngine.testRun();
      this.buildEngine.start();
      this.buildEngine.setFinishListener(new IGPFinishedListener() {
         public void finished(int var1) {
            try {
               BuildTask.this.buildFinished(var1, BuildTask.this.buildEngine.getFinishSent());
               BuildTask.this.buildEngine.destroy();
            } catch (Exception var3x) {
               BuildTask.Log.error("Cannot restart build", var3x);
            }
         }
      });
      if (var1 != null) {
         do {
            Thread.sleep(500L);
         } while (!this.progressEngine.isFinished());
      }
   }

   private void increaseActivityAfterFinish() {
      AllTimeStats.getInstance().buildTaskEnd();
   }

   protected void buildFinished(int var1, boolean var2) throws Exception {
      if (var1 == 3) {
         this.project.onMemoryError(null);
      } else {
         this.progressEngine.done();
         this.increaseActivityAfterFinish();
         if (!var2) {
            this.progressEngine.finish();
         }
      }
   }

   private IBuildEngine getBuildEngineByMode(Element var1) throws Exception {
      ProjectResources.addResources(this);
      String var2 = XMLUtil.elementToString(var1);
      SettingsMap var3 = new SettingsMap();

      for (String var5 : this.settingsData.getParams().keySet()) {
         if (!var5.equals("BlocksConfig")) {
            var3.set(var5, this.settingsData.getParams().get(var5));
         }
      }

      Element var6 = (Element)this.settingsData.getParams().get("BlocksConfig");
      Element var7 = this.configConverter.convertSimpleBuildingBlocks(var6, rng);
      var3.set("ConvertedBlocks", var7);
      if (this.buildMode.generationType.equals("genetic-evolution")) {
         return new GeneticBuildEngine(
            var3,
            this.projectName,
            this.progressEngine,
            this.initialPopulationDatabank,
            this.outputDatabank,
            this.lastGenerationDatabank,
            var1,
            var2,
            this.getCustomName().hashCode(),
            this.caMethod,
            this.caFilter,
            this.caInputArgs
         );
      } else if (this.buildMode.generationType.equals("random-generation")) {
         return new RandomBuildEngine(
            var3,
            this.tradingEngine,
            this.projectName,
            this.progressEngine,
            this.outputDatabank,
            var1,
            var2,
            this.getCustomName().hashCode(),
            this.caMethod,
            this.caFilter,
            this.caInputArgs
         );
      } else {
         throw new Exception(String.format("Unknown build mode: %s", this.buildMode.generationType));
      }
   }

   public String getType() {
      return "Build";
   }

   public String getName() {
      return L.tsq("Build strategies");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      BuildTask var3 = new BuildTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public String getPluginFolderName() {
      return "TaskBuild";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public String[] getSettings() {
      return new String[]{"Data", "Notes", "RiskMoneyManagement", "Rankings", "Options", "CrossChecks", "Blocks", "WhatToBuild", "PartsToImprove", "ATMs"};
   }

   protected int getRunningStatus() {
      return this.progressEngine.getRunningStatus();
   }

   protected Databank[] getUsedDatabanks() {
      return new Databank[]{this.outputDatabank};
   }

   protected Databank getOutputDatabank() {
      return this.outputDatabank;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      super.logTaskFinished(var1);
      ProjectRunInfo var2 = this.project.getTrackingInfo();
      String var3 = L.t("Strategies generated", new Object[0])
         + ": "
         + var2.totalJobsDone
         + ", "
         + L.t("Time per strategy", new Object[0])
         + ": "
         + SQUtils.formatDuration(var2.timePerStrategy)
         + ", "
         + L.t("Time per accepted strategy", new Object[0])
         + ": "
         + SQUtils.formatDuration(var2.timePerAcceptedStrategy)
         + ", "
         + L.t("Accepted", new Object[0])
         + ": "
         + var2.strategiesAccepted
         + ", "
         + L.t("Rejected", new Object[0])
         + ": "
         + var2.strategiesRejected
         + ", "
         + L.t("Strategies per hour", new Object[0])
         + ": "
         + this.formatIntOrDouble(var2.strategiesPerHour)
         + ", "
         + L.t("Accepted strategies per hour", new Object[0])
         + ": "
         + this.formatIntOrDouble(var2.acceptedStrategiesPerHour);
      var1.print(var3);
      var1.addDurationStats(var2);
      var1.addDismissalStats(var2, this);
      var1.addAcceptedStats(var2, this);
   }
}
