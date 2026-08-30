package com.strategyquant.plugin.Task.impl.SaveToFiles;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DatabankExport;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectGlobalLog;
import com.strategyquant.tradinglib.project.TaskStat;
import com.strategyquant.tradinglib.taskImpl.AbstractTask;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;

@Author(name = "Tamas Takacs")
@Name(name = "Save to files plugin")
@Category(name = "Task")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SaveToFiles extends AbstractTask {
   private boolean overwriteFiles = false;
   private String destDirectorySqx = null;
   private String destDirectoryStr = null;
   private String destDirectoryDatabank = null;
   private String destDirectoryTrades = null;
   private String destDirectoryHtml = null;
   private String destDirectoryPdf = null;
   private String destDirectorySC = null;
   private boolean saveInSqxFormat = false;
   private boolean saveInStrFormat = false;
   private boolean exportDatabank = false;
   private boolean exportTrades = false;
   private boolean saveInHtmlFormat = false;
   private boolean saveInPdfFormat = false;
   private boolean saveSourceCode = false;
   private Databank databankSource = null;
   private int savedStr;
   private int savedSqx;
   private int savedDatabank;
   private int savedTrades;
   private int savedHtml;
   private int savedPdf;
   private int savedSc;
   private boolean MNActive;
   private int MNValue;
   private String data;
   private String format;
   private boolean useComma;
   private boolean setNoteActive;
   private String setNoteType;
   private String setNoteCustom;
   private String sourceCodeType;
   private SourceCodeGenerator generator;
   private String sourceCodeFormat;

   public SaveToFiles() throws Exception {
      super(null, null);
   }

   public SaveToFiles(String var1, ProgressEngine var2) throws Exception {
      super(var1, var2);
   }

   private void init() throws Exception {
      this.savedStr = 0;
      this.savedSqx = 0;
      this.savedDatabank = 0;
      this.savedTrades = 0;
      this.overwriteFiles = (Boolean)this.settingsData.getParams().get("OverwriteFiles");
      this.destDirectorySqx = (String)this.settingsData.getParams().get("DestDirectorySqx");
      this.destDirectoryStr = (String)this.settingsData.getParams().get("DestDirectoryStr");
      this.destDirectoryDatabank = (String)this.settingsData.getParams().get("DestDirectoryDatabank");
      this.destDirectoryTrades = (String)this.settingsData.getParams().get("DestDirectoryTrades");
      this.destDirectoryHtml = (String)this.settingsData.getParams().get("DestDirectoryHtml");
      this.destDirectoryPdf = (String)this.settingsData.getParams().get("DestDirectoryPdf");
      this.destDirectorySC = (String)this.settingsData.getParams().get("DestDirectorySC");
      this.saveInSqxFormat = (Boolean)this.settingsData.getParams().get("SaveInSqxFormat");
      this.saveInStrFormat = (Boolean)this.settingsData.getParams().get("SaveInStrFormat");
      this.exportDatabank = (Boolean)this.settingsData.getParams().get("ExportDatabank");
      this.exportTrades = (Boolean)this.settingsData.getParams().get("ExportTrades");
      this.saveInHtmlFormat = (Boolean)this.settingsData.getParams().get("SaveInHtmlFormat");
      this.saveInPdfFormat = (Boolean)this.settingsData.getParams().get("SaveInPdfFormat");
      this.saveSourceCode = (Boolean)this.settingsData.getParams().get("SaveSourceCode");
      this.databankSource = (Databank)this.settingsData.getParams().get("DatabankSource");
      this.MNActive = (Boolean)this.settingsData.getParams().get("MNActive");
      this.MNValue = (Integer)this.settingsData.getParams().get("MNValue");
      this.data = (String)this.settingsData.getParams().get("Data");
      this.format = (String)this.settingsData.getParams().get("Format");
      this.useComma = (Boolean)this.settingsData.getParams().get("UseComma");
      this.setNoteActive = (Boolean)this.settingsData.getParams().get("SetNoteActive");
      this.setNoteType = (String)this.settingsData.getParams().get("SetNoteType");
      this.setNoteCustom = (String)this.settingsData.getParams().get("SetNoteCustom");
      this.sourceCodeType = (String)this.settingsData.getParams().get("SourceCodeType");
      if (!this.sourceCodeType.equals("Strategy XML")) {
         this.generator = SourceCodeGenerators.getInstance().getGeneratorFromName(this.sourceCodeType);
         if (this.generator == null) {
            throw new Exception(L.t("Invalid source code generator", new Object[]{false}));
         }
      }

      this.sourceCodeFormat = (String)this.settingsData.getParams().get("SourceCodeFormat");
   }

   public void start() throws Exception {
      this.init();
      this.progressEngine.setLogPrefix(this.taskLogPrefix);
      this.progressEngine.printToLog(L.t("Starting...", new Object[0]));
      this.progressEngine.start();
      this.project = ProjectEngine.get(this.projectName);
      HashMap var1 = new HashMap();
      var1.put("useComma", new String[]{this.useComma + ""});
      var1.put("data", new String[]{this.data});
      var1.put("setNoteActive", new String[]{this.setNoteActive + ""});
      var1.put("setNoteType", new String[]{this.setNoteType});
      var1.put("setNoteCustomText", new String[]{this.setNoteCustom});
      int var3 = 0;
      String var4 = L.tsq("Input");
      ArrayList var5 = this.databankSource.getRecordKeys();
      this.project.setGlobalStats(TaskStat.BAR, this.getCustomName().hashCode(), var4, var5.size(), ++var3);
      if (this.saveInSqxFormat || this.saveInStrFormat || this.exportTrades || this.saveInHtmlFormat || this.saveInPdfFormat || this.saveSourceCode) {
         if (this.saveInSqxFormat) {
            this.project.setGlobalStats(TaskStat.BAR, this.getCustomName().hashCode(), L.tsq("SQX"), var5.size(), ++var3, var4);
         }

         if (this.saveInStrFormat) {
            this.project.setGlobalStats(TaskStat.BAR, this.getCustomName().hashCode(), L.tsq("STR"), var5.size(), ++var3, var4);
         }

         if (this.exportTrades) {
            this.project.setGlobalStats(TaskStat.BAR, this.getCustomName().hashCode(), L.tsq("Trades"), var5.size(), ++var3, var4);
         }

         if (this.saveInHtmlFormat) {
            this.project.setGlobalStats(TaskStat.BAR, this.getCustomName().hashCode(), L.tsq("HTML"), var5.size(), ++var3, var4);
         }

         if (this.saveInPdfFormat) {
            this.project.setGlobalStats(TaskStat.BAR, this.getCustomName().hashCode(), L.tsq("PDF"), var5.size(), ++var3, var4);
         }

         if (this.saveSourceCode) {
            this.project.setGlobalStats(TaskStat.BAR, this.getCustomName().hashCode(), L.tsq("Code"), var5.size(), ++var3, var4);
         }

         for (int var6 = var5.size() - 1; var6 >= 0; var6--) {
            String var7 = (String)var5.get(var6);

            try {
               ResultsGroup var8 = null;

               try {
                  var8 = this.databankSource.getLocked(var7, "onSaveToFiles");
                  if (this.saveInSqxFormat) {
                     try {
                        String var2;
                        if (this.overwriteFiles) {
                           var2 = this.destDirectorySqx + "/" + var7 + ".sqx";
                        } else {
                           var2 = this.generateUniqueName(this.destDirectorySqx, var7, ".sqx");
                        }

                        this.databankSource.getReportSaver().save(var8, "sqx", var2, this.MNActive, this.MNValue, var1);
                        this.progressEngine.printToLogDebug(L.t("%s saved in SQX format", new Object[]{var7}));
                        this.savedSqx++;
                     } catch (Exception var28) {
                        Log.error("Error while saving strategy '" + var7 + "' to SQX", var28);
                     }
                  }

                  if (this.saveInStrFormat) {
                     try {
                        String var33;
                        if (this.overwriteFiles) {
                           var33 = this.destDirectoryStr + "/" + var7 + ".str";
                        } else {
                           var33 = this.generateUniqueName(this.destDirectoryStr, var7, ".str");
                        }

                        this.databankSource.getReportSaver().save(var8, "str", var33, this.MNActive, this.MNValue, var1);
                        this.progressEngine.printToLogDebug(L.t("%s saved in STR (SQ3) format", new Object[]{var7}));
                        this.savedStr++;
                     } catch (Exception var27) {
                        Log.error("Error while saving strategy '" + var7 + "' to STR", var27);
                     }
                  }

                  if (this.exportTrades) {
                     try {
                        String var34;
                        if (this.overwriteFiles) {
                           var34 = this.destDirectoryTrades + "/" + var7 + "." + this.format;
                        } else {
                           var34 = this.generateUniqueName(this.destDirectoryTrades, var7, "." + this.format);
                        }

                        this.databankSource.getReportSaver().save(var8, this.format, var34, this.MNActive, this.MNValue, var1);
                        this.progressEngine.printToLogDebug(L.t("%s saved trades", new Object[]{var7}));
                        this.savedTrades++;
                     } catch (Exception var26) {
                        Log.error("Error while saving trades of strategy '" + var7 + "'", var26);
                     }
                  }

                  if (this.saveInHtmlFormat) {
                     try {
                        String var35;
                        if (this.overwriteFiles) {
                           var35 = this.destDirectoryHtml + "/" + var7 + ".html";
                        } else {
                           var35 = this.generateUniqueName(this.destDirectoryHtml, var7, ".html");
                        }

                        this.databankSource.getReportSaver().save(var8, "html", var35, this.MNActive, this.MNValue, var1);
                        this.progressEngine.printToLogDebug(L.t("%s saved in HTML format", new Object[]{var7}));
                        this.savedHtml++;
                     } catch (Exception var25) {
                        Log.error("Error while saving strategy '" + var7 + "' to HTML", var25);
                     }
                  }

                  if (this.saveInPdfFormat) {
                     try {
                        String var36;
                        if (this.overwriteFiles) {
                           var36 = this.destDirectoryPdf + "/" + var7 + ".pdf";
                        } else {
                           var36 = this.generateUniqueName(this.destDirectoryPdf, var7, ".pdf");
                        }

                        this.databankSource.getReportSaver().save(var8, "pdf", var36, this.MNActive, this.MNValue, var1);
                        this.progressEngine.printToLogDebug(L.t("%s saved in PDF format", new Object[]{var7}));
                        this.savedPdf++;
                     } catch (Exception var24) {
                        Log.error("Error while saving strategy '" + var7 + "' to PDF", var24);
                     }
                  }

                  if (this.saveSourceCode) {
                     try {
                        String var9 = null;
                        if (this.sourceCodeType.equals("Strategy XML")) {
                           var9 = "xml";
                        } else {
                           var9 = this.generator.getFileExtension()[1];
                        }

                        if (this.sourceCodeFormat != null) {
                           var9 = this.sourceCodeFormat;
                        }

                        String var37;
                        if (this.overwriteFiles) {
                           var37 = this.destDirectorySC + "/" + (var9.contains("java") ? SQUtils.correctClassName(var7) : var7) + "." + var9;
                        } else {
                           var37 = this.generateUniqueName(this.destDirectorySC, var7, "." + var9);
                        }

                        this.databankSource.getReportSaver().save(var8, var9, var37, this.MNActive, this.MNValue, this.generator, var1, false);
                        this.progressEngine.printToLogDebug(L.t("%s saved Source Code .%s", new Object[]{var7, var9}));
                        this.savedSc++;
                     } catch (Exception var23) {
                        Log.error("Error while saving strategy '" + var7 + "' Source Code", var23);
                     }
                  }
               } catch (Exception var29) {
                  Log.warn("Strategy '" + var7 + "' no longer exists (was probably replaced by better strategy with the same fingerprint)");
                  continue;
               } finally {
                  if (var8 != null) {
                     var8.releaseLock("onSaveToFiles");
                  }
               }

               this.progressEngine.checkPaused();
               if (this.progressEngine.isStopped()) {
                  break;
               }
            } catch (OutOfMemoryError var31) {
               this.project.onMemoryError(var31);
               break;
            } catch (Exception var32) {
               this.progressEngine.printToLog(L.t("%s cannot be saved. Error: %s", new Object[]{var7, var32.getMessage()}));
               Log.error("Error while saving strategy. Exc.", var32);
            }
         }
      }

      if (this.exportDatabank) {
         try {
            String var38;
            if (this.overwriteFiles) {
               var38 = this.destDirectoryDatabank + "/DatabankExport." + this.format;
            } else {
               var38 = this.generateUniqueName(this.destDirectoryDatabank, "DatabankExport", "." + this.format);
            }

            DatabankExport var41 = new DatabankExport();
            if (var38.endsWith("xlsx")) {
               var41.toXlsx(this.databankSource, var38, null, this.useComma, null);
            } else {
               var41.toCsv(this.databankSource, var38, null, this.useComma, null, ";");
            }

            this.progressEngine.printToLogDebug(L.t("Exported databank contents", new Object[0]));
            this.savedDatabank++;
         } catch (Exception var22) {
            Log.error("Error while exporting databank contents", var22);
         }
      }

      this.progressEngine.finish();
   }

   private String generateUniqueName(String var1, String var2, String var3) {
      String var4 = var1 + "/" + (var3.contains("java") ? SQUtils.correctClassName(var2) : var2) + var3;
      if (!new File(var4).exists()) {
         return var4;
      }

      int var5 = 1;

      while (true) {
         var4 = var1 + "/" + (var3.contains("java") ? SQUtils.correctClassName(var2 + "(" + var5 + ")") : var2 + "(" + var5 + ")") + var3;
         if (!new File(var4).exists()) {
            return var4;
         }

         var5++;
      }
   }

   protected int getRunningStatus() {
      return 0;
   }

   public String getPluginFolderName() {
      return "TaskSaveToFiles";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public String getType() {
      return "SaveToFiles";
   }

   public String getName() {
      return L.tsq("Save to files");
   }

   public ISQTask clone(String var1, ProgressEngine var2) throws Exception {
      SaveToFiles var3 = new SaveToFiles(var1, var2);
      var3.settingTabPlugins = this.settingTabPlugins;
      return var3;
   }

   public String[] getSettings() {
      return new String[]{"SaveToFiles"};
   }

   protected Databank[] getUsedDatabanks() {
      return new Databank[]{this.databankSource};
   }

   protected Databank getOutputDatabank() {
      return null;
   }

   public void logTaskFinished(ProjectGlobalLog var1) {
      super.logTaskFinished(var1);
      var1.print(L.t("Saved strategies in SQX format", new Object[0]) + ": " + this.savedSqx);
      var1.print(L.t("Saved strategies in STR (SQ3) format", new Object[0]) + ": " + this.savedStr);
      var1.print(L.t("Exported databank contents", new Object[0]) + ": " + this.savedDatabank);
      var1.print(L.t("Exported trades", new Object[0]) + ": " + this.savedTrades);
      var1.print(L.t("Saved strategies in HTML format", new Object[0]) + ": " + this.savedHtml);
      var1.print(L.t("Saved strategies in PDF format", new Object[0]) + ": " + this.savedPdf);
      var1.print(L.t("Saved Source Codes", new Object[0]) + ": " + this.savedSc);
   }
}
