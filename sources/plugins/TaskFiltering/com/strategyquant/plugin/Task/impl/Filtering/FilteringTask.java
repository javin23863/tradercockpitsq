package com.strategyquant.plugin.Task.impl.Filtering;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.ConditionsChecker;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Filter strategies plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class FilteringTask extends AbstractTask {
   private static final String LOCK_FILTERINGTASK = "FilteringTask";
   private int actionType;
   private int conditionsType;
   private ArrayList<Condition> conditions;
   private Databank databankSource = null;
   private Databank databankTarget = null;
   private String dismissReason;
   private int totalResults;
   private int totalPassed = 0;
   private int maxStrategies = 0;
   private ConditionsChecker conditionsChecker = new ConditionsChecker();

   public FilteringTask() throws Exception {
      super(null, null);
   }

   public FilteringTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "Filtering";
   }

   public String getName() {
      return L.tsq("Filter strategies");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      FilteringTask var3 = new FilteringTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public void start() throws Exception {
      this.project = ProjectEngine.get(this.projectName);
      this.databankSource = (Databank)this.settingsData.getParams().get("DatabankSource");
      this.databankTarget = (Databank)this.settingsData.getParams().get("DatabankTarget");
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Starting strategies filtering...", new Object[0]));
      this.progressEngine.start();
      if (this.databankTarget != null) {
         this.setDatabankFilter(this.databankTarget, false);
      }

      this.actionType = (Integer)this.settingsData.getParams().get("ActionType");
      this.conditionsType = (Integer)this.settingsData.getParams().get("ConditionsType");
      this.conditions = (ArrayList<Condition>)this.settingsData.getParams().get("Conditions");
      this.maxStrategies = (Integer)this.settingsData.getParams().get("MaxStrategiesFilter");
      ArrayList var1 = this.databankSource.getRecords();
      this.totalPassed = 0;
      this.totalResults = var1.size();

      for (int var4 = var1.size() - 1; var4 >= 0; var4--) {
         ResultsGroup var5 = (ResultsGroup)var1.get(var4);
         String var6 = var5.getName();

         try {
            boolean var3 = false;
            if (this.maxStrategies > 0 && this.totalPassed >= this.maxStrategies) {
               break;
            }

            if (this.conditionsPass(var5)) {
               this.totalPassed++;
               var3 = true;
               switch (this.actionType) {
                  case 0:
                     this.databankSource.remove(var6, true, true, true, true, "FilteringTask");
                     this.progressEngine.printToLogDebug(L.t("%s, deleted: %s", new Object[]{var6, this.dismissReason}));
                     break;
                  case 1:
                     ResultsGroup var9 = var5.clone();
                     this.databankTarget.add(var9, true);
                     this.progressEngine.printToLogDebug(L.t("%s, copied: %s", new Object[]{var6, this.dismissReason}));
                     break;
                  case 2:
                     ResultsGroup var2 = var5.clone();
                     this.databankTarget.add(var2, true);
                     this.databankSource.remove(var6, true, true, true, false, "FilteringTask");
                     this.progressEngine.printToLogDebug(L.t("%s, moved: %s", new Object[]{var6, this.dismissReason}));
               }
            }

            this.increaseGlobalStats(var3);
            this.progressEngine.checkPaused();
            if (this.progressEngine.isStopped()) {
               break;
            }
         } catch (OutOfMemoryError var8) {
            this.project.onMemoryError(var8);
            break;
         }
      }

      if (this.actionType == 0) {
         this.databankSource.updateBestResults();
      } else {
         this.databankTarget.updateBestResults();
      }

      this.progressEngine.finish();
   }

   private void increaseGlobalStats(boolean var1) {
      String var2 = this.actionTypeToText();
      this.project.increaseGlobalStats(this.getCustomName().hashCode(), L.t("Input", new Object[0]), 1L, var2, var1 ? 1L : 0L);
   }

   private boolean conditionsPass(ResultsGroup var1) {
      if (this.conditionsType == 2 || this.conditionsType == 3) {
         SettingsMap var2 = var1.specialValues();
         if (this.conditionsType == 2) {
            if (var2.containsKey(SpecialValues.FiltersResultFailedReason)
               && var2.getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)) {
               this.dismissReason = L.t("Filter result = Passed", new Object[0]);
               return true;
            }

            return false;
         }

         if (this.conditionsType == 3) {
            if (var2.containsKey(SpecialValues.FiltersResultFailedReason)
               && var2.getString(SpecialValues.FiltersResultFailedReason).equals(SpecialValues.FiltersResultPassed)) {
               return false;
            }

            this.dismissReason = L.t("Filter result = Failed", new Object[0]);
            return true;
         }
      }

      if (this.conditions != null && !this.conditions.isEmpty()) {
         String var3 = this.checkConditions(var1);
         if (this.conditionsType == 0 && var3 == null) {
            this.dismissReason = L.t("fit the conditions", new Object[0]);
            return true;
         } else if (this.conditionsType == 1 && var3 != null) {
            this.dismissReason = L.t("not fit %s", new Object[]{var3});
            return true;
         } else {
            return false;
         }
      } else {
         this.dismissReason = L.t("No conditions", new Object[0]);
         return true;
      }
   }

   private String checkConditions(ResultsGroup var1) {
      for (Condition var3 : this.conditions) {
         try {
            if (var3.isUsed()) {
               boolean var4 = this.conditionsChecker.check(var1, var3);
               if (!var4) {
                  return this.conditionsChecker.dismissalMessage;
               }
            }
         } catch (Exception var5) {
            this.progressEngine.printToLog(L.t("Error while evaluating conditions: %s", new Object[]{var5.getMessage()}));
            Log.error("Error while evaluating condition: ", var5);
         }
      }

      return null;
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskFiltering";
   }

   public int getPreferredPosition() {
      return 3;
   }

   public String[] getSettings() {
      return new String[]{"Filtering"};
   }

   protected Databank[] getUsedDatabanks() {
      return new Databank[]{this.databankSource, this.databankTarget};
   }

   protected Databank getOutputDatabank() {
      return this.databankTarget;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      super.logTaskFinished(var1);
      String var2 = this.actionTypeToText();
      var1.print(var2 + ": " + this.totalPassed);
      if (this.progressEngine != null) {
         this.progressEngine.printToLogDebug(var2 + ": " + this.totalPassed);
      }
   }

   private String actionTypeToText() {
      if (this.actionType == 0) {
         return L.t("Deleted", new Object[0]);
      } else if (this.actionType == 1) {
         return L.t("Copied", new Object[0]);
      } else {
         return this.actionType == 2 ? L.t("Moved", new Object[0]) : "NA";
      }
   }
}
