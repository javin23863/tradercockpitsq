package com.strategyquant.plugin.Task.impl.WaitFor;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.json.JSONObject;

@Author(name = "Tamas Takacs")
@Name(name = "Wait for user/file")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class WaitForTask extends AbstractTask {
   private String logMessage;
   private String file;
   private String action;

   public WaitForTask() throws Exception {
      super(null, null);
   }

   public WaitForTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "WaitFor";
   }

   public String getName() {
      return L.tsq("Wait for user/file");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      WaitForTask var3 = new WaitForTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   private void init() {
      this.file = (String)this.settingsData.getParams().get("File");
      this.action = (String)this.settingsData.getParams().get("ActionType");
   }

   public void start() throws Exception {
      this.init();
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.start();
      this.logMessage = null;
      boolean var1 = false;
      if (this.action.equals("user")) {
         var1 = true;
         this.logMessage = L.t("Project is paused, waiting for your action.", new Object[0]);
         JSONObject var2 = new JSONObject();
         var2.put("projectName", this.projectName);
         var2.put("taskName", this.customName);
         var2.put("projectPaused", true);
         DataToSend var3 = new DataToSend("customProjectNotification", var2);
         SQWebSocketManager.addToDataQueue(var3, new String[]{"SQUANT"});
      } else {
         final File var4 = new File(this.file);
         if (var4.exists()) {
            this.logMessage = L.t("File '%s' exists. Project continues with the next task.", new Object[]{var4.getName()});
         } else {
            var1 = true;
            this.logMessage = L.t("Project is paused, waiting for file '%s'.", new Object[]{var4.getName()});
            (new Thread() {
               @Override
               public void run() {
                  while (true) {
                     try {
                        Thread.sleep(1000L);
                     } catch (Exception var3) {
                     }

                     try {
                        if (var4.exists()) {
                           String var1x = L.t("File '%s' exists.", new Object[]{var4.getName()});
                           WaitForTask.access$084(WaitForTask.this, "\n" + var1x);
                           WaitForTask.this.progressEngine.printToLog(var1x);
                           WaitForTask.this.progressEngine.resume();
                           return;
                        }
                     } catch (Exception var2) {
                        AbstractTask.Log.error("Exc.", var2);
                     }
                  }
               }
            }).start();
         }
      }

      this.progressEngine.printToLog(this.logMessage);
      if (var1) {
         this.progressEngine.pause();
         this.progressEngine.checkPaused();
      }

      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskWaitFor";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public String[] getSettings() {
      return new String[]{"WaitFor"};
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
