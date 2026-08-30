package com.strategyquant.plugin.Task.impl.Notification;

import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.EmailSender;
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
import java.io.Serializable;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.json.JSONObject;

@Author(name = "Tamas Takacs")
@Name(name = "NotificationTask plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class NotificationTask extends AbstractTask {
   private String type;
   private String email;
   private boolean pause;
   private String smtpServer;
   private int smtpPort;
   private String smtpUsername;
   private String smtpPassword;
   private String smtpEmailFrom;
   private boolean smtpSSL;
   private String logMessage;

   public NotificationTask() throws Exception {
      super(null, null);
   }

   public NotificationTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "Notification";
   }

   public String getName() {
      return L.tsq("Notification");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      NotificationTask var3 = new NotificationTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   private void init() {
      this.type = (String)this.settingsData.getParams().get("Type");
      this.email = (String)this.settingsData.getParams().get("Email");
      this.pause = (Boolean)this.settingsData.getParams().get("PauseProject");
      if (this.type.toLowerCase().equals("email")) {
         this.smtpServer = (String)this.settingsData.getParams().get("SMTPServer");
         this.smtpPort = (Integer)this.settingsData.getParams().get("SMTPPort");
         this.smtpUsername = (String)this.settingsData.getParams().get("SMTPUsername");
         this.smtpPassword = (String)this.settingsData.getParams().get("SMTPPassword");
         this.smtpEmailFrom = (String)this.settingsData.getParams().get("SMTPEmailFrom");
         this.smtpSSL = (Boolean)((Serializable)this.settingsData.getParams().get("SMTPUseSSL"));
      }
   }

   public void start() throws Exception {
      this.init();
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Processing notification...", new Object[0]));
      this.progressEngine.start();
      this.logMessage = null;
      String var1 = L.t("Task notification for project %s", new Object[]{this.projectName});
      if (this.pause) {
         var1 = var1
            + "\n\n"
            + L.t("Note - Project is paused, waiting for your action.", new Object[0])
            + "\n"
            + L.t("Please don't reply to this email, it is not watched!", new Object[0])
            + "\n";
      }

      if (this.type.toLowerCase().equals("email")) {
         try {
            EmailSender.sendTo(
               this.smtpServer,
               this.smtpPort,
               this.smtpUsername,
               this.smtpPassword,
               L.t("SQ Task notification for project %s", new Object[]{this.projectName}),
               var1,
               this.smtpEmailFrom,
               this.email,
               this.smtpSSL
            );
            this.logMessage = L.t("Task notification was successfully sent.", new Object[0]);
            this.progressEngine.printToLog(this.logMessage);
         } catch (Exception var4) {
            Log.error("Exc.", var4);
            this.logMessage = L.t("Task notification could not be sent. Please check your SMTP settings.", new Object[0]);
            this.progressEngine.printToLog(this.logMessage);
         }
      } else if (this.type.toLowerCase().equals("popup")) {
         JSONObject var2 = new JSONObject();
         var2.put("projectName", this.projectName);
         var2.put("taskName", this.customName);
         var2.put("projectPaused", this.pause);
         DataToSend var3 = new DataToSend("customProjectNotification", var2);
         SQWebSocketManager.addToDataQueue(var3, new String[]{"SQUANT"});
      }

      if (this.pause) {
         String var5 = L.t("Project is paused, waiting for your action.", new Object[0]);
         this.logMessage = this.logMessage + "\n" + var5;
         this.progressEngine.printToLog(var5);
         this.progressEngine.pause();
         this.progressEngine.checkPaused();
      }

      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskNotification";
   }

   public int getPreferredPosition() {
      return 5;
   }

   public String[] getSettings() {
      return new String[]{"Notification"};
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
