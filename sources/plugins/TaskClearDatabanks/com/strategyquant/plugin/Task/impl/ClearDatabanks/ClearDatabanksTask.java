package com.strategyquant.plugin.Task.impl.ClearDatabanks;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tomas Brynda")
@Name(name = "Clear databanks task plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class ClearDatabanksTask extends AbstractTask {
   private ArrayList<String> databanksToClear;

   public ClearDatabanksTask() throws Exception {
      super(null, null);
   }

   public ClearDatabanksTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "ClearDatabanks";
   }

   public String getName() {
      return L.tsq("Clear databanks");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      ClearDatabanksTask var3 = new ClearDatabanksTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public void start() throws Exception {
      this.project = ProjectEngine.get(this.projectName);
      this.databanksToClear = (ArrayList<String>)this.settingsData.getParams().get("DatabanksToClear");
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Clearing databanks...", new Object[0]));
      this.progressEngine.start();

      try {
         if (this.databanksToClear.contains("all")) {
            for (Databank var2 : this.project.getDatabanks().list()) {
               var2.clearRecords(
                  true,
                  true,
                  String.format(
                     "'Clear databanks task' %s/%s, clear all databanks - databank %s/%s", this.projectName, this.getTitle(), this.projectName, var2.getName()
                  )
               );
            }

            this.progressEngine.printToLog(L.t("Cleared all databanks", new Object[0]));
         } else {
            for (int var4 = 0; var4 < this.databanksToClear.size(); var4++) {
               Databank var5 = (Databank)this.project.getDatabanks().get(this.databanksToClear.get(var4));
               if (var5 == null) {
                  this.progressEngine.printToLog(L.t("Error: Databank %s not found", new Object[]{this.databanksToClear.get(var4)}));
               } else {
                  var5.clearRecords(
                     true,
                     true,
                     String.format(
                        "'Clear databanks task' %s/%s, clear selected databanks - databank %s/%s",
                        this.projectName,
                        this.getTitle(),
                        this.projectName,
                        var5.getName()
                     )
                  );
                  this.progressEngine.printToLog(L.t("Cleared databank '%s'", new Object[]{var5.getName()}));
               }
            }
         }
      } catch (OutOfMemoryError var3) {
         this.project.onMemoryError(var3);
      }

      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskClearDatabanks";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public String[] getSettings() {
      return new String[]{"ClearDatabanks"};
   }

   protected Databank[] getUsedDatabanks() {
      return null;
   }

   protected Databank getOutputDatabank() {
      return null;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      String var2 = this.databanksToClear.toString().replaceAll("[\\[.\\]]", "");
      var1.print(L.t("- Databanks cleared", new Object[0]) + ": " + var2);
      super.logTaskFinished(var1);
   }
}
