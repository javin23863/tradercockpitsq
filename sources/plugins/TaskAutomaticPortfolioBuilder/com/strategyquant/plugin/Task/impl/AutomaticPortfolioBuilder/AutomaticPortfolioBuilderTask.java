package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tomas Brynda")
@Name(name = "AutomaticPortfolioBuilder Task plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class AutomaticPortfolioBuilderTask extends AbstractTask {
   private String projectLogMessage = "";

   public AutomaticPortfolioBuilderTask() throws Exception {
      super(null, null);
   }

   public AutomaticPortfolioBuilderTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "AutomaticPortfolioBuilder";
   }

   public String getName() {
      return L.tsq("Automatic Portfolio Builder");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      AutomaticPortfolioBuilderTask var3 = new AutomaticPortfolioBuilderTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public void start() throws Exception {
      this.project = ProjectEngine.get(this.projectName);
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Starting PortfolioMaster...", new Object[0]));
      this.progressEngine.start();

      try {
         this.checkSpecialTrialLimitation();
         PortfolioMasterSettings var1 = (PortfolioMasterSettings)this.settingsData.getParams().get("AutomaticPortfolioBuilder");
         AutomaticPortfolioBuilder var2 = new AutomaticPortfolioBuilder();
         var2.start(this.project, this.progressEngine, var1);
      } catch (OutOfMemoryError var3) {
         this.project.onMemoryError(var3);
      }

      this.progressEngine.finish();
   }

   private void printToLog(String var1) {
      this.projectLogMessage = this.projectLogMessage + var1 + "\n";
   }

   private void printToBothLogs(String var1) {
      this.progressEngine.printToLog(var1);
      this.projectLogMessage = this.projectLogMessage + "- " + var1 + "\n";
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskAutomaticPortfolioBuilder";
   }

   public int getPreferredPosition() {
      return 3;
   }

   public String[] getSettings() {
      return new String[]{"AutomaticPortfolioBuilder", "Databanks"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return null;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      if (!this.projectLogMessage.isEmpty()) {
         var1.print(this.projectLogMessage, false);
      }

      super.logTaskFinished(var1);
      var1.print(this.logMessage);
   }
}
