package com.strategyquant.plugin.Task.impl.LogDatabankStats;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.ConditionsChecker;
import com.strategyquant.tradinglib.conditions.IConditionValue;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "LogDatabankStatsTask plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class LogDatabankStatsTask extends AbstractTask {
   private String logMessage;
   private Databank databank;
   private ArrayList<Object[]> logStats;
   private ConditionsChecker conditionsChecker = new ConditionsChecker();

   public LogDatabankStatsTask() throws Exception {
      super(null, null);
   }

   public LogDatabankStatsTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "LogDatabankStats";
   }

   public String getName() {
      return L.tsq("Log databank stats");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      LogDatabankStatsTask var3 = new LogDatabankStatsTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   private void init() throws Exception {
      this.project = ProjectEngine.get(this.projectName);
      this.databank = (Databank)this.settingsData.getParams().get("DatabankSource");
      this.logStats = (ArrayList<Object[]>)this.settingsData.getParams().get("LogStats");
   }

   public void start() throws Exception {
      this.init();
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Logging databank statistics of '%s'", new Object[]{this.databank.getName()}));
      this.progressEngine.start();
      this.logMessage = "\n";
      this.logMessage = this.logMessage + L.t("Logging databank statistics of '%s'", new Object[]{this.databank.getName()}) + "\n";
      ArrayList var1 = this.databank.getRecords();
      if (this.logStats.isEmpty()) {
         this.progressEngine.printToLog(L.t("No log data configured", new Object[0]));
         this.logMessage = this.logMessage + L.t("No log data configured", new Object[0]) + "\n";
      } else {
         for (int var2 = 0; var2 < this.logStats.size(); var2++) {
            try {
               Object[] var3 = this.logStats.get(var2);
               String var4 = (String)var3[0];
               ArrayList var5 = (ArrayList)var3[1];
               IConditionValue var6 = (IConditionValue)var3[2];
               this.logStats(var4, var5, var6, var1);
               this.progressEngine.checkPaused();
               if (this.progressEngine.isStopped()) {
                  break;
               }
            } catch (OutOfMemoryError var7) {
               this.project.onMemoryError(var7);
               break;
            }
         }
      }

      this.progressEngine.finish();
   }

   private void logStats(String var1, ArrayList<Condition> var2, IConditionValue var3, ArrayList<ResultsGroup> var4) throws Exception {
      double var8 = 0.0;
      int var10 = 0;

      for (int var11 = 0; var11 < var4.size(); var11++) {
         ResultsGroup var5 = (ResultsGroup)var4.get(var11);
         if (this.checkConditions(var5, var2)) {
            var10++;

            try {
               switch (var1) {
                  case "sum":
                     double var19 = var3.statsExists(new Object[]{var5}) ? var3.getValue(new Object[]{var5}) : 0.0;
                     var8 += var19;
                     break;
                  case "avg":
                     double var6 = var3.statsExists(new Object[]{var5}) ? var3.getValue(new Object[]{var5}) : 0.0;
                     var8 += var6;
                     break;
                  case "count":
                     var8++;
               }
            } catch (CrossCheckDataNotExistException var18) {
            }
         }
      }

      String var22 = "NA";
      String var23 = var3.getTitle();
      switch (var1) {
         case "sum":
            String var21 = var3.printFormatedValue(var8);
            var22 = L.t("Sum of %s of strategies matching conditions: %s, results passed %d", new Object[]{var23, var21, var10});
            break;
         case "avg":
            String var20 = var3.printFormatedValue(var8 / var10);
            var22 = L.t("Average of %s of strategies matching conditions: %s, results passed %d", new Object[]{var23, var20, var10});
            break;
         case "count":
            double var16 = var8 / var4.size() * 100.0;
            var22 = L.t(
               "Count of strategies matching conditions: %s (%s of all)", new Object[]{Double.valueOf(var8).intValue(), PlTypes.printPL(var16, (byte)20)}
            );
      }

      String var24 = "";

      for (int var25 = 0; var25 < var2.size(); var25++) {
         Condition var26 = (Condition)var2.get(var25);
         if (var26.isUsed()) {
            var24 = var24 + "\t" + var26.toString() + "\n";
         }
      }

      this.progressEngine.printToLog(var22 + "\n" + var24);
      this.logMessage = this.logMessage + var22 + "\n" + var24 + "\n";
   }

   private boolean checkConditions(ResultsGroup var1, ArrayList<Condition> var2) {
      for (int var3 = 0; var3 < var2.size(); var3++) {
         Condition var4 = (Condition)var2.get(var3);

         try {
            if (var4.isUsed() && !this.conditionsChecker.check(var1, var4)) {
               return false;
            }
         } catch (CrossCheckDataNotExistException var6) {
         } catch (Exception var7) {
            this.progressEngine.printToLog(L.t("Error while evaluating conditions: %s", new Object[]{var7.getMessage()}));
            Log.error("Error while evaluating condition: ", var7);
         }
      }

      return true;
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskLogDatabankStats";
   }

   public int getPreferredPosition() {
      return 500;
   }

   public String[] getSettings() {
      return new String[]{"LogDatabankStats"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return null;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      var1.print(this.logMessage, false);
      super.logTaskFinished(var1);
   }
}
