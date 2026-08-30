package com.strategyquant.tradinglib.project;

import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.memory.SQMemoryProtectionError;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.GeneticInfo;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.engine.BacktestEngine;
import com.strategyquant.tradinglib.file.SQFileManager;
import com.strategyquant.tradinglib.gp.GPEvolutionMessage;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.tradinglib.project.bestresults.BestResults;
import com.strategyquant.tradinglib.project.console.CLILogger;
import com.strategyquant.tradinglib.project.websocket.ProjectDataPublisher;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import com.strategyquant.tradinglib.taskImpl.TaskException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.StampedLock;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQProject {
   private static final Logger Log = LoggerFactory.getLogger(SQProject.class);
   public static final String MemoryProtectionError = "StrategyQuant detected too big memory consumption (over 85% of memory was used)\nand automatically stopped the project.\nIt is done to prevent program freezing, it should always operate with less than 85% of memory used.\n\nThe solution is to increase the maximum available memory in Performance settings or use configuration that uses less memory.";
   public static final String OutOfMemoryError = "StrategyQuant has run out of memory\nPlease increase the maximum available memory in Performance settings or use configuration that uses less memory.";
   public static final int RunFromFirstTask = 0;
   public static final int RunFromSpecificTask = 50;
   public static final int RunOnlySpecificTask = 100;
   public static final String userConfigFileName = "project.cfx";
   private static boolean useMemoryProtection;
   private String projectName;
   private ArrayList<ISQTask> tasks;
   private Databanks databanks;
   private Element configXml = null;
   private File projectConfigFile = null;
   private String projectFolder = null;
   private boolean projectRunning = false;
   private ISQTask lastRunningTask = null;
   private ProjectRunInfo projectRunInfo = new ProjectRunInfo();
   private ProjectProgress projectProgress = new ProjectProgress();
   private GeneticInfo geneticInfo = new GeneticInfo();
   public ProgressEngine progressEngine;
   public UserConfirm userConfirm;
   public ProjectLog log;
   private ProjectGlobalLog globalLog = null;
   public EngineCharts engineCharts;
   public BestResults bestResults;
   public ProjectDataPublisher publisher;
   public boolean correctionsMade = false;
   private StampedLock runInfoLock = new StampedLock();
   private int runCycles = 1;
   private long startTime;
   private long currentTaskStartTime;
   private long pauseDuration;
   private long lastPauseTime;
   private long currentTaskPauseDuration;
   private long currentTaskLastPauseTime;
   private int runSpecification = 0;
   private String runTask = null;
   private String lastEventMessage;
   private boolean hasUnresolvedResources = false;
   private IntList tasksList = new IntArrayList();

   public SQProject(String var1) throws Exception {
      this.checkProjectName(var1);
      var1 = var1.trim();
      this.projectName = var1;
      this.projectFolder = this.getProjectFolderPath(var1);
      this.projectConfigFile = new File(this.projectFolder + "project.cfx");
      this.tasks = new ArrayList<>();
      this.databanks = new Databanks(var1);
      this.progressEngine = new ProgressEngine(var1);
      this.userConfirm = new UserConfirm(var1);
      this.log = new ProjectLog();
      this.engineCharts = new EngineCharts(this);
      this.bestResults = new BestResults(this);
      this.publisher = new ProjectDataPublisher(this);
      this.initProject();
   }

   public static void init() {
      useMemoryProtection = Boolean.parseBoolean(MainApp.settings().get("memoryProtection", "false"));
   }

   private void initProject() throws Exception {
      File var1 = new File(this.projectFolder);
      if (!var1.exists() && !var1.mkdirs()) {
         throw new Exception(L.t("Cannot create Project directory.", new Object[0]));
      }

      if (!this.checkConfigFileExists()) {
         this.createDefaultConfigFile();
      } else {
         try {
            this.configXml = SQFileManager.loadProjectConfig(this.projectConfigFile);
         } catch (Exception var4) {
            Log.error("Config file of project '" + this.projectName + "' is corrupted");
            int var3 = MainApp.awaitUserConfirmation(
               L.t("Project error", new Object[0]),
               L.t("Config file of project '%s' is corrupted. Do you want to use default config?", new Object[]{this.projectName})
            );
            if (var3 != 1) {
               throw new Exception(L.t("Project config error", new Object[0]));
            }

            this.createDefaultConfigFile();
         }
      }

      this.initDatabanksFromConfig();
      this.updateProjectDatabanks();
      this.checkTasksConfig();
      this.checkUnresolvedResources();
      if (this.correctionsMade) {
         SQFileManager.updateProjectConfig(this.projectConfigFile, this.configXml);
      }
   }

   public Element checkUnresolvedResources() {
      this.hasUnresolvedResources = false;
      Element var1 = null;
      if (!ProjectEngine.isDefaulProject(this.projectName)) {
         try {
            var1 = ProjectResources.checkProjectResources(this.projectConfigFile, this.configXml);
            if (var1 != null) {
               if (var1.getChildren().size() > 0) {
                  this.setUnresolvedResources(true);
               } else {
                  var1 = null;
               }
            }
         } catch (Exception var3) {
            Log.error(String.format("Checking project '%s' resources failed. Exc.", this.getName()), var3);
         }
      }

      return var1;
   }

   private void checkTasksConfig() throws Exception {
      try {
         if (projectIsOlderThan136(this.configXml)) {
            this.transformProjectFile();
         }

         Element var1 = this.configXml.getChild("Tasks");

         for (Element var4 : var1.getChildren("Task")) {
            String var5 = XMLUtil.getStringAttr(var4, "taskXMLFile", null);
            Element var6 = SQFileManager.loadTaskConfig(this.projectConfigFile, var5);
            boolean var7 = false;
            if (var6.getChild("Databanks") == null) {
               String var8 = XMLUtil.getStringAttr(var4, "type", null);

               try {
                  ISQTask var9 = this.findTaskPluginByType(var8);
                  Element var10 = var9.getDefaultConfig("task_forex.xml");
                  Element var11 = var10.getChild("Settings");
                  Element var12 = var11 != null ? var11.getChild("Databanks") : null;
                  if (var12 != null) {
                     var6.addContent(var12.clone());
                     SQFileManager.updateTaskConfig(this.projectConfigFile, var5, var6);
                     Log.info("Project '{}', task '{}' - injected missing <Databanks> from default config.", this.projectName, var5);
                  }
               } catch (Exception var15) {
                  Log.error("Project '{}', task '{}' - could not inject default <Databanks>: {}", new Object[]{this.projectName, var5, var15.getMessage()});
               }
            }

            Element var17 = var6.getChild("CrossChecks");
            if (var17 != null) {
               Element var18 = var17.getChild("MonteCarloRetest");
               if (var18 != null && var18.getChild("Settings") != null) {
                  for (Element var20 : var18.getChild("Settings").getChild("Methods").getChildren("Method")) {
                     if (var20.getAttributeValue("type").equals("RandomizeHistoryData")) {
                        Element var21 = var20.getChild("Params").clone();
                        var21.removeContent();

                        for (Element var14 : var20.getChild("Params").getChildren("Param")) {
                           if (var14.getAttributeValue("key").equals("Probability")) {
                              var21.addContent(var14.clone().setAttribute("key", "ProbabilityUp"));
                              var21.addContent(var14.clone().setAttribute("key", "ProbabilityDown"));
                           } else if (var14.getAttributeValue("key").equals("MaxChange")) {
                              var21.addContent(var14.clone().setAttribute("key", "MaxChangeUp"));
                              var21.addContent(var14.clone().setAttribute("key", "MaxChangeDown"));
                           }
                        }

                        if (!var21.getChildren().isEmpty() && var20.removeChild("Params")) {
                           var20.addContent(var21);
                           var7 = true;
                        }
                     }
                  }

                  if (var7) {
                     SQFileManager.updateTaskConfig(this.projectConfigFile, var5, var6);
                  }
               }
            }
         }
      } catch (Exception var16) {
         Log.error(String.format("Checking project '%s' xml failed. Exc.", this.getName()), var16);
         throw var16;
      }
   }

   public static boolean projectIsOlderThan136(Element var0) {
      String var1 = null;

      try {
         var1 = var0.getAttributeValue("name");
         String var2 = XMLUtil.getStringAttr(var0, "version", null);
         if (var2 == null) {
            return true;
         }

         int var3 = Integer.parseInt(var2.substring(0, var2.indexOf(".")));
         return var3 < 136;
      } catch (Exception var4) {
         Log.error("Error while checking project file version of project " + var1, var4);
         return true;
      }
   }

   private void transformProjectFile() throws Exception {
      File var1 = new File(this.projectFolder + SQUtils.stripExtension(this.projectConfigFile.getName()) + "_backup." + "cfx");
      Files.copy(this.projectConfigFile.toPath(), var1.toPath(), StandardCopyOption.REPLACE_EXISTING);
      if (!this.projectConfigFile.delete()) {
         throw new Exception(L.t("Unable to delete old config file %s", new Object[]{this.projectConfigFile.getAbsolutePath()}));
      }

      SQFileManager.createV136ProjectFile(this);
      if (!var1.delete()) {
         Log.warn("Unable to delete old project backup file " + var1.getAbsolutePath());
      }
   }

   private boolean checkConfigFileExists() {
      if (!this.projectConfigFile.exists()) {
         File var1 = new File(this.projectFolder + "project.xml");
         if (!var1.exists()) {
            return false;
         }

         try {
            SQFileManager.xmlToCfx(var1, true);
         } catch (Exception var3) {
            Log.error("Error while converting xml file to cfx", var3);
            this.projectConfigFile = var1;
         }
      }

      return true;
   }

   public void initTasksFromConfig() throws Exception {
      this.tasks.clear();
      Element var1 = this.configXml.getChild("Tasks");
      List var2 = var1.getChildren("Task");
      boolean var3 = false;
      boolean var4 = false;

      for (Element var6 : var2) {
         String var7 = XMLUtil.getAttr(var6, "type");
         String var8 = XMLUtil.getAttr(var6, "name");
         if (this.runSpecification == 100) {
            if (this.runTask.equals(var8) && var7.equals("GoToTask")) {
               var4 = true;
            }
         } else if (this.runSpecification == 50) {
            if (!var3) {
               var3 = this.runTask.equals(var8);
            }

            if (var3 && var7.equals("GoToTask")) {
               var4 = true;
            }
         }
      }

      HashMap var19 = SQFileManager.loadTaskConfigs(this.projectConfigFile);
      int var20 = 1;
      var3 = false;

      for (Element var22 : var2) {
         String var9 = XMLUtil.getAttr(var22, "type");
         String var10 = XMLUtil.getAttr(var22, "name");
         String var11 = var22.getAttributeValue("title");
         String var12 = var22.getAttributeValue("taskXMLFile");
         boolean var13 = Boolean.parseBoolean(XMLUtil.tryGetAttr(var22, "active", "true"));
         Element var14 = (Element)var19.get(var12);
         ISQTask var15 = this.findTaskPluginByType(var9);
         boolean var16 = false;
         if (this.runSpecification == 100) {
            var16 = this.runTask.equals(var10);
            if (var4) {
               var16 = true;
            }
         } else if (this.runSpecification == 50) {
            if (!var3) {
               var3 = this.runTask.equals(var10);
            }

            if (var3) {
               var16 = var13;
            }

            if (var4 && var13) {
               var16 = true;
            }
         } else {
            var16 = var13;
         }

         if (this.hasUnresolvedResources) {
            var16 = false;
         }

         ISQTask var17 = var15.clone(this.projectName, this.progressEngine);
         var17.setActive(var13);
         var17.setCustomName(var10);
         var17.setConfigFileName(var12);
         var17.setTitle(var11);
         var17.setIndex(var20);
         var17.setConfig(var14, true, var16);
         this.tasks.add(var17);
         var20++;
      }
   }

   private void initDatabanksFromConfig() throws Exception {
      this.correctionsMade = this.databanks.loadFromConfig(this.configXml);
   }

   public DefaultProjectConfig getDefaultConfig() throws Exception {
      ArrayList var1 = new ArrayList();
      DefaultProjectConfig var2 = new DefaultProjectConfig();
      var2.projectConfig = this.createDefaultProjectXml();
      var2.taskConfig = null;
      IAppPlugin var3 = this.getAppPlugin();
      if (var3 != null && var3.getDefaultTaskType() != null && var3.getDefaultTaskName() != null) {
         for (ISQTask var5 : SQPluginManager.getPlugins(ISQTask.class)) {
            if (var5.getType().equals(var3.getDefaultTaskType())) {
               Element var6 = var5.getDefaultConfig("task_forex.xml");
               String var7 = var3.getDefaultTaskType();
               var6.setAttribute("type", var7);
               var6.setAttribute("name", var3.getDefaultTaskName());
               var2.taskXMLFileName = this.generateTaskXMLFileName(var7);
               var6.setAttribute("taskXMLFile", var2.taskXMLFileName);
               var2.taskConfig = var6.getChild("Settings").clone();
               var6.removeContent();
               var2.projectConfig.getChild("Tasks").addContent(var6);
               Element var8 = var2.projectConfig.getChild("Databanks");
               var8.removeContent();
               Element var9 = var2.taskConfig.getChild("Databanks");

               for (Element var11 : var9.getChildren("Databank")) {
                  Element var12 = new Element("Databank");
                  String var13 = var11.getAttributeValue("value");
                  if (!var1.contains(var13)) {
                     var1.add(var13);
                     var12.setAttribute("name", var11.getAttributeValue("value"));
                     var12.setAttribute("view", "Default - Main data");
                     var12.setAttribute("syncType", "Auto-sync never");
                     var8.addContent(var12);
                  }
               }
               break;
            }
         }
      }

      return var2;
   }

   public String generateTaskXMLFileName(String var1) {
      String var2 = var1 + "-Task";
      String var3 = ".xml";
      ArrayList var4 = new ArrayList();

      for (ISQTask var6 : this.tasks) {
         var4.add(var6.getTaskXMLFileName());
      }

      for (int var7 = 1; var7 < 1000; var7++) {
         String var8 = var2 + var7 + var3;
         if (!var4.contains(var8)) {
            return var8;
         }
      }

      return var2 + System.currentTimeMillis() + var3;
   }

   private void createDefaultConfigFile() throws Exception {
      DefaultProjectConfig var1 = this.getDefaultConfig();
      SQFileManager.createV136ProjectFile(this.projectConfigFile, var1);
      this.configXml = var1.projectConfig;
   }

   private IAppPlugin getAppPlugin() {
      for (IAppPlugin var2 : SQPluginManager.getPlugins(IAppPlugin.class)) {
         String var3 = var2.getProject();
         if (var3 != null && var3.equals(this.projectName)) {
            return var2;
         }
      }

      return null;
   }

   private Element createDefaultProjectXml() {
      Element var1 = new Element("Project");
      var1.setAttribute("name", this.projectName);
      var1.setAttribute("version", MainApp.getAppVersion());
      var1.addContent(new Element("Tasks"));
      Element var2 = new Element("Databanks");
      var2.addContent(Databank.getDefaultConfig());
      var1.addContent(var2);
      return var1;
   }

   public ISQTask addTask(String var1, String var2) throws Exception {
      return this.addTask(var1, var2, -1);
   }

   public ISQTask addTask(String var1, String var2, int var3) throws Exception {
      this.checkProjectStatus("add");
      if (this.containsTask(var1)) {
         return null;
      }

      ISQTask var4 = this.findTaskPluginByType(var2);
      String var5 = this.generateTaskXMLFileName(var2);
      Element var6 = var4.getDefaultConfig("task_forex.xml");
      var6.setAttribute("name", var1);
      var6.setAttribute("type", var2);
      var6.setAttribute("taskXMLFile", var5);
      Element var7 = var6.getChild("Settings").clone();
      var4.setConfig(var7, false, true);
      var4.setCustomName(var1);
      var4.setConfigFileName(var5);
      var6.removeContent();
      Element var8 = this.configXml.getChild("Tasks");
      if (var3 >= 0) {
         var8.addContent(var3, var6);
         this.tasks.add(var3, var4);
      } else {
         var8.addContent(var6);
         this.tasks.add(var4);
      }

      this.updateProjectDatabanks();
      this.saveConfig();
      return var4;
   }

   public ISQTask removeTask(String var1) throws Exception {
      this.checkProjectStatus("remove");
      Element var2 = this.configXml.getChild("Tasks");
      List var3 = var2.getChildren("Task");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         String var5 = ((Element)var3.get(var4)).getAttributeValue("name");
         if (var5.equals(var1)) {
            var3.remove(var4);
            break;
         }
      }

      ISQTask var7 = null;

      for (int var8 = this.tasks.size() - 1; var8 >= 0; var8--) {
         ISQTask var6 = this.tasks.get(var8);
         if (var6.getCustomName().equals(var1)) {
            var7 = this.tasks.remove(var8);
            break;
         }
      }

      this.updateProjectDatabanks();
      this.saveConfig();
      return var7;
   }

   public void checkProjectStatus(String var1) throws Exception {
      if (this.getRunningStatus() != 0
         && this.getRunningStatus() != 100
         && this.getRunningStatus() != 3
         && this.getRunningStatus() != 4
         && this.getRunningStatus() != 50) {
         throw new Exception(L.t("Cannot %s task while project is running", new Object[]{var1}));
      }
   }

   private void updateProjectDatabanks() {
      ArrayList var1 = new ArrayList();
      Element var2 = this.configXml.getChild("Tasks");
      Element var3 = this.configXml.getChild("Databanks");
      Element var4 = new Element("Databanks");

      for (Element var6 : var3.getChildren("Databank")) {
         String var7 = var6.getAttributeValue("name");
         if (!var1.contains(var7)) {
            var1.add(var7);
            var4.addContent(var6.clone());
         }
      }

      for (Element var17 : var2.getChildren("Task")) {
         String var18 = var17.getAttributeValue("type");

         try {
            ISQTask var8 = this.findTaskPluginByType(var18);
            Element var9 = var8.getDefaultConfig("task_forex.xml");
            Element var10 = var9.getChild("Settings").getChild("Databanks");
            if (var10 != null) {
               for (Element var12 : var10.getChildren("Databank")) {
                  String var13 = var12.getAttributeValue("value");
                  if (var13 != null && !var1.contains(var13)) {
                     var1.add(var13);
                     Element var14 = new Element("Databank");
                     var14.setAttribute("name", var13);
                     var14.setAttribute("view", "Default");
                     var14.setAttribute("syncType", "Auto-sync never");
                     var4.addContent(var14);
                     this.databanks.add(var13);
                     this.correctionsMade = true;
                  }
               }
            }
         } catch (Exception var15) {
            Log.debug("Cannot read databank settings of task '" + var17.getAttributeValue("name") + "'", var15);
         }
      }

      if (!var1.contains("Results")) {
         var4.addContent(Databank.getDefaultConfig());
         this.databanks.add("Results");
         this.correctionsMade = true;
      }

      this.databanks.updatePositions();
      this.configXml.removeContent(var3);
      this.configXml.addContent(var4);
   }

   public void moveTask(String var1, int var2) throws Exception {
      this.checkProjectStatus("move");
      Element var3 = this.configXml.getChild("Tasks");
      List var4 = var3.getChildren("Task");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = ((Element)var4.get(var5)).clone();
         String var7 = var6.getAttributeValue("name");
         if (var7.equals(var1)) {
            if (var5 == var2) {
               return;
            }

            var4.remove(var5);
            if (var5 > var2) {
               var4.add(var2, var6);
            } else {
               var4.add(var2 - 1, var6);
            }
            break;
         }
      }

      this.saveConfig();
   }

   public boolean containsTask(String var1) {
      Element var2 = this.configXml.getChild("Tasks");

      for (Element var5 : var2.getChildren("Task")) {
         String var6 = var5.getAttributeValue("name");
         if (var6.equals(var1)) {
            return true;
         }
      }

      return false;
   }

   public Element getTaskConfigByName(String var1) {
      Element var2 = this.configXml.getChild("Tasks");

      for (Element var5 : var2.getChildren("Task")) {
         String var6 = var5.getAttributeValue("name");
         if (var6.equals(var1)) {
            return var5;
         }
      }

      return null;
   }

   public Element getTaskSettingsByName(String var1) {
      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         if (var3.getCustomName().equals(var1)) {
            return var3.getConfig();
         }
      }

      return null;
   }

   public Element getTaskSettingsByTitle(String var1) {
      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         if (var3.getTitle().equals(var1)) {
            return var3.getConfig();
         }
      }

      return null;
   }

   public ISQTask getTaskByXMLFile(String var1) {
      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         if (var3.getTaskXMLFileName().equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public Element getTaskSettingsByXMLFile(String var1) {
      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         if (var3.getTaskXMLFileName().equals(var1)) {
            return var3.getConfig();
         }
      }

      return null;
   }

   public int getTaskElementIndex(String var1) {
      Element var2 = this.configXml.getChild("Tasks");
      List var3 = var2.getChildren("Task");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         String var6 = var5.getAttributeValue("name");
         if (var6.equals(var1)) {
            return var4;
         }
      }

      return -1;
   }

   public int getTasksCount() {
      Element var1 = this.configXml.getChild("Tasks");
      List var2 = var1.getChildren("Task");
      return var2.size();
   }

   public Databank addDatabank(String var1, int var2) throws Exception {
      if (this.databanks.containsKey(var1)) {
         return this.databanks.get(var1);
      }

      Databank var3 = this.databanks.add(var1);
      var3.setPosition(var2);
      Element var4 = this.configXml.getChild("Databanks");
      Element var5 = var3.getConfig();
      var4.addContent(var5);
      this.saveConfig();
      return var3;
   }

   public boolean removeDatabank(String var1) throws Exception {
      if (!this.databanks.containsKey(var1)) {
         return false;
      }

      Databank var2 = this.databanks.remove(var1);
      var2.clearRecords(true, false, String.format("Remove databank %s/%s.", this.projectName, var1));
      File var3 = new File(var2.getDatabankFolder());
      SQUtils.deleteRecursive(var3);
      var3.delete();
      Element var4 = this.configXml.getChild("Databanks");
      List var5 = var4.getChildren("Databank");

      for (Element var7 : var5) {
         if (var1.equals(var7.getAttributeValue("name"))) {
            var5.remove(var7);
            break;
         }
      }

      this.saveConfig();
      return true;
   }

   public boolean containsDatabank() {
      try {
         ArrayList var1 = this.listDatabanks();
         return !var1.isEmpty();
      } catch (Exception var2) {
         return false;
      }
   }

   public Databank getResultsDatabank() {
      try {
         return this.databanks.get("Results");
      } catch (Exception var2) {
         return null;
      }
   }

   private ArrayList<String> listDatabanks() throws Exception {
      ArrayList var1 = new ArrayList();
      Element var2 = XMLUtil.getChildElem(this.configXml, "Databanks");

      for (Element var5 : var2.getChildren("Databank")) {
         String var6 = var5.getAttributeValue("name");
         var1.add(var6);
      }

      return var1;
   }

   public boolean isRunning() {
      return this.projectRunning;
   }

   public String getActiveTaskName() {
      return this.isRunning() && this.lastRunningTask != null ? this.lastRunningTask.getCustomName() : null;
   }

   public ISQTask getLastRunningTask() {
      return this.lastRunningTask;
   }

   public void saveConfig() throws Exception {
      if (this.configXml.getAttributeValue("version") == null) {
         this.configXml.setAttribute("version", MainApp.getAppVersion());
      }

      SQFileManager.updateWholeProjectConfig(this);
   }

   private ISQTask findTaskPluginByType(String var1) throws Exception {
      for (ISQTask var3 : SQPluginManager.getPlugins(ISQTask.class)) {
         if (var3.getType().equals(var1)) {
            return var3.clone(this.projectName, this.progressEngine);
         }
      }

      throw new Exception(L.t("Task '%s' does not exist.", new Object[]{var1}));
   }

   public ISQTask getTaskByName(String var1) throws Exception {
      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         if (var3.getCustomName().equals(var1)) {
            return var3;
         }
      }

      throw new Exception(L.t("Task '%s' does not exist.", new Object[]{var1}));
   }

   public boolean containsTaskByName(String var1) throws Exception {
      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         if (var3.getCustomName().equals(var1)) {
            return true;
         }
      }

      return false;
   }

   public ISQTask getTaskByHash(int var1) throws Exception {
      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         if (var3.getCustomName().hashCode() == var1) {
            return var3;
         }
      }

      throw new Exception(L.t("Task with hash '%s' does not exist.", new Object[]{var1}));
   }

   public ISQTask getTask(int var1) throws Exception {
      return this.tasks.get(var1);
   }

   public IntList getTasksHashes() throws Exception {
      this.tasksList.clear();
      Element var1 = this.configXml.getChild("Tasks");
      List var2 = var1.getChildren("Task");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         this.tasksList.add(((Element)var2.get(var3)).getAttributeValue("name").hashCode());
      }

      return this.tasksList;
   }

   private int getIndexOfTask(String var1) throws Exception {
      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         if (var3.getCustomName().equals(var1)) {
            return var2;
         }
      }

      throw new Exception(L.t("Task '%s' does not exist.", new Object[]{var1}));
   }

   public void start() throws Exception {
      if (MainApp.v571hfnsHw().eHvc2JguAd() == null) {
         throw new Exception(L.t("Missing license.", new Object[0]));
      }

      this.resetTracking(false, true);
      this.progressEngine.reset();
      this.publisher.resetLastData("engine-channel");
      this.globalLog = null;
      Log.info("=========== Project started ===========");

      for (int var1 = 0; var1 < this.tasks.size(); var1++) {
         ISQTask var2 = this.tasks.get(var1);
         if (this.tasks.get(var1).getSettingsData().hasErrors()) {
            this.progressEngine.setStatus(50);
            if (this.isCustomProject()) {
               this.projectRunInfo.errorInfo = L.t(
                  "Cannot start project '%s', it has config errors in task '%s'.", new Object[]{this.getName(), var2.getTitle()}
               );
            } else {
               this.projectRunInfo.errorInfo = L.t("Cannot start project '%s', it has config errors.", new Object[]{this.getName()});
            }

            this.projectRunInfo.error = this.tasks.get(var1).getErrorsDescription();
            this.projectRunInfo.errorType = 50;
            throw new Exception(this.projectRunInfo.errorInfo + "\n" + this.projectRunInfo.error);
         }
      }

      if (this.databanks.isEmpty()) {
         throw new Exception(L.t("No databanks found.", new Object[0]));
      }

      if (this.tasks.isEmpty()) {
         throw new Exception(L.t("No tasks found.", new Object[0]));
      }

      boolean var3 = false;

      for (int var4 = 0; var4 < this.tasks.size(); var4++) {
         if (this.tasks.get(var4).isActive()) {
            var3 = true;
            break;
         }
      }

      if (!var3) {
         throw new Exception(L.t("No active task found.", new Object[0]));
      }

      this.runCycles = 1;
      this.startTime = System.currentTimeMillis();
      this.pauseDuration = 0L;
      if (MainApp.runInConsoleWithoutActiveWebserver() && !MainApp.runInConsoleInInteractiveMode()) {
         this.performStartProject();
      } else {
         (new Thread() {
            @Override
            public void run() {
               SQProject.this.performStartProject();
            }
         }).start();
      }
   }

   private void waitForTaskFinish() {
      while (!this.progressEngine.isStopped()) {
         try {
            Thread.sleep(200L);
         } catch (InterruptedException var2) {
            Log.info("Interrupted exception", var2);
         }
      }
   }

   public void stop() {
      if (this.progressEngine.getRunningStatus() != 6 && this.progressEngine.getRunningStatus() != 4) {
         this.progressEngine.stop();
      } else {
         this.publisher.resetLastData("engine-channel");
      }
   }

   public void pause() {
      if (this.progressEngine.getRunningStatus() != 5 && this.progressEngine.getRunningStatus() != 2) {
         this.lastPauseTime = System.currentTimeMillis();
         this.currentTaskLastPauseTime = System.currentTimeMillis();
         this.progressEngine.pause();
      } else {
         this.publisher.resetLastData("engine-channel");
      }
   }

   public void resume() {
      if (this.progressEngine.getRunningStatus() != 1) {
         this.pauseDuration = this.pauseDuration + (System.currentTimeMillis() - this.lastPauseTime);
         this.currentTaskPauseDuration = this.currentTaskPauseDuration + (System.currentTimeMillis() - this.currentTaskLastPauseTime);
         this.progressEngine.resume();
      } else {
         this.publisher.resetLastData("engine-channel");
      }
   }

   public void checkPaused() {
      this.progressEngine.checkPaused();
   }

   public void printToLog(String var1) {
      this.progressEngine.printToLog(var1);
   }

   public int getRunningStatus() {
      return this.progressEngine.getRunningStatus();
   }

   public void resetTracking(boolean var1, boolean var2) {
      this.projectRunInfo.jobExceptionsCount = 0;
      this.projectRunInfo.totalJobsDone = 0L;
      this.projectRunInfo.runningJobs = 0L;
      this.projectRunInfo.waitingJobs = 0L;
      if (var2) {
         this.projectRunInfo.startTime = System.currentTimeMillis();
         this.projectRunInfo.projectRunningTime = 0L;
         this.projectRunInfo.totalProjectRunningTime = 0L;
      }

      this.currentTaskStartTime = 0L;
      this.currentTaskPauseDuration = 0L;
      this.projectRunInfo.taskRunningTime = 0L;
      this.projectRunInfo.backtestsPerformed = 0L;
      this.projectRunInfo.strategiesGenerated = 0L;
      this.projectRunInfo.strategiesRejected = 0L;
      this.projectRunInfo.strategiesRejectedPct = 0.0;
      this.projectRunInfo.strategiesAccepted = 0L;
      this.projectRunInfo.strategiesAcceptedPct = 0.0;
      this.projectRunInfo.timePerStrategy = 0L;
      this.projectRunInfo.timePerAcceptedStrategy = 0L;
      this.projectRunInfo.strategiesPerHour = 0.0;
      this.projectRunInfo.acceptedStrategiesPerHour = 0.0;
      this.projectRunInfo.strategiesPassed = 0L;
      this.projectRunInfo.strategiesFailed = 0L;
      this.projectRunInfo.infiniteProgress = var1;
      this.projectRunInfo.error = null;
      this.projectRunInfo.errorInfo = null;
      this.projectRunInfo.errorType = 0;
      this.projectRunInfo.progressPercent = 0.0;
      this.projectRunInfo.resetAcceptedCounts();
      this.projectRunInfo.resetDismissalCounts();
      this.projectRunInfo.resetDurationStats();
   }

   public void trackJob(JobDetails var1, int var2, DurationStats var3, int var4, long var5, long var7) {
      this.projectRunInfo.totalJobsDone++;
      if (var1 != null && !var1.isSuccess()) {
         this.projectRunInfo.jobExceptionsCount++;
      }

      this.projectRunInfo.backtestsPerformed = BacktestEngine.backtestsCount.get();
      this.projectRunInfo.runningJobs = var5;
      this.projectRunInfo.waitingJobs = var7;
      this.projectRunInfo.strategiesGenerated = this.projectRunInfo.totalJobsDone;
      if (var2 == 10007 || var2 == 10006) {
         this.projectRunInfo.strategiesAccepted++;
         this.projectRunInfo.increaseAcceptedReasonCount(var2);
      } else if (var2 > 0) {
         this.projectRunInfo.strategiesRejected++;
         this.projectRunInfo.increaseDismissalReasonCount(var2);
      } else {
         this.projectRunInfo.strategiesAccepted++;
         this.projectRunInfo.increaseAcceptedReasonCount(var2);
      }

      this.projectRunInfo.strategiesRejectedPct = this.computePct(this.projectRunInfo.strategiesRejected, this.projectRunInfo.strategiesGenerated);
      this.projectRunInfo.strategiesAcceptedPct = this.computePct(this.projectRunInfo.strategiesAccepted, this.projectRunInfo.strategiesGenerated);
      this.projectRunInfo.projectRunningTime = this.getProjectRunningTime();
      this.projectRunInfo.totalProjectRunningTime = this.getTotalProjectRunningTime();
      this.projectRunInfo.taskRunningTime = this.getCurrentTaskRunningTime();
      if (this.projectRunInfo.totalJobsDone > 0L) {
         this.projectRunInfo.timePerStrategy = this.projectRunInfo.taskRunningTime / this.projectRunInfo.totalJobsDone;
      } else {
         this.projectRunInfo.timePerStrategy = 0L;
      }

      if (this.projectRunInfo.strategiesAccepted > 0L) {
         this.projectRunInfo.timePerAcceptedStrategy = this.projectRunInfo.taskRunningTime / this.projectRunInfo.strategiesAccepted;
      } else {
         this.projectRunInfo.timePerAcceptedStrategy = 0L;
      }

      this.projectRunInfo.strategiesPerHour = 3600000.0 * this.projectRunInfo.totalJobsDone / this.projectRunInfo.taskRunningTime;
      this.projectRunInfo.acceptedStrategiesPerHour = 3600000.0 * this.projectRunInfo.strategiesAccepted / this.projectRunInfo.taskRunningTime;
      if (var3 != null) {
         this.projectRunInfo.updateDurationStats(var3);
      }
   }

   public void increaseGlobalStats(int var1, String var2, long var3, String var5, long var6) {
      if (this.globalLog != null) {
         this.globalLog.increaseStats(var1, var2, var3, var5, var6);
      }
   }

   public void setGlobalStats(String var1, int var2, String var3, long var4, int var6, String var7) {
      if (this.globalLog != null) {
         this.globalLog.setStats(var1, var2, var3, var4, var6, var7);
      }
   }

   public void setGlobalStats(String var1, int var2, String var3, long var4, int var6) {
      if (this.globalLog != null) {
         this.globalLog.setStats(var1, var2, var3, var4, var6, "");
      }
   }

   public void increaseGlobalStats(String var1, int var2, String var3, long var4, int var6) {
      if (this.globalLog != null) {
         this.globalLog.increaseStats(var1, var2, var3, var4, var6);
      }
   }

   private double computePct(double var1, double var3) {
      return var3 == 0.0 ? 0.0 : var1 / var3;
   }

   public double getFailedJobsPercentage() {
      long var1 = this.projectRunInfo.totalJobsDone;
      int var3 = this.projectRunInfo.jobExceptionsCount;
      return var1 < 3L ? 0.0 : var3 / var1;
   }

   public void loadTrackingInfo(ProjectRunInfo var1, boolean var2) {
      long var3 = this.runInfoLock.readLock();

      try {
         var1.copyFrom(this.projectRunInfo, var2);
      } finally {
         this.runInfoLock.unlock(var3);
      }
   }

   public void updateTrackingInfo(ProjectRunInfo var1) {
      long var2 = this.runInfoLock.writeLock();

      try {
         this.projectRunInfo.copyFrom(var1, false);
      } finally {
         this.runInfoLock.unlock(var2);
      }
   }

   public ProjectRunInfo getTrackingInfo() {
      long var1 = this.runInfoLock.readLock();

      try {
         return this.projectRunInfo;
      } finally {
         this.runInfoLock.unlock(var1);
      }
   }

   public void increaseTrackingInfoStep(int var1) {
      long var2 = this.runInfoLock.writeLock();

      try {
         this.projectRunInfo.step++;
         if (var1 == 0) {
            this.projectRunInfo.progressPercent = 100.0;
         } else {
            this.projectRunInfo.progressPercent = (double)this.projectRunInfo.step / var1 * 100.0;
         }
      } finally {
         this.runInfoLock.unlock(var2);
      }
   }

   public void setTrackingInfoReasonToDismiss(int var1, String var2) {
      long var3 = this.runInfoLock.writeLock();

      try {
         this.projectRunInfo.getDismissalReasons().put(var1, var2);
      } finally {
         this.runInfoLock.unlock(var3);
      }
   }

   public String getProjectFolderPath(String var1) {
      return SQPaths.projectsDirPath + "/" + var1 + "/";
   }

   public boolean isAppProject() {
      return this.getAppPlugin() != null;
   }

   private boolean isCustomProject() {
      return !this.isAppProject();
   }

   public Element getConfig() {
      if (this.configXml.getAttributeValue("version") == null) {
         this.configXml.setAttribute("version", MainApp.getAppVersion());
      }

      return this.configXml;
   }

   public void updateConfig(Element var1) throws Exception {
      this.configXml = var1;
      SQFileManager.updateProjectConfig(this);
   }

   public Databanks getDatabanks() {
      return this.databanks;
   }

   public String getName() {
      return this.projectName;
   }

   public String getProjectFolder() {
      String var1 = this.projectFolder;
      Log.debug("Project log folder is '{}'", var1);
      return var1;
   }

   public void beforeStart() throws Exception {
      if (this.projectName.equals("Optimizer")) {
         Databank var1 = this.databanks.get("Results");
         ArrayList var2 = ProjectConfigHelper.getOptimizationStrategies(this, var1);
         if (var2 == null && var1.size() > 0 || var2 != null && var1.size() > var2.size()) {
            this.userConfirm
               .awaitConfirmation(
                  L.t("Databank not empty", new Object[0]), L.t("Do you want to remove old optimization results from Results databank?", new Object[0])
               );
            if (this.userConfirm.isConfirmed()) {
               var1.removeAll(true, "Optimizer - before start, user confirmed to remove old optimization results from Results databank.");
            }
         }
      }
   }

   public void confirm(boolean var1) {
      this.userConfirm.setConfirmed(var1);
   }

   public ProjectProgress getProgress() {
      return this.projectProgress;
   }

   public void increaseFailedJobsCount() {
      long var1 = this.runInfoLock.writeLock();

      try {
         this.projectRunInfo.jobExceptionsCount++;
      } finally {
         this.runInfoLock.unlock(var1);
      }
   }

   public void increaseStrategiesFailed() {
      long var1 = this.runInfoLock.writeLock();

      try {
         this.projectRunInfo.strategiesFailed++;
      } finally {
         this.runInfoLock.unlock(var1);
      }
   }

   public void increaseStrategiesPassed() {
      long var1 = this.runInfoLock.writeLock();

      try {
         this.projectRunInfo.strategiesPassed++;
      } finally {
         this.runInfoLock.unlock(var1);
      }
   }

   public GeneticInfo getGeneticInfo() {
      return this.geneticInfo;
   }

   public void resetGeneticInfo(boolean var1, int var2) {
      this.geneticInfo.reset(var1, var2);
      this.engineCharts.fitnessEvolution.reset(var1, var2);
   }

   public void updateGeneticInfo(GPEvolutionMessage var1, boolean var2) {
      this.geneticInfo.update(var1);
      this.engineCharts.fitnessEvolution.addNextIslansEvodData(this.geneticInfo.getIslandsEvoData(), var2);
   }

   public void setTaskTitle(String var1, String var2) throws Exception {
      this.checkProjectStatus("rename");
      Element var3 = this.configXml.getChild("Tasks");
      List var4 = var3.getChildren("Task");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("name");
         if (var7.equals(var1)) {
            var6.setAttribute("title", var2);
            break;
         }
      }

      this.saveConfig();
   }

   public void setTaskActive(String var1, boolean var2) throws Exception {
      this.checkProjectStatus("set the status of");
      Element var3 = this.configXml.getChild("Tasks");
      List var4 = var3.getChildren("Task");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("name");
         if (var7.equals(var1)) {
            var6.setAttribute("active", var2 + "");
            break;
         }
      }

      this.saveConfig();
   }

   public ISQTask cloneTask(String var1, String var2) throws Exception {
      this.checkProjectStatus("clone");
      int var3 = this.getTaskElementIndex(var1);
      if (var3 < 0) {
         throw new Exception(L.t("Cannot clone task.", new Object[0]));
      }

      Element var4 = this.configXml.getChild("Tasks");
      ISQTask var5 = this.tasks.get(var3);
      Element var6 = (Element)var4.getChildren().get(var3);
      String var7 = XMLUtil.getAttr(var6, "type");
      String var8 = this.findTaskNameByType(var7);
      String var9 = var8 + " " + 1;

      for (int var10 = 1; var10 < 100 && this.containsTask(var9); var10++) {
         var9 = var8 + " " + var10;
      }

      String var15 = XMLUtil.getAttr(var6, "title", var1) + " - Cloned";
      String var11 = this.generateTaskXMLFileName(var7);
      Element var12 = var5.getConfig().clone();
      ISQTask var13 = var5.clone(this.projectName, this.progressEngine);
      var13.setCustomName(var9);
      var13.setConfigFileName(var11);
      var13.setTitle(var15);
      var13.setIndex(var3);
      var13.setConfig(var12, false, true);
      var13.setActive(true);
      Element var14 = var6.clone();
      var14.setAttribute("name", var9);
      var14.setAttribute("title", var15);
      var14.setAttribute("taskXMLFile", var11);
      if (var2 != null && !var2.equals("end")) {
         var4.addContent(var3 + 1, var14);
         this.tasks.add(var3, var13);
      } else {
         var4.addContent(var14);
         this.tasks.add(var13);
      }

      this.saveConfig();
      return var13;
   }

   private String findTaskNameByType(String var1) {
      for (ISQTask var3 : SQPluginManager.getPlugins(ISQTask.class)) {
         if (var3.getType().equals(var1)) {
            return var3.getName();
         }
      }

      return null;
   }

   public void onMemoryError(Error var1) {
      boolean var2 = var1 instanceof SQMemoryProtectionError;
      this.projectRunInfo.errorType = 50;
      this.projectRunInfo.errorInfo = "Out of memory error";
      this.projectRunInfo.error = var2
         ? "StrategyQuant detected too big memory consumption (over 85% of memory was used)\nand automatically stopped the project.\nIt is done to prevent program freezing, it should always operate with less than 85% of memory used.\n\nThe solution is to increase the maximum available memory in Performance settings or use configuration that uses less memory."
         : "StrategyQuant has run out of memory\nPlease increase the maximum available memory in Performance settings or use configuration that uses less memory.";
      this.progressEngine.error();
      String var3 = "Project stopped - " + (var1 != null ? var1.getMessage() : "Exceeded memory limit");
      Log.error(var3);
      this.progressEngine.printToLog(var3);
   }

   public static boolean isMemoryProtectionUsed() {
      return useMemoryProtection;
   }

   public void setProjectName(String var1) throws Exception {
      this.checkProjectName(var1);
      var1 = var1.trim();
      File var2 = new File(this.projectFolder);
      File var3 = new File(this.getProjectFolderPath(var1));
      Files.move(var2.toPath(), var3.toPath());
      this.projectName = var1;
      this.projectFolder = this.getProjectFolderPath(var1);
      this.projectConfigFile = new File(this.projectFolder + "project.cfx");
      this.databanks.setProjectName(var1);
      this.progressEngine.setProjectName(var1);
      this.userConfirm.setProjectName(var1);
      this.configXml.setAttribute("name", var1);
      this.saveConfig();
   }

   private void checkProjectName(String var1) throws Exception {
      if (var1 == null) {
         throw new Exception("Project name cannot be empty.");
      }

      String var2 = var1.trim();
      if (var2.isEmpty()) {
         throw new Exception("Project name cannot be empty.");
      }

      if (var2.startsWith(".")) {
         throw new Exception("Project name cannot start with dot.");
      }

      if (var2.matches(".*[\\\\/:*?\"<>|].*")) {
         throw new Exception("Project name contains forbidden characters: \\ / : * ? \" < > |");
      }

      if (var2.matches("^(?i)(nul|prn|con|lpt[0-9]|com[0-9])(\\.|$).*")) {
         throw new Exception("Project name uses a reserved system name.");
      }

      for (int var3 = 0; var3 < var2.length(); var3++) {
         char var4 = var2.charAt(var3);
         if (Character.isWhitespace(var4) && var4 != ' ') {
            throw new Exception("Project name cannot contain tabs or new lines.");
         }
      }
   }

   public File getProjectConfigFile() {
      return this.projectConfigFile;
   }

   public void setRunSpecification(int var1) {
      this.setRunSpecification(var1, null);
   }

   public void setRunSpecification(int var1, String var2) {
      this.runSpecification = var1;
      this.runTask = var2;
   }

   public int getRunCycles() {
      return this.runCycles;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public long getTotalProjectRunningTime() {
      return System.currentTimeMillis() - this.startTime;
   }

   public long getProjectRunningTime() {
      return System.currentTimeMillis() - this.startTime - this.pauseDuration;
   }

   public long getCurrentTaskRunningTime() {
      if (this.currentTaskStartTime == 0L) {
         this.currentTaskStartTime = System.currentTimeMillis();
      }

      return System.currentTimeMillis() - this.currentTaskStartTime - this.currentTaskPauseDuration;
   }

   public String printGlobalLog() throws Exception {
      return this.globalLog == null ? "" : this.globalLog.load();
   }

   public TaskStats getTaskStats(int var1) {
      return this.globalLog == null ? null : this.globalLog.getTaskStats(var1);
   }

   public void setLastEvent(String var1) {
      this.lastEventMessage = var1;
   }

   public String getLastEvent() {
      return this.lastEventMessage;
   }

   public JSONObject getInfo() {
      boolean var1 = false;
      boolean var2 = false;
      boolean var3 = false;
      boolean var4 = false;
      Element var5 = this.getConfig().getChild("Tasks");
      List var6 = var5.getChildren("Task");

      for (int var7 = 0; var7 < var6.size(); var7++) {
         try {
            Element var8 = ((Element)var6.get(var7)).getChild("Settings").getChild("WhatToBuild").getChild("BuildMode");
            if (var8.getAttributeValue("generationType").equals("genetic-evolution")) {
               if (XMLUtil.getInt(var8, "InitGenerationType", 1) == 2) {
                  var4 = true;
               }

               if (XMLUtil.getBoolean(var8, "ShowLastGenerationDatabank", false)) {
                  var3 = true;
               }
            }
         } catch (Exception var12) {
         }
      }

      ArrayList var13 = this.getDatabanks().list();
      int var14 = 0;
      int var9 = 0;

      for (int var10 = 0; var10 < var13.size(); var10++) {
         Databank var11 = (Databank)var13.get(var10);
         if ((var1 || !var11.getName().equals("Strategies to optimize"))
            && (var2 || !var11.getName().equals("Strategies to improve"))
            && (var3 || !var11.getName().equals("Last generation"))
            && (var4 || !var11.getName().equals("Initial population"))) {
            var9++;
            var14 += var11.size();
         }
      }

      return new JSONObject()
         .put("projectName", this.getName())
         .put("filePath", this.projectConfigFile.getAbsolutePath())
         .put("tasks", this.getTasksCount())
         .put("activeTask", this.getActiveTaskName())
         .put("databanks", var9)
         .put("strategies", var14)
         .put("hasUnresolvedResources", this.hasUnresolvedResources);
   }

   public ProjectGlobalLog getGlobalLog() {
      return this.globalLog;
   }

   public ArrayList<ISQTask> getTasks() {
      return this.tasks;
   }

   public String toJSON() {
      JsonCreator var1 = new JsonCreator();
      var1.beginObject();
      var1.put("name", this.projectName, true);
      var1.put("xml", this.fixXMLCode(XMLUtil.elementToString(this.configXml)), true);
      var1.putBeginObject("tasks");

      for (int var2 = 0; var2 < this.tasks.size(); var2++) {
         ISQTask var3 = this.tasks.get(var2);
         var1.put(var3.getTaskXMLFileName(), this.fixXMLCode(XMLUtil.elementToString(var3.getConfig())), var2 < this.tasks.size() - 1);
      }

      var1.endObject(false);
      var1.endObject(false);
      return var1.toJson();
   }

   private String fixXMLCode(String var1) {
      String var2 = SQUtils.replaceAll(var1, "\\", "\\\\");
      var2 = SQUtils.replaceAll(var2, "/", "\\/");
      var2 = SQUtils.replaceAll(var2, "\r\n", "\\r\\n");
      return SQUtils.replaceAll(var2, "\"", "\\\"");
   }

   public int getRunSpecification() {
      return this.runSpecification;
   }

   private void performStartProject() {
      try {
         if (!MainApp.v571hfnsHw().gDmtOfRJBr() && this.isCustomProject()) {
            throw new Exception(MainApp.OnlyInProVersionTitle);
         }

         int var1 = 0;
         if (this.runSpecification == 50) {
            var1 = this.getIndexOfTask(this.runTask);
         }

         if (this.isCustomProject()) {
            this.globalLog = new ProjectGlobalLog(this.getProjectFolder());
         }

         for (; var1 < this.tasks.size(); var1++) {
            this.currentTaskPauseDuration = 0L;
            this.currentTaskLastPauseTime = 0L;

            try {
               if (this.progressEngine.isStoppedNotFinished()) {
                  Log.debug("Project stopped.");
                  break;
               }

               if (!this.tasks.get(var1).isActive() && this.runSpecification != 100) {
                  this.progressEngine.setLogPrefix(this.tasks.get(var1).getTitle());
                  this.progressEngine.printToLog(L.t("SKIPPED, inactive task", new Object[]{true}));
               } else if (this.runSpecification != 100 || this.runTask.equals(this.tasks.get(var1).getCustomName())) {
                  this.lastRunningTask = this.tasks.get(var1);
                  this.projectRunning = true;
                  if (this.isCustomProject()) {
                     this.lastRunningTask.setTaskLogPrefix(this.lastRunningTask.getTitle());
                     this.lastRunningTask.logTaskStarted(this.globalLog);
                  }

                  this.lastRunningTask.increaseNumberOfRuns();
                  this.currentTaskStartTime = System.currentTimeMillis();
                  this.lastRunningTask.start();
                  this.waitForTaskFinish();
                  if (this.lastRunningTask != null) {
                     this.lastRunningTask.afterFinish();
                     if (this.isCustomProject()) {
                        this.lastRunningTask.logTaskFinished(this.globalLog);
                     }
                  }
               }
            } catch (GoToTaskException var8) {
               this.runSpecification = 0;
               if (!var8.taskName.equals("lastTaskBreakcmd")) {
                  var1 = this.getIndexOfTask(var8.taskName) - 1;
                  this.runCycles++;
               } else {
                  this.runCycles++;
               }
            }
         }

         if (this.isCustomProject()) {
            this.progressEngine.setStatus(4);
            this.progressEngine.setLogPrefix(null);
            this.progressEngine.printToLog(L.t("Project finished", new Object[0]));
         }
      } catch (TaskException var9) {
         Log.error("Exc.", var9);
         String var14;
         if (var9.getCode() == 9) {
            var14 = var9.getMessage();
         } else {
            var14 = SQUtils.getStackTrace(var9);
            var14 = var14.substring(0, Math.min(var14.length(), 300)) + "...";
         }

         this.progressEngine.setStatus(50);
         this.projectRunInfo.error = var14;
         this.projectRunInfo.errorInfo = L.t("Error while running project '%s'.", new Object[]{this.getName()});
         this.projectRunInfo.errorType = 50;
         this.progressEngine.printToLog(L.t("Error while running project '%s'.", new Object[]{this.getName()}) + "\n" + var14);
         CLILogger.log(L.t("Error while running project '%s'.", new Object[]{this.getName()}) + "\n" + var14);
      } catch (Exception var10) {
         Log.error("Exc.", var10);
         String var2 = SQUtils.getStackTrace(var10);
         var2 = var2.substring(0, Math.min(var2.length(), 300)) + "...";
         this.progressEngine.setStatus(50);
         this.projectRunInfo.error = var10.getMessage();
         this.projectRunInfo.errorInfo = L.t("Error while running project '%s'.", new Object[]{this.getName()});
         this.projectRunInfo.errorType = 50;
         this.progressEngine.printToLog(L.t("Error while running project '%s'.", new Object[]{this.getName()}) + "\n" + var2);
         CLILogger.log(L.t("Error while running project '%s'.", new Object[]{this.getName()}) + "\n" + var2);
      } finally {
         if (this.globalLog != null) {
            this.globalLog.close();
         }

         this.projectRunning = false;
         ProjectEngine.projectFinished(this.projectName);
      }
   }

   public void setUnresolvedResources(boolean var1) {
      if (!ProjectEngine.isDefaulProject(this.projectName)) {
         this.hasUnresolvedResources = var1;
      }
   }

   public boolean hasUnresolvedResources() {
      return this.hasUnresolvedResources;
   }
}
