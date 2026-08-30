package com.strategyquant.plugin.Task.impl.CallExternalScript;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Call external script")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class CallExternalScript extends AbstractTask {
   private String file;
   private String params;
   private String type;
   private long runTime;

   public CallExternalScript() throws Exception {
      super(null, null);
   }

   public CallExternalScript(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "CallExternalScript";
   }

   public String getName() {
      return L.tsq("Call external script");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      CallExternalScript var3 = new CallExternalScript(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   private void init() {
      this.file = (String)this.settingsData.getParams().get("File");
      this.params = (String)this.settingsData.getParams().get("Params");
      this.type = (String)this.settingsData.getParams().get("Type");
      this.runTime = (Long)this.settingsData.getParams().get("MaxBuildRunTime");
   }

   public void start() throws Exception {
      this.init();
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.start();
      File var1 = new File(this.file);
      if (var1.exists()) {
         this.printLog(String.format("Executing script '%s'", var1.getName()));
         String var2 = this.file;
         if (this.params != null && !this.params.isBlank()) {
            var2 = var2 + " " + this.params;
         }

         Process var3 = Runtime.getRuntime().exec(var2);
         if (this.type.equals("time")) {
            this.printLog(L.t("Project is paused, waiting for %s", new Object[]{SQTime.formatDateTime(this.runTime / 1000L)}));
            Timer var4 = new Timer();
            var4.schedule(new TimerTask() {
               @Override
               public void run() {
                  CallExternalScript.this.progressEngine.resume();
               }
            }, this.runTime);
            this.progressEngine.pause();
            this.progressEngine.checkPaused();
         } else {
            this.printLog(L.t("Project is paused, waiting for script.", new Object[0]));
            var3.waitFor();
            this.printLog(L.t("Script done.", new Object[0]));
            this.progressEngine.resume();
         }
      } else {
         this.printLog(String.format("Script '%s' doesn't exist.", var1.getName()));
      }

      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskCallExternalScript";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public String[] getSettings() {
      return new String[]{"CallExternalScript"};
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
