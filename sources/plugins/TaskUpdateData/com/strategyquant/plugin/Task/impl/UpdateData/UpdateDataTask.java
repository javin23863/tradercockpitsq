package com.strategyquant.plugin.Task.impl.UpdateData;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.data.download.NotDownloadableDataException;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.DataManagerDataProgress;
import com.strategyquant.tradinglib.project.websocket.MultiProgressListener;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONObject;

@Author(name = "Tamas Takacs")
@Name(name = "Update data")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class UpdateDataTask extends AbstractTask {
   private String logMessage;
   private List<DataInfo> symbolsToUpdate;
   private String type;

   public UpdateDataTask() throws Exception {
      super(null, null);
   }

   public UpdateDataTask(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   public String getType() {
      return "UpdateData";
   }

   public String getName() {
      return L.tsq("Update data");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      UpdateDataTask var3 = new UpdateDataTask(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   private void init() {
      this.type = (String)this.settingsData.getParams().get("Type");
      this.symbolsToUpdate = new ArrayList<>();
      if (this.type.equals("project")) {
         try {
            SQProject var1 = ProjectEngine.get(this.projectName);
            Element var2 = var1.getConfig();
            ArrayList var3 = var1.getTasks();

            for (int var4 = 0; var4 < var3.size(); var4++) {
               ISQTask var5 = (ISQTask)var3.get(var4);
               this.loadSymbols(var5.getConfig());
            }
         } catch (Exception var8) {
            Log.error("Error while parsing resources.", var8);
         }
      } else {
         try {
            ArrayList var9 = DataManager.list();

            for (int var10 = 0; var10 < var9.size(); var10++) {
               try {
                  DataInfo var11 = (DataInfo)var9.get(var10);
                  if (var11.sourceDataId <= 0) {
                     DownloadDispatcher.createListener(var11);
                     if (!this.symbolsToUpdate.contains(var11)) {
                        this.symbolsToUpdate.add(var11);
                     }
                  }
               } catch (NotDownloadableDataException var6) {
               }
            }
         } catch (Exception var7) {
            Log.error("Exc.", var7);
         }
      }
   }

   private void loadSymbols(Element var1) throws Exception {
      Element var2 = var1.getChild("Resources");
      if (var2 != null) {
         Element var3 = var2.getChild("Symbols");
         if (var3 != null) {
            List var4 = var3.getChildren("Symbol");

            for (int var5 = 0; var5 < var4.size(); var5++) {
               String var6 = ((Element)var4.get(var5)).getAttributeValue("name");
               DataInfo var7 = DataManager.getDataInfo("History", var6);
               if (var7 == null) {
                  this.progressEngine.printToLog(L.t("Symbol '%s' not found in DataManager.", new Object[]{var6}));
               } else {
                  if (var7.sourceDataId > 0) {
                     var7 = DataManager.getDataInfo("History", var7.sourceDataId);
                  }

                  if (var7 != null) {
                     try {
                        DownloadDispatcher.createListener(var7);
                        if (!this.symbolsToUpdate.contains(var7)) {
                           this.symbolsToUpdate.add(var7);
                        }
                     } catch (NotDownloadableDataException var9) {
                     }
                  }
               }
            }
         }
      }
   }

   public void start() throws Exception {
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.start();
      this.init();
      if (this.symbolsToUpdate.isEmpty()) {
         String var1 = L.t("No data found to update.", new Object[0]);
         this.logMessage = var1;
         this.progressEngine.printToLog(var1);
      } else {
         String var6 = L.t("Updating data...", new Object[0]);
         this.progressEngine.printToLog(var6);
         this.logMessage = var6;
         String var2 = "";

         for (int var3 = 0; var3 < this.symbolsToUpdate.size(); var3++) {
            DataInfo var4 = this.symbolsToUpdate.get(var3);
            String var5 = var4.symbol;
            var6 = L.t("Updating %s", new Object[]{var5});
            this.progressEngine.printToLog(var6);
            this.logMessage = this.logMessage + "\n" + var6;
            var2 = var2 + var5 + ",";
         }

         var2 = var2.substring(0, var2.length() - 1);
         HashMap var9 = new HashMap();
         var9.put("symbols", new String[]{var2});
         Program.get("DataManagerData").call("updateSelected", new Object[]{var9});
         (new Thread() {
               @Override
               public void run() {
                  HashMap var1 = new HashMap();

                  while (true) {
                     try {
                        Thread.sleep(5000L);
                     } catch (Exception var7) {
                     }

                     try {
                        for (int var3 = 0; var3 < UpdateDataTask.this.symbolsToUpdate.size(); var3++) {
                           String var4 = UpdateDataTask.this.symbolsToUpdate.get(var3).symbol;
                           MultiProgressListener var5 = DataManagerDataProgress.get().getListener(var4);
                           JSONObject var6x = var5 == null ? null : var5.getData();
                           if ((var5 == null || var6x != null && var6x.has("pct") && (var6x.getInt("pct") == 100 || var6x.getInt("pct") < 0))
                              && !var1.containsKey(var4)) {
                              if (var5 != null && (var6x == null || var6x.getInt("pct") != 100)) {
                                 if (var6x != null && var6x.getInt("pct") < 0 && var6x.has("message")) {
                                    String var9x = L.t("%s update failed - %s.", new Object[]{var4, var5.getData().getString("message")});
                                    UpdateDataTask.this.progressEngine.printToLog(var9x);
                                    UpdateDataTask.access$284(UpdateDataTask.this, "\n" + var9x);
                                    var1.put(var4, false);
                                 }
                              } else {
                                 String var2x = L.t("%s successfully updated.", new Object[]{var4});
                                 UpdateDataTask.this.progressEngine.printToLog(var2x);
                                 UpdateDataTask.access$284(UpdateDataTask.this, "\n" + var2x);
                                 var1.put(var4, true);
                              }
                           }
                        }

                        MultiProgressListener var11 = DataManagerDataProgress.get().getListener("TotalProgressSymbol");
                        JSONObject var12 = var11.getData();
                        if (var1.size() == UpdateDataTask.this.symbolsToUpdate.size() || var12 != null && var12.has("pct") && var12.getInt("pct") == 100) {
                           String var10 = L.t("All data updated.", new Object[0]);
                           UpdateDataTask.this.progressEngine.printToLog(var10);
                           UpdateDataTask.access$284(UpdateDataTask.this, "\n" + var10);
                           UpdateDataTask.this.progressEngine.resume();
                           return;
                        }
                     } catch (Exception var8) {
                        AbstractTask.Log.error("Exc.", var8);
                     }
                  }
               }
            })
            .start();
         this.progressEngine.printToLog(L.t("Project was paused until data download is finished.", new Object[0]));
         this.progressEngine.pause();
         this.progressEngine.checkPaused();
      }

      this.progressEngine.finish();
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskUpdateData";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public String[] getSettings() {
      return new String[]{"UpdateData"};
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
