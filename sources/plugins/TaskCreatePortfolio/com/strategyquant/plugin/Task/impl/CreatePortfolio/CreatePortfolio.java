package com.strategyquant.plugin.Task.impl.CreatePortfolio;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Create portfolio plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class CreatePortfolio extends AbstractTask {
   private Databank databankSource = null;
   private Databank databankTarget = null;
   private int maxStrategies = 100;
   private int strategies;
   private String logMessage;

   public CreatePortfolio() throws Exception {
      super(null, null);
   }

   public CreatePortfolio(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   private void init() {
      this.strategies = 0;
      this.logMessage = null;
      this.databankSource = (Databank)this.settingsData.getParams().get("DatabankSource");
      this.databankTarget = (Databank)this.settingsData.getParams().get("DatabankTarget");
      this.maxStrategies = (Integer)this.settingsData.getParams().get("MaxStrategies");
   }

   public void start() throws Exception {
      this.init();
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Starting...", new Object[0]));
      this.progressEngine.start();
      ArrayList var1 = this.databankSource.getRecords();
      ArrayList var2 = new ArrayList();

      try {
         for (int var3 = var1.size() - 1; var3 >= 0 && var2.size() != this.maxStrategies; var3--) {
            ResultsGroup var4 = this.databankSource.getLocked(((ResultsGroup)var1.get(var3)).getName(), "CreatePortfolioTask");
            var2.add(var4);
            this.strategies++;
         }

         if (var1.isEmpty()) {
            this.logMessage = L.t("Portfolio cannot be created because databank '%s' is empty.", new Object[]{this.databankSource.getName()});
            this.progressEngine.printToLog(L.t("Portfolio not created: databank '%s' is empty.", new Object[]{this.databankSource.getName()}));
         } else if (var2.size() < 2) {
            this.logMessage = L.t("Portfolio cannot be created because databank '%s' contains only one strategy.", new Object[]{this.databankSource.getName()});
            this.progressEngine
               .printToLog(L.t("Portfolio not created: databank '%s' contains only one strategy.", new Object[]{this.databankSource.getName()}));
         } else {
            ResultsGroup var10 = ResultsGroup.merge(var2, new String[]{"AdditionalMarket", "CrossCheck_"}, null);
            var10.createPortfolioResult();
            this.databankTarget.add(var10, true);
            this.databankTarget.updateBestResults(var10);
            this.progressEngine.printToLog(L.t("Portfolio created from %d strategies.", new Object[]{this.strategies}));
         }
      } finally {
         for (ResultsGroup var7 : var2) {
            var7.releaseLock("CreatePortfolioTask");
         }
      }

      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskCreatePortfolio";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public String getType() {
      return "CreatePortfolio";
   }

   public String getName() {
      return L.tsq("Create portfolio");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      CreatePortfolio var3 = new CreatePortfolio(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public String[] getSettings() {
      return new String[]{"CreatePortfolio"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return this.databankTarget;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      super.logTaskFinished(var1);
      if (this.logMessage == null) {
         var1.print(
            L.t(
               "Portfolio created from %d strategies from source databank '%s' saved to target databank '%s'.",
               new Object[]{this.strategies, this.databankSource.getName(), this.databankTarget.getName()}
            )
         );
      } else {
         var1.print(this.logMessage);
      }
   }
}
