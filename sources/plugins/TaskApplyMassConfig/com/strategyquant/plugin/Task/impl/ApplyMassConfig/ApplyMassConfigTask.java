package com.strategyquant.plugin.Task.impl.ApplyMassConfig;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.applyMassConfig.ApplyMassConfig;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tomas Brynda")
@Name(name = "ApplyMassConfigTask plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Apply mass config")
@PluginImplementation
public class ApplyMassConfigTask extends AbstractTask {
   private ApplyMassConfig applyMassConfig;

   public ApplyMassConfigTask() throws Exception {
      super(null, null);
   }

   public ApplyMassConfigTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "ApplyMassConfig";
   }

   public String getName() {
      return L.tsq("Apply mass config");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      ApplyMassConfigTask var3 = new ApplyMassConfigTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public void start() throws Exception {
      this.applyMassConfig = (ApplyMassConfig)this.settingsData.getParams().get("ApplyMassConfig");
      this.project = ProjectEngine.get(this.projectName);
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Applying mass config...", new Object[0]));
      this.progressEngine.start();
      this.applyMassConfig.applyChangesToTasks(this.project, this.progressEngine);
      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskApplyMassConfig";
   }

   public int getPreferredPosition() {
      return 3;
   }

   public String[] getSettings() {
      return new String[]{"ApplyMassConfig"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return null;
   }
}
