package com.strategyquant.plugin.Task.impl.CustomAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisInfo;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "CustomAnalysisTask plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class CustomAnalysisTask extends AbstractTask {
   private static final String LockCustomAnalysisTask = "CustomAnalysisTask";
   private String projectLogMessage = "";
   private CustomAnalysisInfo ca = null;
   private Databank databankSource = null;
   private Databank databankTarget = null;

   public CustomAnalysisTask() throws Exception {
      super(null, null);
   }

   public CustomAnalysisTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "CustomAnalysis";
   }

   public String getName() {
      return L.tsq("Custom analysis");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      CustomAnalysisTask var3 = new CustomAnalysisTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   private void init() throws Exception {
      this.project = ProjectEngine.get(this.projectName);
      this.ca = (CustomAnalysisInfo)this.settingsData.getParams().get("CustomAnalysis");
      this.databankSource = (Databank)this.settingsData.getParams().get("DatabankSource");
      this.databankTarget = (Databank)this.settingsData.getParams().get("DatabankTarget");
   }

   public void start() throws Exception {
      this.init();
      this.projectLogMessage = "";
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Starting custom analysis...", new Object[0]));
      this.progressEngine.start();
      ArrayList var1 = this.databankSource.getRecords();
      ArrayList var2 = new ArrayList();

      for (int var3 = 0; var3 < var1.size(); var3++) {
         ResultsGroup var4 = (ResultsGroup)var1.get(var3);
         if (this.databankSource != this.databankTarget) {
            var4 = var4.clone();
         }

         var2.add(var4);
      }

      if (this.ca.perStrategy1 != null) {
         this.runPerStrategyAnalysis(this.ca.perStrategy1, var2, this.ca.inputArgsPerStrategy1);
      }

      if (this.ca.fullDatabank1 != null) {
         var2 = this.runFullDatabankAnalysis(this.ca.fullDatabank1, var2, this.ca.inputArgsFullDatabank1);
      }

      if (this.ca.perStrategy2 != null) {
         this.runPerStrategyAnalysis(this.ca.perStrategy2, var2, this.ca.inputArgsPerStrategy2);
      }

      if (this.ca.fullDatabank2 != null) {
         var2 = this.runFullDatabankAnalysis(this.ca.fullDatabank2, var2, this.ca.inputArgsFullDatabank2);
      }

      Object2ObjectOpenHashMap var6 = new Object2ObjectOpenHashMap();

      for (int var7 = 0; var7 < var2.size(); var7++) {
         var6.put(((ResultsGroup)var2.get(var7)).getName(), (ResultsGroup)var2.get(var7));
      }

      for (int var5 = var1.size() - 1; var5 >= 0; var5--) {
         ResultsGroup var8 = (ResultsGroup)var1.get(var5);
         if (var6.containsKey(var8.getName())) {
            if (!this.databankSource.getName().equals(this.databankTarget.getName())) {
               this.databankTarget.add((ResultsGroup)var6.get(var8.getName()), true);
               this.progressEngine.printToLogDebug("   - " + L.t("%s, moved to : %s", new Object[]{var8.getName(), this.databankTarget.getName()}));
            }
         } else if (this.ca.filter) {
            this.databankSource.remove(var8.getName(), true, true, true, true, "CustomAnalysisTask");
            this.progressEngine.printToLogDebug("   - " + L.t("%s, removed from : %s", new Object[]{var8.getName(), this.databankSource.getName()}));
         }
      }

      this.progressEngine.finish();
   }

   private ArrayList<ResultsGroup> runFullDatabankAnalysis(CustomAnalysisMethod var1, ArrayList<ResultsGroup> var2, String var3) throws Exception {
      var1.setProjectLog(this.projectLogMessage);
      var1.setInputArgs(var3);
      ArrayList var4 = var1.processDatabank(this.project.getName(), this.getTitle(), this.databankSource.getName(), var2);
      this.projectLogMessage = var1.getProjectLog();
      return var4;
   }

   private void runPerStrategyAnalysis(CustomAnalysisMethod var1, ArrayList<ResultsGroup> var2, String var3) throws Exception {
      for (int var4 = var2.size() - 1; var4 >= 0; var4--) {
         ResultsGroup var5 = (ResultsGroup)var2.get(var4);
         this.printToLog(L.t("%s, running Per strategy analysis: %s", new Object[]{var5.getName(), var1.name}));
         var1.setInputArgs(var3);
         if (!var1.filterStrategy(this.project.getName(), this.getTitle(), this.databankSource.getName(), var5)) {
            this.printToLog("   - Failed");
            var2.remove(var4);
         } else {
            this.printToLog("   - OK");
         }
      }
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskCustomAnalysis";
   }

   public int getPreferredPosition() {
      return 500;
   }

   public String[] getSettings() {
      return new String[]{"CustomAnalysis", "Databanks"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return null;
   }

   private void printToLog(String var1) {
      this.projectLogMessage = this.projectLogMessage + var1 + "\n";
      this.progressEngine.printToLogDebug(var1);
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      if (!this.projectLogMessage.isEmpty()) {
         var1.print(this.projectLogMessage, false);
      }

      super.logTaskFinished(var1);
   }
}
