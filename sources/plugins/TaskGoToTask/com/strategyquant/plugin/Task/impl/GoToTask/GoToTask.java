package com.strategyquant.plugin.Task.impl.GoToTask;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.GoToTaskException;
import com.strategyquant.tradinglib.project.IProjectCondition;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Go To Task plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class GoToTask extends AbstractTask {
   private String targetTaskName;
   private boolean resetEvaluatedCycles;
   private boolean resetActivatedCycles;
   private ArrayList<IProjectCondition> conditions;
   private String logMessage;
   private int evaluatedCycles;
   private int activatedCycles;

   public GoToTask() throws Exception {
      super(null, null);
   }

   public GoToTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
      this.evaluatedCycles = 0;
      this.activatedCycles = 0;
   }

   public void start() throws Exception {
      this.targetTaskName = (String)this.settingsData.getParams().get("GoToTaskName");
      this.resetEvaluatedCycles = (Boolean)this.settingsData.getParams().get("GoToTaskResetEvaluatedCycles");
      this.resetActivatedCycles = (Boolean)this.settingsData.getParams().get("GoToTaskResetActivatedCycles");
      this.conditions = (ArrayList<IProjectCondition>)this.settingsData.getParams().get("ProjectConditions");
      this.project = ProjectEngine.get(this.projectName);
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.logMessage = "";
      if (!this.targetTaskName.trim().isEmpty()) {
         this.evaluatedCycles++;
         boolean var1 = true;

         for (int var2 = 0; var2 < this.conditions.size(); var2++) {
            IProjectCondition var3 = this.conditions.get(var2);
            boolean var4 = var3.isMet(this.project, this);
            this.logMessage = this.logMessage
               + "- "
               + L.t(
                  "Condition '%s' (%s) - %s.",
                  new Object[]{var3.getDescription(), var3.getLastCheckedValue(), var4 ? L.t("passed", new Object[0]) : L.t("failed", new Object[0])}
               )
               + "\n";
            if (!var4) {
               var1 = false;
               this.logMessage = this.logMessage
                  + "  - "
                  + L.t("Condition '%s' (%s) failed, going to following task.", new Object[]{var3.getDescription(), var3.getLastCheckedValue()});
               this.progressEngine.printToLog(this.logMessage);
               break;
            }
         }

         if (!var1) {
            if (this.taskIndex < this.project.getTasksCount()) {
               ISQTask var6 = this.project.getTask(this.taskIndex);
               this.targetTaskName = var6 == null ? "lastTaskBreakcmd" : var6.getCustomName();
               if (this.resetActivatedCycles) {
                  this.logMessage = this.logMessage + "- " + L.t("Number of ACTIVATED cycles has been reset.\n", new Object[0]);
                  this.activatedCycles = 0;
               }

               if (var6 == null) {
                  this.progressEngine.setRunningStatus(4);
               }

               this.logTaskFinished(this.project.getGlobalLog());
               throw new GoToTaskException(this.targetTaskName);
            } else {
               this.progressEngine.setRunningStatus(4);
               this.logTaskFinished(this.project.getGlobalLog());
               throw new GoToTaskException("lastTaskBreakcmd");
            }
         } else if (var1) {
            this.activatedCycles++;
            ISQTask var5 = this.project.getTaskByName(this.targetTaskName);
            int var7 = var5 == null ? 0 : var5.getIndex();
            String var8 = var5 == null ? this.targetTaskName : var5.getTitle();
            this.logMessage = this.logMessage
               + L.t(
                  "- %s. Skipping to task #%d %s\n",
                  new Object[]{this.conditions.isEmpty() ? L.t("No conditions", new Object[0]) : L.t("All conditions passed", new Object[0]), var7, var8}
               );
            if (this.resetEvaluatedCycles) {
               this.logMessage = this.logMessage + "- " + L.t("Number of EVALUATED cycles has been reset.\n", new Object[0]);
               this.evaluatedCycles = 0;
            }

            this.progressEngine.printToLog(this.logMessage);
            this.logTaskFinished(this.project.getGlobalLog());
            throw new GoToTaskException(this.targetTaskName);
         }
      }
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskGoToTask";
   }

   public int getPreferredPosition() {
      return 4;
   }

   public String getType() {
      return "GoToTask";
   }

   public String getName() {
      return L.tsq("Go To Task");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      GoToTask var3 = new GoToTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public String[] getSettings() {
      return new String[]{"GoToTask"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return null;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      var1.print(this.logMessage);
      super.logTaskFinished(var1);
   }

   public double getCustomValue(String var1) {
      if (var1.equals("evaluatedCycles")) {
         return this.evaluatedCycles;
      } else {
         return var1.equals("activatedCycles") ? this.activatedCycles : 0.0;
      }
   }
}
