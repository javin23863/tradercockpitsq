package com.strategyquant.plugin.Task.impl.LoadFromFiles;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.apache.commons.io.FileUtils;

@Author(name = "Tamas Takacs")
@Name(name = "Load from files plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class LoadFromFiles extends AbstractTask {
   private static final int OnMissingInstrumentSkipStrategy = 0;
   private static final int OnMissingInstrumentUseInstrument = 0;
   private String sourceDirectory = null;
   private boolean includeSubdirectories = false;
   private Databank databankTarget = null;
   private int unrecognizedInstrumentAction;
   private String unrecognizedInstrument;
   private int loaded;

   public LoadFromFiles() throws Exception {
      super(null, null);
   }

   public LoadFromFiles(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   private void init() {
      this.sourceDirectory = (String)this.settingsData.getParams().get("SourceDirectory");
      this.includeSubdirectories = (Boolean)this.settingsData.getParams().get("IncludeSubdirectories");
      this.databankTarget = (Databank)this.settingsData.getParams().get("DatabankTarget");
      this.unrecognizedInstrumentAction = (Integer)this.settingsData.getParams().get("UnrecognizedInstrumentAction");
      this.unrecognizedInstrument = (String)this.settingsData.getParams().get("Instrument");
   }

   public void start() throws Exception {
      this.init();
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Starting...", new Object[0]));
      this.progressEngine.start();
      this.project = ProjectEngine.get(this.projectName);
      this.project.bestResults.disable();
      IProgram var2 = Program.get("Loader");

      try {
         this.project.publisher.resetLastData("progress-channel");

         for (File var5 : FileUtils.listFiles(new File(this.sourceDirectory), new String[]{"sqx", "str"}, this.includeSubdirectories)) {
            if (!var5.isDirectory()) {
               String var1 = SQUtils.stripExtension(var5.getName());

               try {
                  ResultsGroup var6 = (ResultsGroup)var2.call(
                     "loadFile", new Object[]{var5.getAbsolutePath(), this.unrecognizedInstrumentAction == 0, this.unrecognizedInstrument}
                  );
                  if (var6 != null) {
                     this.databankTarget.add(var6, true);
                     this.databankTarget.updateBestResults(var6);
                     this.progressEngine.printToLogDebug(L.t("%s loaded", new Object[]{var6.getName()}));
                     this.loaded++;
                  }

                  this.progressEngine.checkPaused();
                  if (this.progressEngine.isStopped()) {
                     break;
                  }
               } catch (OutOfMemoryError var7) {
                  this.project.onMemoryError(var7);
                  break;
               } catch (Exception var8) {
                  this.progressEngine.printToLog(L.t("%s cannot be loaded. Error: %s", new Object[]{var1, var8.getMessage()}));
                  Log.error("Error while loading strategy. Exc.", var8);
               }
            }
         }
      } catch (Exception var9) {
         Log.error("Error while loading strategies. Exc.", var9);
      }

      this.project.bestResults.enable();
      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskLoadFromFiles";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public String getType() {
      return "LoadFromFiles";
   }

   public String getName() {
      return L.tsq("Load from files");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      LoadFromFiles var3 = new LoadFromFiles(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public String[] getSettings() {
      return new String[]{"LoadFromFiles"};
   }

   protected Databank[] getUsedDatabanks() {
      return new Databank[]{this.databankTarget};
   }

   protected Databank getOutputDatabank() {
      return this.databankTarget;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      super.logTaskFinished(var1);
      var1.print(L.t("Loaded strategies", new Object[0]) + ": " + this.loaded);
   }
}
