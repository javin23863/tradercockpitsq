package com.strategyquant.plugin.Task.impl.DeleteFile;

import com.strategyquant.lib.L;
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
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Delete file")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class DeleteFileTask extends AbstractTask {
   private String logMessage;
   private String file;

   public DeleteFileTask() throws Exception {
      super(null, null);
   }

   public DeleteFileTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "DeleteFile";
   }

   public String getName() {
      return L.tsq("Delete file");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      DeleteFileTask var3 = new DeleteFileTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   private void init() {
      this.file = (String)this.settingsData.getParams().get("File");
   }

   public void start() throws Exception {
      this.init();
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Deleting file...", new Object[0]));
      this.progressEngine.start();

      try {
         File var1 = new File(this.file);
         if (!var1.exists()) {
            throw new Exception(L.t("File '%s' not found.", new Object[]{var1.getName()}));
         }

         if (!var1.delete()) {
            throw new Exception(L.t("File '%s' cannot be deleted.", new Object[]{var1.getName()}));
         }

         this.progressEngine.printToLog(L.t("File '%s' deleted.", new Object[]{var1.getName()}));
         this.logMessage = L.t("File '%s' deleted.", new Object[]{var1.getName()});
      } catch (Exception var2) {
         Log.error("Error while loading strategies. Exc.", var2);
         this.progressEngine.printToLog(var2.getMessage());
         this.logMessage = var2.getMessage();
      }

      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskDeleteFile";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public String[] getSettings() {
      return new String[]{"DeleteFile"};
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
