package com.strategyquant.plugin.Task.impl.StopAndStart;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.IProjectCondition;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.ProjectNameComparator;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tomas Brynda")
@Name(name = "Stop & Start task plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class StopAndStartTask extends AbstractTask {
   private ArrayList<IProjectCondition> conditions;
   private boolean skipToFirstProject = false;
   private String logMessage;
   private ProjectNameComparator projectNameComparator = new ProjectNameComparator();

   public StopAndStartTask() throws Exception {
      super(null, null);
   }

   public StopAndStartTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "StopAndStart";
   }

   public String getName() {
      return L.tsq("Stop & Start");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      StopAndStartTask var3 = new StopAndStartTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public void start() throws Exception {
      this.project = ProjectEngine.get(this.projectName);
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Starting Stop & Start evaluation...", new Object[0]));
      this.progressEngine.start();
      this.conditions = (ArrayList<IProjectCondition>)this.settingsData.getParams().get("ProjectConditions");
      this.skipToFirstProject = (Boolean)this.settingsData.getParams().get("SkipToFirstProject");
      this.logMessage = null;
      boolean var1 = true;

      for (int var2 = 0; var2 < this.conditions.size(); var2++) {
         IProjectCondition var3 = this.conditions.get(var2);
         if (!var3.isMet(this.project, this)) {
            var1 = false;
            this.logMessage = L.t("Condition '%s' (%s) not met", new Object[]{var3.getDescription(), var3.getLastCheckedValue()});
            this.progressEngine.printToLog(this.logMessage);
            break;
         }
      }

      if (var1) {
         this.logMessage = L.t("All conditions met, stopping project...", new Object[0]);
         this.progressEngine.printToLog(this.logMessage);
         this.progressEngine.stop();
         String var6 = (String)this.settingsData.getParams().get("ProjectToStart");
         if (var6 != null) {
            try {
               if (var6.equals("next-project")) {
                  var6 = this.getNextProjectName();
               }

               if (var6 != null) {
                  String var7 = L.t("Starting project '%s'...", new Object[]{var6});
                  this.logMessage = this.logMessage + "\n" + var7;
                  this.progressEngine.printToLog(var7);
                  SQProject var8 = ProjectEngine.get(var6);
                  var8.setRunSpecification(0);
                  var8.initTasksFromConfig();
                  var8.beforeStart();
                  var8.start();
               }
            } catch (Exception var5) {
               String var4 = L.t("Cannot start project '%s'", new Object[]{var5.getMessage()});
               this.logMessage = this.logMessage + "\n" + var4;
               this.progressEngine.printToLog(var4);
            }
         }
      } else {
         this.progressEngine.finish();
      }
   }

   private String getNextProjectName() {
      String var1 = null;
      List var2 = ProjectEngine.listCustomProjects();
      var2.sort(this.projectNameComparator);

      for (int var3 = 0; var3 < var2.size(); var3++) {
         SQProject var4 = (SQProject)var2.get(var3);
         if (var4.getName().equals(this.projectName)) {
            if (var3 < var2.size() - 1) {
               var1 = ((SQProject)var2.get(var3 + 1)).getName();
            } else if (this.skipToFirstProject) {
               var1 = ((SQProject)var2.get(0)).getName();
            }
            break;
         }
      }

      return var1;
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskStopAndStart";
   }

   public int getPreferredPosition() {
      return 3;
   }

   public String[] getSettings() {
      return new String[]{"StopAndStart"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return null;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      super.logTaskFinished(var1);
      var1.print(this.logMessage);
   }
}
