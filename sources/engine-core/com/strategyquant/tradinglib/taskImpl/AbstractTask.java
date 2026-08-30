package com.strategyquant.tradinglib.taskImpl;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DefaultDatabankFilter;
import com.strategyquant.tradinglib.databank.OptimizeDatabankFilter;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.TaskStat;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.SettingError;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.util.ResultComparatorByName;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTask implements ISQTask {
   public static final Logger Log = LoggerFactory.getLogger(AbstractTask.class);
   protected String projectName;
   protected String customName;
   protected String configFileName;
   private Element taskConfig;
   protected String title;
   protected int taskIndex;
   protected boolean active = true;
   protected ArrayList<ISettingTabPlugin> settingTabPlugins;
   protected TaskSettingsData settingsData = new TaskSettingsData();
   protected ProgressEngine progressEngine;
   protected String taskLogPrefix;
   protected SQProject project;
   private int numberOfRuns = 0;
   protected String logMessage;
   private Comparator<ResultsGroup> comparatorByName = new ResultComparatorByName();
   private long taskStarted;

   public AbstractTask(String var1, ProgressEngine var2) throws Exception {
      this.projectName = var1;
      this.progressEngine = var2;
   }

   public String getProduct() {
      return "SQUANT";
   }

   @Override
   public Element getDefaultConfig(String var1) throws Exception {
      try {
         if (this.getPluginFolderName() == null) {
            throw new Exception("Property 'pluginFolderName' is not set.");
         }

         String var2 = SQStructure.PLUGINS_DIR + "/" + this.getPluginFolderName();
         File var3 = new File(var2 + "/" + "task.xml");
         if (!var3.exists()) {
            var3 = new File(var2 + "/" + var1);
         }

         if (!var3.exists()) {
            var2 = SQStructure.USER_EXTEND_PLUGINS + "/" + this.getPluginFolderName();
            var3 = new File(var2 + "/" + "task.xml");
         }

         return XMLUtil.fileToXmlElement(var3).clone();
      } catch (Exception var4) {
         Log.error("Cannot get task default config. Exc.", var4);
         throw new Exception(L.t("Cannot get task default config.", new Object[0]), var4);
      }
   }

   @Override
   public boolean isActive() {
      return this.active;
   }

   @Override
   public void setActive(boolean var1) {
      this.active = var1;
   }

   @Override
   public String getCustomName() {
      return this.customName;
   }

   @Override
   public void setCustomName(String var1) {
      this.customName = var1;
   }

   @Override
   public void setTitle(String var1) {
      this.title = var1;
   }

   @Override
   public String getTitle() {
      return this.title == null ? this.getCustomName() : this.title;
   }

   @Override
   public void setConfigFileName(String var1) {
      this.configFileName = var1;
   }

   @Override
   public String getTaskXMLFileName() {
      return this.configFileName;
   }

   @Override
   public TaskSettingsData getSettingsData() {
      return this.settingsData;
   }

   @Override
   public void setConfig(Element var1, boolean var2, boolean var3) {
      if (!var2 || this.taskConfig == null || !XMLUtil.elementToString(var1).equals(XMLUtil.elementToString(this.taskConfig))) {
         this.taskConfig = var1;
         this.settingsData.clear();
         if (var1 == null) {
            this.settingsData.addError(null, null, "Invalid Task XML configuration - missing Settings.");
         } else if (var3) {
            for (int var4 = 0; var4 < this.settingTabPlugins.size(); var4++) {
               this.settingTabPlugins.get(var4).readSettings(this.projectName, this, var1, this.settingsData);
            }
         }
      }
   }

   @Override
   public Element getConfig() {
      return this.taskConfig;
   }

   public void initPlugin() throws Exception {
      this.settingTabPlugins = new ArrayList<>();
      if (this.getSettings() != null) {
         for (ISettingTabPlugin var3 : SQPluginManager.getPlugins(ISettingTabPlugin.class)) {
            if (this.isInArray(var3.getSettingName())) {
               this.settingTabPlugins.add(var3);
            }
         }
      }
   }

   protected boolean isInArray(String var1) {
      String[] var2 = this.getSettings();

      for (int var3 = 0; var3 < var2.length; var3++) {
         if (var1.equals(var2[var3])) {
            return true;
         }
      }

      return false;
   }

   @Override
   public String getErrorsDescription() {
      String var1 = "";

      for (SettingError var3 : this.settingsData.getErrors()) {
         String var4 = formatWSError(var3.settingName, var3.fieldName, var3.errorMsg);
         Log.error(var4);
         var1 = var1 + var4 + "\n";
      }

      return var1;
   }

   public static String formatWSError(String var0, String var1, String var2) {
      StringBuilder var3 = new StringBuilder("Error: ");
      var3.append(var2);
      if (var1 != null) {
         var3.append(" in field: ");
         var3.append(var1);
      }

      var3.append(", in setting: ");
      var3.append(var0);
      return var3.toString();
   }

   protected void setDatabankFilter(Databank var1, boolean var2) {
      try {
         int var3 = 100000;
         if (this.settingsData.getParams().containsKey("MaxStrategies")) {
            var3 = (Integer)this.settingsData.getParams().get("MaxStrategies");
         }

         if (this.getType().equals("Optimize")) {
            var1.applyFilter(new OptimizeDatabankFilter(var3));
         } else if (!this.getType().equals("Retest") && !this.getType().equals("AutomaticRetest")) {
            var1.applyFilter(new DefaultDatabankFilter(var3, var2));
         }

         Log.debug("Ranking settings set for databank '{}'. ", var1.getName());
      } catch (Exception var4) {
         Log.error(
            "Project '" + this.projectName + "', Task '" + this.getCustomName() + "' - Cannot set databank filter do databank '" + var1.getName() + "'.", var4
         );
      }
   }

   public String getTaskXML() {
      try {
         Element var1 = this.getTaskElement();
         return var1 != null ? XMLUtil.elementToString(var1) : null;
      } catch (Exception var2) {
         Log.error("Cannot get task config", var2);
         return null;
      }
   }

   public Element getTaskElement() {
      return this.taskConfig;
   }

   @Override
   public void setTaskLogPrefix(String var1) {
      this.taskLogPrefix = var1;
   }

   @Override
   public void afterFinish() {
      this.removeDatabankFilters();
      this.synchronizeOutputDatabank();
   }

   @Override
   public void increaseNumberOfRuns() {
      this.numberOfRuns++;
   }

   @Override
   public int getNumberOfRuns() {
      return this.numberOfRuns;
   }

   private void removeDatabankFilters() {
      Databank[] var1 = this.getUsedDatabanks();
      if (var1 != null && var1.length > 0) {
         for (int var2 = 0; var2 < var1.length; var2++) {
            if (var1[var2] != null) {
               try {
                  var1[var2].removeFilter();
               } catch (Exception var4) {
                  Log.error("Cannot remove databank filter from databank '" + var1[var2].getName() + "'");
               }
            }
         }
      }
   }

   private void synchronizeOutputDatabank() {
      boolean var1 = false;

      try {
         var1 = Boolean.parseBoolean(MainApp.settings().get("syncDatabanksAfterTaskDone", "false"));
      } catch (Exception var3) {
         Log.error("Unable to read SyncDatabanksAfterTaskDone setting. Skipping synchronization...", var3);
      }

      if (var1) {
         Databank var2 = this.getOutputDatabank();
         if (var2 != null && !var2.getSyncType().equals("Auto-sync never")) {
            var2.synchronizeNow(
               true, String.format("Task %s/%s done, synchronize output databank %s/%s.", this.projectName, this.getTitle(), this.projectName, var2.getName())
            );
         }
      }
   }

   @Override
   public abstract void start() throws Exception;

   protected abstract int getRunningStatus();

   protected abstract Databank[] getUsedDatabanks();

   protected abstract Databank getOutputDatabank();

   @Override
   public void logTaskStarted(ProjectGlobalLog var1) {
      this.taskStarted = System.currentTimeMillis();
      var1.printEmptyLine();
      var1.printSeparator();
      var1.print(L.t("TASK STARTED at %s", new Object[]{SQTime.getLogTime(true, true)}));
      var1.print(L.t("Task: %s, Type: %s", new Object[]{this.getTitle(), this.getType()}));
      String var2 = this.getDatabanksStats(true);
      var1.print(L.t("Databanks before start: %s", new Object[]{var2}));
   }

   @Override
   public void logTaskFinished(ProjectGlobalLog var1) {
      long var2 = System.currentTimeMillis() - this.taskStarted;
      String var4 = SQUtils.formatDuration(var2);
      String var5 = this.getDatabanksStats(true);
      var1.printEmptyLine();
      var1.print(L.t("TASK FINISHED at %s in %s", new Object[]{SQTime.getLogTime(true, true), var4}));
      var1.print(L.t("Databanks after finish: %s", new Object[]{var5}));
      var1.printEmptyLine();
      if (this.project == null) {
         try {
            this.project = ProjectEngine.get(this.projectName);
         } catch (Exception var7) {
            Log.error("Exc.", var7);
         }
      }

      if (this.project != null) {
         this.project.setGlobalStats(TaskStat.MILLISECONDS, this.getCustomName().hashCode(), L.t("Duration", new Object[0]), var2, 3);
      }
   }

   protected String getDatabanksContents() {
      StringBuilder var1 = new StringBuilder();

      try {
         SQProject var2 = ProjectEngine.get(this.projectName);
         ArrayList var3 = var2.getDatabanks().list();

         for (int var4 = 0; var4 < var3.size(); var4++) {
            Databank var5 = (Databank)var3.get(var4);
            if (var5.size() != 0 && !var5.getName().equals("Results")) {
               this.printDatabankContents(var1, var5);
            }
         }
      } catch (Exception var6) {
         var1.append("EXCEPTION: ");
         var1.append(var6.getMessage());
         var1.append("\n");
      }

      return var1.toString();
   }

   private void printDatabankContents(StringBuilder var1, Databank var2) throws Exception {
      var1.append("-------------- ");
      var1.append(var2.getName());
      var1.append("\n");
      ArrayList var3 = var2.getRecords();
      List var4 = var3.subList(0, var2.size());
      Collections.sort(var4, this.comparatorByName);

      for (int var5 = 0; var5 < var4.size(); var5++) {
         ResultsGroup var6 = (ResultsGroup)var4.get(var5);
         int var7 = var6.portfolio().stats((byte)0, (byte)10, (byte)127).getInt("NumberOfTrades");
         double var8 = var6.portfolio().stats((byte)0, (byte)10, (byte)127).getDouble("NetProfit");
         var1.append(var6.getName());
         var1.append("\t\t\t\t");
         var1.append(var7);
         var1.append("\t\t");
         var1.append(var8);
         var1.append("\n");
      }
   }

   private String getDatabanksStats(boolean var1) {
      String var2 = "";

      try {
         SQProject var3 = ProjectEngine.get(this.projectName);
         ArrayList var4 = var3.getDatabanks().list();

         for (int var5 = 0; var5 < var4.size(); var5++) {
            Databank var6 = (Databank)var4.get(var5);
            if (var1 || var6.size() != 0) {
               var2 = var2 + var6.getName() + " (" + var6.size() + "), ";
            }
         }
      } catch (Exception var7) {
         var2 = "cannot get databanks";
      }

      if (var2.endsWith(", ")) {
         var2 = var2.substring(0, var2.length() - 2);
      }

      return var2;
   }

   protected String formatIntOrDouble(double var1) {
      return var1 > 1000.0 ? String.format("%.0f", var1) : String.format("%.2f", var1);
   }

   protected void printLog(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         this.logMessage = "\n" + var1;
      } else {
         this.logMessage = var1;
      }

      this.progressEngine.printToLog(var1);
   }

   @Override
   public int getIndex() {
      return this.taskIndex;
   }

   @Override
   public void setIndex(int var1) {
      this.taskIndex = var1;
   }

   @Override
   public double getCustomValue(String var1) {
      return 0.0;
   }

   protected void checkSpecialTrialLimitation() throws Exception {
      if (MainApp.v571hfnsHw().bDm88fRJA3()) {
         throw new Exception(L.t("Task '%s' is available only in full version, not in this special trial.", new Object[]{this.getName()}));
      }
   }
}
