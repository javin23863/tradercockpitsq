package com.strategyquant.plugin.Servlet.impl.Project;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.lib.memory.SQMemoryProtectionError;
import com.strategyquant.lib.utils.JsonCreator;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategiesActionCache;
import com.strategyquant.tradinglib.applyMassConfig.ApplyMassConfig;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisInfo;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisMethodsList;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DefaultDatabankFilter;
import com.strategyquant.tradinglib.databank.IDatabankFilter;
import com.strategyquant.tradinglib.databank.IProgressListener;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.file.SQFileManager;
import com.strategyquant.tradinglib.project.DefaultProjectConfig;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectCustomResources;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.console.CLILogger;
import com.strategyquant.tradinglib.project.websocket.WebSocketNotification;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.DataItemsGenerator;
import com.strategyquant.tradinglib.strategy.StrategyLoader;
import com.strategyquant.tradinglib.strategy.StrategyMerger;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.stream.Collectors;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(ProjectServlet.class);
   private static final String ApplyMassConfigPath = SQPaths.settingsDirPath + "/ApplyMassConfig.xml";
   private static final String LockCustomAnalysis = "CustomAnalysisManual";
   private OperatingSystem os = new OperatingSystem();

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "start":
            return this.onStart(var2);
         case "pause":
            return this.onPause(var2);
         case "stop":
            return this.onStop(var2);
         case "resume":
            return this.onResume(var2);
         case "projectChanged":
            return this.onProjectChanged(var2);
         case "databankList":
            return this.onDatabankList(var2);
         case "databankCount":
            return this.onDatabankCount(var2);
         case "listStrategies":
            return this.onListStrategies(var2);
         case "getStrategyStats":
            return this.onGetStrategyStats(var2);
         case "moveDatabank":
            return this.onMoveDatabank(var2);
         case "moveDatabankToPosition":
            return this.onMoveDatabankToPosition(var2);
         case "createDatabank":
            return this.onCreateDatabank(var2);
         case "removeDatabank":
            return this.onRemoveDatabank(var2);
         case "renameDatabank":
            return this.onRenameDatabank(var2);
         case "loadFilesToDatabank":
            return this.onLoadFilesToDatabank(var2);
         case "saveReports":
            return this.onSaveReports(var2);
         case "saveSingleFileJava":
            return this.onSaveSingleFileJava(var2);
         case "removeReports":
            return this.onRemoveReports(var2);
         case "createPortfolio":
            return this.onCreatePortfolio(var2);
         case "removeAllReports":
            return this.onRemoveAllReports(var2);
         case "clearAllDatabanks":
            return this.onClearAllDatabanks(var2);
         case "cancelRecordsLoading":
            return this.onCancelRecordsLoading(var2);
         case "getDataItems":
            return this.onGetDataItems(var2);
         case "getRankColumns":
            return this.onGetRankColumns(var2);
         case "changeMaxRecords":
            return this.onChangeMaxRecords(var2);
         case "getXMLConfig":
         case "getConfig":
            return this.onGetConfig(var2);
         case "checkResources":
            return this.onCheckResources(var2);
         case "updateTaskXML":
            return this.onUpdateTaskXML(var2);
         case "updateProjectXML":
            return this.onUpdateProjectXML(var2);
         case "getTemplateConfig":
            return this.onGetTemplateConfig(var2);
         case "loadConfig":
            return this.onLoadConfig(var2);
         case "saveConfig":
            return this.onSaveConfig(var2);
         case "getCurrentTemplateSettings":
            return this.onGetCurrentTemplateSettings(var2);
         case "updateTemplateConfig":
            return this.onUpdateTemplateConfig(var2);
         case "applySimpleTemplate":
            return this.onApplySimpleTemplate(var2);
         case "resetTaskConfig":
            return this.onResetTaskConfig(var2);
         case "instrumentSelected":
            return this.onInstrumentSelected(var2);
         case "retest":
            return this.onRetest(var2);
         case "confirm":
            return this.onConfirm(var2);
         case "loadGridData":
            return this.onLoadGridData(var2);
         case "setDatabankColumnValue":
            return this.onSetDatabankColumnValue(var2);
         case "setDatabankSynchronization":
            return this.onSetDatabankSynchronization(var2);
         case "synchronizeDatabank":
            return this.onSynchronizeDatabank(var2);
         case "synchronizeDatabanks":
            return this.onSynchronizeDatabanks(var2);
         case "syncfromfiles":
            return this.onSyncfromfiles(var2);
         case "synchronizeAllDatabanks":
            return this.onSynchronizeAllDatabanks(var2);
         case "printToLog":
            return this.onPrintToLog(var2);
         case "cutStrategies":
            return this.onCutStrategies(var2);
         case "copyStrategies":
            return this.onCopyStrategies(var2);
         case "pasteStrategies":
            return this.onPasteStrategies(var2);
         case "resolveResources":
            return this.onResolveResources(var2);
         case "cancelResolving":
            return this.onCancelResolving();
         case "checkCustomResources":
            return this.onCheckCustomResources(var2);
         case "checkTemplateResources":
            return this.onCheckTemplateResources(var2);
         case "resolveCustomResources":
            return this.onResolveCustomResources(var2);
         case "list":
            return this.onList();
         case "status":
            return this.onStatus(var2);
         case "mergeStrategies":
            return this.onMergeStrategies(var2);
         case "splitStrategies":
            return this.onSplitStrategies(var2);
         case "mergeWFStrategies":
            return this.onMergeWFStrategies(var2);
         case "moveStrategiesToPM":
            return this.onMoveToPM(var2);
         case "moveStrategiesToPC":
            return this.onMoveToPC(var2);
         case "applyMassConfig":
            return this.onApplyMassConfig(var2);
         case "applyMassConfigLoad":
            return this.onApplyMassConfigLoad(var2);
         case "runCustomAnalysis":
            return this.onRunCustomAnalysis(var2);
         case "setNote":
            return this.onSetNote(var2);
         default:
            throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
      }
   }

   private CustomAnalysisInfo getCustomAnalysisInfo(Map<String, String[]> var1) throws Exception {
      CustomAnalysisInfo var2 = new CustomAnalysisInfo();
      var2.filter = Boolean.valueOf(this.tryGetParamValue(var1, "filter"));
      var2.inputArgsPerStrategy1 = this.tryGetParamValue(var1, "inputArgsPerStrategy1");
      var2.inputArgsFullDatabank1 = this.tryGetParamValue(var1, "inputArgsFullDatabank1");
      String var3 = this.tryGetParamValue(var1, "analyseType");
      String var4 = this.tryGetParamValue(var1, "perStrategy1");
      if (!var4.equals("none") && "strategy".equals(var3)) {
         var2.perStrategy1 = (CustomAnalysisMethod)CustomAnalysisMethodsList.get().findClassByName(var4);
      }

      var4 = this.tryGetParamValue(var1, "fullDatabank1");
      if (!var4.equals("none") && "databank".equals(var3)) {
         var2.fullDatabank1 = (CustomAnalysisMethod)CustomAnalysisMethodsList.get().findClassByName(var4);
      }

      return var2;
   }

   private String onRunCustomAnalysis(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      CustomAnalysisInfo var3 = this.getCustomAnalysisInfo(var1);
      String var4 = this.tryGetParam(var1, "projectName")[0];
      String var5 = this.tryGetParam(var1, "databankName")[0];
      SQProject var6 = ProjectEngine.get(var4);
      Databank var7 = (Databank)var6.getDatabanks().get(var5);
      boolean var8 = Boolean.valueOf(this.tryGetParam(var1, "selectedStrategies")[0]);
      ArrayList var9 = var7.getRecords();
      if (var8) {
         String[] var10 = this.tryGetParam(var1, "strategies[]");
         HashSet var11 = new HashSet<>(Arrays.asList(var10));
         ArrayList var12 = new ArrayList();

         for (ResultsGroup var14 : var9) {
            String var15 = var14.getName();
            if (var11.contains(var15)) {
               var12.add(var14);
            }
         }

         var9 = var12;
      }

      String var17 = this.tryGetParam(var1, "analyseType")[0];
      LinkedList var18 = new LinkedList();

      try {
         if ("strategy".equals(var17) && var3.perStrategy1 != null) {
            for (ResultsGroup var22 : var9) {
               var3.perStrategy1.setInputArgs(var3.inputArgsPerStrategy1);
               if (!var3.perStrategy1.filterStrategy(var4, "manual", var5, var22)) {
                  var18.add(var22);
               }
            }
         }

         if ("databank".equals(var17) && var3.fullDatabank1 != null) {
            var3.fullDatabank1.setInputArgs(var3.inputArgsFullDatabank1);
            ArrayList var20 = var3.fullDatabank1.processDatabank(var4, "manual", var5, var9);
            Set var23 = var20.stream().map(ResultsGroup::getName).collect(Collectors.toSet());

            for (ResultsGroup var26 : var9) {
               if (!var23.contains(var26.getName())) {
                  var18.add(var26);
               }
            }
         }

         if (var3.filter) {
            for (ResultsGroup var24 : var18) {
               var7.remove(var24.getName(), true, true, true, true, "CustomAnalysisManual");
            }
         }
      } catch (Exception var16) {
         return apiErrorJSON(L.t("Custom analysis failed.", new Object[0]), var16);
      }

      var2.put("success", L.t("Custom analysis executed.", new Object[0]));
      return var2.toString();
   }

   private String onSetNote(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "note")[0];
      String var4 = this.tryGetParam(var1, "projectName")[0];
      String var5 = this.tryGetParam(var1, "databankName")[0];
      SQProject var6 = ProjectEngine.get(var4);
      Databank var7 = (Databank)var6.getDatabanks().get(var5);
      ArrayList var8 = var7.getRecords();
      String[] var9 = this.tryGetParam(var1, "strategies[]");
      HashSet var10 = new HashSet<>(Arrays.asList(var9));
      ArrayList var11 = new ArrayList();

      for (ResultsGroup var13 : var8) {
         String var14 = var13.getName();
         if (var10.contains(var14)) {
            var11.add(var13);
         }
      }

      var8 = var11;

      try {
         for (ResultsGroup var18 : var8) {
            var18.setNote(var3);
            var7.update(var18.getName(), var18, true, "setNote");
         }
      } catch (Exception var15) {
         return apiErrorJSON(L.t("Set note failed.", new Object[0]), var15);
      }

      var7.refreshGrid();
      var2.put("success", L.t("Set note executed.", new Object[0]));
      return var2.toString();
   }

   private String onApplyMassConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "xmlConfig")[0];
         SQProject var5 = ProjectEngine.get(var3);
         Element var6 = XMLUtil.stringToElement(var4);
         XMLUtil.xmlToFile(var6, new File(ApplyMassConfigPath));
         ApplyMassConfig var7 = new ApplyMassConfig();
         var7.init(var5, var6);
         var7.applyChangesToTasks(var5, null);
         SQFileManager.updateWholeProjectConfig(var5);
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot apply mass config.", new Object[0]), var8);
      }

      var2.put("success", L.t("Mass config applied.", new Object[0]));
      return var2.toString();
   }

   private String onApplyMassConfigLoad(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         File var3 = new File(ApplyMassConfigPath);
         Element var4 = var3.exists() ? XMLUtil.fileToXmlElement(var3) : new Element("ApplyMassConfig");
         var2.put("xmlConfig", XMLUtil.elementToString(var4));
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot load last apply mass config.", new Object[0]), var5);
      }

      var2.put("success", L.t("Mass config loaded.", new Object[0]));
      return var2.toString();
   }

   private String onSynchronizeAllDatabanks(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         for (SQProject var5 : ProjectEngine.list()) {
            for (Databank var8 : var5.getDatabanks().list()) {
               var8.synchronizeNow(String.format("API request project/synchronizeAllDatabanks %s/%s", var5.getName(), var8.getName()));
            }
         }
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot synchronize databanks.", new Object[0]), var9);
      }

      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         var2.put("success", L.t("Synchronization finished.", new Object[0]));
      } else {
         var2.put("success", L.t("Synchronization started.", new Object[0]));
      }

      return var2.toString();
   }

   private String onSynchronizeDatabanks(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      SQProject var4 = ProjectEngine.get(var3);

      for (final Databank var7 : var4.getDatabanks().list()) {
         try {
            var7.loadStrategies(new IProgressListener() {
               public void onError(double var1, String var3x) {
                  CLILogger.log(String.format("Loading strategies to databank '%s' failed - %s", var7.getName(), var3x));
               }

               public void onDone() {
                  CLILogger.log(String.format("Loaded %d strategies to databank %s", var7.size(), var7.getName()));
               }

               public void onProgress(double var1) {
               }
            });
         } catch (Exception var9) {
            return apiErrorJSON(L.t("Cannot synchronize databank.", new Object[0]), var9);
         }
      }

      var2.put("success", L.t("Synchronization finished.", new Object[0]));
      return var2.toString();
   }

   private String onDatabankCount(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Databank var3 = this.getDatabank(var1);
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         CLILogger.log(String.format("Records: %d", var3.size()));
      } else {
         var2.put("count", var3.size());
         var2.put("success", L.t("Databank count retrieved.", new Object[0]));
      }

      return var2.toString();
   }

   private String onDatabankList(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      SQProject var4 = ProjectEngine.get(var3);
      JSONArray var5 = this.listDatabanks(var4);
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         for (int var6 = 0; var6 < var5.length(); var6++) {
            JSONObject var7 = var5.getJSONObject(var6);
            CLILogger.log(String.format("%s, Records: %s", var7.getString("name"), var7.getInt("records")));
         }
      } else {
         var2.put("databanks", var5);
         var2.put("success", L.t("Databanks listed.", new Object[0]));
      }

      return var2.toString();
   }

   private String onMoveDatabank(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      String var4 = this.tryGetParam(var1, "databankName")[0];
      boolean var5 = Boolean.parseBoolean(this.tryGetParam(var1, "moveLeft")[0]);
      SQProject var6 = ProjectEngine.get(var3);
      var6.getDatabanks().move(var4, var5);
      var2.put("databanks", this.listDatabanks(var6));
      var2.put("success", L.t("Databanks listed.", new Object[0]));
      return var2.toString();
   }

   private String onMoveDatabankToPosition(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      String var4 = this.tryGetParam(var1, "databankName")[0];
      int var5 = Integer.parseInt(this.tryGetParam(var1, "newPosition")[0]);
      SQProject var6 = ProjectEngine.get(var3);
      var6.getDatabanks().moveToPosition(var4, var5);
      var2.put("databanks", this.listDatabanks(var6));
      var2.put("success", L.t("Databanks listed.", new Object[0]));
      return var2.toString();
   }

   private JSONArray listDatabanks(SQProject var1) {
      JSONArray var2 = new JSONArray();

      for (Databank var4 : var1.getDatabanks().list()) {
         if (var4.getView() == null) {
            var4.setViewByName("Default - Main data");
         }

         JSONObject var5 = new JSONObject();
         var5.put("name", var4.getName());
         var5.put("view", var4.getView().getName());
         var5.put("records", var4.getRecords().size());
         var5.put("syncType", var4.getSyncType());
         var5.put("position", var4.getPosition());
         var2.put(var5);
      }

      return var2;
   }

   private String onListStrategies(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Databank var3 = this.getDatabank(var1);
      var3.setViewByName("Default - Main data");
      JSONArray var4 = new JSONArray();

      for (ResultsGroup var6 : var3.getRecords()) {
         var4.put(var6.getName());
      }

      var2.put("strategies", var4);
      var2.put("success", L.t("Strategies listed.", new Object[0]));
      return var2.toString();
   }

   private String onGetStrategyStats(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Databank var3 = this.getDatabank(var1);
      var3.setViewByName("Default - Main data");
      String var4 = this.tryGetParam(var1, "strategyName")[0];
      ArrayList var5 = var3.getBufferData(false);
      ArrayList var6 = var3.getRecords();

      for (int var7 = 0; var7 < var6.size(); var7++) {
         if (((ResultsGroup)var6.get(var7)).getName().equals(var4)) {
            JSONArray var8 = new JSONArray();

            for (DatabankTableColumnEntry var10 : var3.getView().columns) {
               var8.put(var10.getColumnEntryName());
            }

            JSONArray var14 = new JSONArray();
            if (var7 < var5.size()) {
               for (Object var13 : (Object[])var5.get(var7)) {
                  var14.put(var13 != null ? var13.toString() : "");
               }
            }

            var2.put("name", var4);
            var2.put("columns", var8);
            var2.put("values", var14);
            var2.put("success", L.t("Strategy stats loaded.", new Object[0]));
            return var2.toString();
         }
      }

      throw new Exception(L.t("Strategy '%s' not found.", new Object[]{var4}));
   }

   private String onCreateDatabank(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "databankName")[0];
         boolean var5 = false;

         try {
            var5 = Boolean.parseBoolean(this.tryGetParam(var1, "predefined")[0]);
         } catch (Exception var9) {
         }

         if (!var5) {
            switch (var4) {
               case "Results":
               case "Strategies to improve":
               case "Initial population":
               case "Strategies to optimize":
               case "Existing portfolio":
               case "Simple strategies":
                  throw new Exception(L.t("Name '%s' is reserved and cannot be used.", new Object[]{var4}));
            }
         }

         SQProject var11 = ProjectEngine.get(var3);
         if (var11.getDatabanks().get(var4) != null) {
            throw new Exception(L.t("Databank with name '%s' already exists.", new Object[]{var4}));
         }

         int var12;
         if (var1.containsKey("position")) {
            var12 = Integer.parseInt(this.tryGetParam(var1, "position")[0]);
         } else {
            var12 = var11.getDatabanks().size();
         }

         Databank var8 = var11.addDatabank(var4, var12);
         var8.setSyncType(Databank.getDefaultSyncType());
         var2.put("xmlConfig", XMLUtil.elementToString(var11.getConfig()));
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Cannot create databank.", new Object[0]), var10);
      }

      var2.put("success", L.t("Databank added.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveDatabank(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "databankName")[0];
         boolean var5 = false;

         try {
            var5 = Boolean.parseBoolean(this.tryGetParam(var1, "predefined")[0]);
         } catch (Exception var7) {
         }

         if (!var5 && Databank.isDefault(var4)) {
            throw new Exception(L.t("You can remove custom databanks only.", new Object[0]));
         }

         SQProject var6 = ProjectEngine.get(var3);
         var6.removeDatabank(var4);
         var2.put("xmlConfig", XMLUtil.elementToString(var6.getConfig()));
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Databank cannot be removed.", new Object[0]), var8);
      }

      var2.put("success", L.t("Databank removed.", new Object[0]));
      return var2.toString();
   }

   private String onRenameDatabank(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      String var4 = this.tryGetParam(var1, "databankName")[0];
      String var5 = this.tryGetParam(var1, "newName")[0];
      SQProject var6 = ProjectEngine.get(var3);
      if (var6.getDatabanks().get(var5) != null) {
         throw new Exception(L.t("Databank with name '%s' already exists.", new Object[]{var5}));
      }

      var6.getDatabanks().rename(var4, var5);
      Element var7 = var6.getConfig();
      ArrayList var8 = var6.getTasks();

      for (int var9 = 0; var9 < var8.size(); var9++) {
         ISQTask var10 = (ISQTask)var8.get(var9);
         Element var11 = var10.getConfig();
         if (var11 != null) {
            Element var12 = var11.getChild("Databanks");
            if (var12 != null) {
               List var13 = var12.getChildren("Databank");

               for (int var14 = 0; var14 < var13.size(); var14++) {
                  Element var15 = (Element)var13.get(var14);
                  String var16 = var15.getAttributeValue("value");
                  if (var16.equals(var4)) {
                     var15.setAttribute("value", var5);
                  }
               }

               if (var10.getType().equals("GoToTask")) {
                  ArrayList var25 = XMLUtil.getNestedElements(var11, "Condition");

                  for (int var26 = 0; var26 < var25.size(); var26++) {
                     Element var27 = (Element)var25.get(var26);
                     Element var17 = var27.getChild("Right-Side");
                     if (var17 != null && var17.getAttribute("databank") != null) {
                        String var18 = var17.getAttributeValue("databank");
                        if (var18.equals(var4)) {
                           var17.setAttribute("databank", var5);
                        }
                     }

                     Element var28 = var27.getChild("Left-Side");
                     if (var28 != null && var28.getAttribute("databank") != null) {
                        String var19 = var28.getAttributeValue("databank");
                        if (var19.equals(var4)) {
                           var28.setAttribute("databank", var5);
                        }
                     }
                  }
               }
            }
         }
      }

      Element var20 = var7.getChild("Databanks");
      List var21 = var20.getChildren("Databank");

      for (int var22 = 0; var22 < var21.size(); var22++) {
         Element var23 = (Element)var21.get(var22);
         String var24 = var23.getAttributeValue("name");
         if (var24.equals(var4)) {
            var23.setAttribute("name", var5);
         }
      }

      SQFileManager.updateWholeProjectConfig(var6);
      var2.put("projectConfig", new JSONObject(var6.toJSON()));
      var2.put("success", L.t("Databanks listed.", new Object[0]));
      return var2.toString();
   }

   private String onStart(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      String var3;
      try {
         var3 = this.tryGetParam(var1, "projectName")[0];
         SQProject var4 = ProjectEngine.get(var3);
         if (var4.getRunningStatus() == 1 || var4.getRunningStatus() == 2) {
            Log.error("Trying to start project that is already running!");
            throw new Exception(L.t("Project is already running.", new Object[0]));
         }

         if (var4.hasUnresolvedResources()) {
            Log.error("Trying to start project that has unresolved resources!");
            throw new Exception(L.t("Project has unresolved resources.", new Object[0]));
         }

         if (var1.containsKey("projectXML")) {
            String var5 = this.tryGetParam(var1, "projectXML")[0];
            var5 = var5.replace(",ChikouSpan=4", "");
            String var6 = null;

            try {
               var6 = this.tryGetParam(var1, "taskXMLFile")[0];
            } catch (Exception var11) {
            }

            String var7 = null;

            try {
               var7 = this.tryGetParam(var1, "taskXML")[0];
            } catch (Exception var10) {
            }

            this.updateProjectXMLBeforeStart(var4, var5, var6, var7);
         }

         var4.setRunSpecification(0);
         String var14 = var1.containsKey("action") ? ((String[])var1.get("action"))[0] : null;
         if (var14 != null) {
            String var15 = this.tryGetParamValue(var1, "taskName");
            if (var14.equals("runProjectFromHere")) {
               var4.setRunSpecification(50, var15);
            } else if (var14.equals("runThisTaskOnly")) {
               var4.setRunSpecification(100, var15);
            } else {
               if (!var14.equals("runFrom") && !var14.equals("runTask")) {
                  Log.error("Invalid run specification. Action: " + var14);
                  throw new Exception(L.t("Invalid run specification. Action: %s", new Object[]{var14}));
               }

               int var16;
               try {
                  var16 = Integer.valueOf(var15);
               } catch (Exception var9) {
                  throw new Exception(L.t("Task index must be a number.", new Object[0]));
               }

               if (var16 < 1 || var16 > var4.getTasksCount()) {
                  throw new Exception(L.t("Task index out of range.", new Object[0]));
               }

               if (var14.equals("runFrom")) {
                  var4.setRunSpecification(50, var4.getTask(var16 - 1).getName());
               } else {
                  var4.setRunSpecification(100, var4.getTask(var16 - 1).getName());
               }
            }
         }

         var4.initTasksFromConfig();
         var4.beforeStart();
         var4.start();
      } catch (Exception var12) {
         Log.error("Exception onStart (2)", var12);
         return apiErrorJSON(L.t("Cannot start project.", new Object[0]), var12);
      }

      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         var2.put(
            "success",
            L.t("Project is running on the background. To get project status use", new Object[0])
               + "\n"
               + String.format("-project action=status name=%s", var3)
         );
      } else {
         var2.put("success", L.t("Project execution started.", new Object[0]));
      }

      return var2.toString();
   }

   private void updateProjectXMLBeforeStart(SQProject var1, String var2, String var3, String var4) throws Exception {
      Element var5 = XMLUtil.stringToElement(var2);
      var1.updateConfig(var5);
      if (var3 != null) {
         for (int var6 = 0; var6 < var1.getTasks().size(); var6++) {
            ISQTask var7 = var1.getTask(var6);
            if (var7.getTaskXMLFileName().equals(var3)) {
               Element var8 = XMLUtil.stringToElement(var4);
               var7.setConfig(var8, false, true);
               ProjectResources.addResources(var7);
               SQFileManager.updateTaskConfig(var1.getProjectConfigFile(), var3, var7.getConfig());
               break;
            }
         }
      }

      ProjectResources.checkBuildTemplates(var1);
   }

   private String onResume(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         SQProject var4 = ProjectEngine.get(var3);
         var4.resume();
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot resume project.", new Object[0]), var5);
      }

      var2.put("success", L.t("Project execution resumed.", new Object[0]));
      return var2.toString();
   }

   private String onPause(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         SQProject var4 = ProjectEngine.get(var3);
         var4.pause();
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot pause project.", new Object[0]), var5);
      }

      var2.put("success", L.t("Project execution paused.", new Object[0]));
      return var2.toString();
   }

   private String onStop(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         SQProject var4 = ProjectEngine.get(var3);
         var4.stop();
      } catch (Exception var5) {
         Log.error("Exception onStop, running status: {}", var5);
         return apiErrorJSON(L.t("Cannot stop project.", new Object[0]), var5);
      }

      var2.put("success", L.t("Project execution stopped.", new Object[0]));
      return var2.toString();
   }

   private String onProjectChanged(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      SQProject var4 = ProjectEngine.get(var3);
      var4.log.resetLastData();
      var4.bestResults.resetLastData();
      var4.publisher.resetLastData();
      var2.put("success", "ok");
      return var2.toString();
   }

   private String onGetRankColumns(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();

      try {
         Databank var4 = this.getDatabank(var1);

         for (DatabankTableColumnEntry var6 : var4.getView().columns) {
            DatabankColumn var7 = var6.tableColumn;
            if (var7.valueType == 1 || var7.valueType == 2) {
               var3.put(var6.getColumnEntryName());
            }
         }

         var2.put("rankColumns", var3);
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot list rank columns.", new Object[0]), var8);
      }

      var2.put("success", L.t("Rank columns listed.", new Object[0]));
      return var2.toString();
   }

   private String onChangeMaxRecords(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "maxRecords")[0];
         boolean var5 = false;
         final SQProject var6 = ProjectEngine.get(var3);
         final Databank var7 = this.getDatabank(var1);

         try {
            boolean var8 = false;
            IDatabankFilter var9 = var7.getFilter();
            if (var9 != null && var9 instanceof DefaultDatabankFilter) {
               var8 = ((DefaultDatabankFilter)var9).getDismissTooSimilarStrategies();
            }

            DefaultDatabankFilter var10 = new DefaultDatabankFilter(Integer.parseInt(var4), var8);
            var7.applyFilter(var10, new IProgressListener() {
               public void onProgress(double var1) {
                  var6.getProgress().update("max-strategies-change", var1, null);
               }

               public void onError(double var1, String var3x) {
                  var6.getProgress().update("max-strategies-change", var1, var3x);
               }

               public void onDone() {
                  var7.removeFilter();
               }
            });
            var2.put("showProgress", var5);
         } catch (Exception var11) {
            if (var7 != null) {
               var7.removeFilter();
            }

            throw var11;
         }
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot change max strategies count.", new Object[0]), var12);
      }

      var2.put("success", L.t("Max strategies setting changed.", new Object[0]));
      return var2.toString();
   }

   private String onLoadFilesToDatabank(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParam(var1, "projectName")[0];
      Object var4 = null;
      if (var1.containsKey("folder")) {
         String var5 = this.tryGetParam(var1, "folder")[0];
         String var6 = this.getParam(var1, "strategies", "all");
         ArrayList var7 = new ArrayList();
         File var8 = new File(var5);
         if (!var8.isDirectory()) {
            throw new Exception(L.t("'%s' is not a directory.", new Object[]{var8.getAbsolutePath()}));
         }

         File[] var9 = var8.listFiles();
         if (var9 != null) {
            for (File var13 : var9) {
               if (var6.equals("all") || var6.contains(SQUtils.stripExtension(var13.getName()))) {
                  var7.add(var13.getAbsolutePath());
               }
            }
         }

         var4 = var7.toArray(new String[var7.size()]);
      } else {
         var4 = this.tryGetParam(var1, "filePaths[]");
      }

      final SQProject var15 = ProjectEngine.get(var3);
      final Databank var16 = this.getDatabank(var1);
      final boolean var17 = var1.containsKey("clear") ? Boolean.parseBoolean(this.tryGetParam(var1, "clear")[0]) : false;
      final String[] var18 = (String[])var4;
      (new Thread() {
         @Override
         public void run() {
            ProjectServlet.this.loadFiles(var16, var18, var15, var17, "load");
            ProjectServlet.Log.info("Strategies loaded");
         }
      }).start();
      var2.put("success", L.t("Loading strategies.", new Object[0]));
      return var2.toString();
   }

   private void loadFiles(Databank var1, String[] var2, SQProject var3, boolean var4, String var5) {
      Throwable var6 = null;
      var3.bestResults.disable();

      try {
         var1.disableSynchronization();
         var3.publisher.resetLastData("progress-channel");
         String var7 = null;
         boolean var8 = false;
         double var9 = 0.0;
         double var11 = 100.0 / var2.length;
         StrategyLoader.init();

         for (int var13 = 0; var13 < var2.length; var13++) {
            try {
               File var14 = new File(var2[var13]);
               if (!var14.isDirectory()) {
                  if (!SQProject.isMemoryProtectionUsed() && !var8) {
                     try {
                        MemoryUsageChecker.checkAvailableMemory();
                     } catch (SQMemoryProtectionError var22) {
                        var3.userConfirm
                           .awaitConfirmation(
                              L.t("Memory usage warning", new Object[0]),
                              L.t("SQ is running out of memory - current consumption reached 85%.", new Object[0])
                                 + "<br>"
                                 + L.t("If you'll continue, SQ might crash or freeze when memory runs out.", new Object[0])
                                 + "<br>"
                                 + L.t(
                                    "We recommend you to stop further strategies loading and eventually to increase available memory in performance settings.",
                                    new Object[0]
                                 )
                                 + "<br><br>"
                                 + L.t("Do you want to stop loading remaining strategies?", new Object[0])
                           );
                        if (var3.userConfirm.isConfirmed()) {
                           break;
                        }

                        var8 = true;
                     }
                  }

                  var7 = SQUtils.stripExtension(var14.getName());
                  ResultsGroup var15 = StrategyLoader.loadStrategy(var3, var7, var2[var13], var9);
                  var9 += var11;
                  if (var15 == null) {
                     break;
                  }

                  var1.add(var15, true);
                  var1.updateBestResults(var15);
               }
            } catch (Exception var23) {
               Log.error("Error while loading strategies. Exc.", var23);
               var6 = var23;
            } catch (Error var24) {
               Log.error("Error while loading strategies. Exc.", var24);
               var6 = var24;
               break;
            }
         }
      } catch (Exception var25) {
         Log.error("Error while loading strategies. Exc.", var25);
         var6 = var25;
      } finally {
         var1.enableSynchronization();
      }

      var3.bestResults.enable();
      if (!StrategyLoader.isCanceled()) {
         var3.getProgress().update(var5, 100.0, var6 == null ? null : "Some strategies were not loaded because of errors.<br>Last error: " + var6.getMessage());
      }
   }

   private String onInstrumentSelected(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      StrategyLoader.instrumentSelected();
      var2.put("success", "ok");
      return var2.toString();
   }

   private String onCancelRecordsLoading(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      StrategyLoader.cancel();
      var2.put("success", L.t("Records loading canceled.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveReports(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "strategies")[0];
         String[] var5 = var4.split(",");
         SQProject var6 = ProjectEngine.get(var3);
         Databank var7 = this.getDatabank(var1);
         if (var5.length == 1 && var5[0].equals("all")) {
            var5 = new String[var7.size()];
            var7.getRecordKeys().toArray(var5);
         }

         ProjectConfigHelper.delete(var6, var7, var5, "onRemoveReports");
         var2.put("records", var7.size());
         var2.put("success", L.t("Reports removed.", new Object[0]));
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Removing reports failed.", new Object[0]), var8);
      }

      var2.put("success", L.t("Reports removed.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveAllReports(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         SQProject var4 = ProjectEngine.get(var3);
         Databank var5 = this.getDatabank(var1);
         var5.removeAll(true, String.format("API request project/removeAllReports from databank %s/%s", var3, var5.getName()));
         String[] var6 = new String[var5.size()];
         var6 = var5.getRecordKeys().toArray(var6);
         if (var6.length == 0) {
            var5.addExtraDatabankChange("remove", "DatabankEmpty");
         } else {
            ProjectConfigHelper.delete(var4, var5, var6, "onRemoveAllReports");
         }
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Removing reports failed.", new Object[0]), var7);
      }

      var2.put("success", L.t("Reports removed.", new Object[0]));
      return var2.toString();
   }

   private String onClearAllDatabanks(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         SQProject var4 = ProjectEngine.get(var3);

         for (String var6 : var4.getDatabanks().keySet()) {
            Databank var7 = (Databank)var4.getDatabanks().get(var6);
            if (var7.getRecords().size() != 0) {
               var7.clearRecords(true, true, String.format("API request project/clearAllDatabanks from project %s/%s", var3, var6));
            }
         }
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Clearing databanks failed.", new Object[0]), var8);
      }

      var2.put("success", L.t("Databanks cleared.", new Object[0]));
      return var2.toString();
   }

   private String onCreatePortfolio(final Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      (new Thread() {
         @Override
         public void run() {
            ProjectServlet.this.createPortfolio(var1);
         }
      }).start();
      var2.put("success", L.t("Creating portfolio started.", new Object[0]));
      return var2.toString();
   }

   private String onMergeStrategies(final Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      (new Thread() {
         @Override
         public void run() {
            ProjectServlet.this.mergeStrategies(var1);
         }
      }).start();
      var2.put("success", L.t("Merging strategies started.", new Object[0]));
      return var2.toString();
   }

   private String onSplitStrategies(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      SQProject var3 = null;
      Databank var4 = null;

      try {
         String var5 = this.tryGetParam(var1, "projectName")[0];
         var3 = ProjectEngine.get(var5);
         String var6 = this.tryGetParam(var1, "strategies")[0];
         String[] var7 = var6.split(",");
         var4 = this.getDatabank(var1);
         if (var7.length == 1 && var7[0].equals("all")) {
            var7 = new String[var4.size()];
            var4.getRecordKeys().toArray(var7);
         }

         var3.publisher.resetLastData("progress-channel");
         var3.getProgress().update("Split strategies", -1.0, null, L.t("Splitting strategies", new Object[0]));

         for (String var11 : var7) {
            ResultsGroup var12 = var4.getLocked(var11, "onSplitStrategies");

            try {
               if (!var12.specialValues().containsKey(SpecialValues.IsMergedPortfolio)) {
                  throw new Exception(L.t("Strategy '' cannot be split - it is not a merged portfolio", new Object[0]));
               }

               List var13 = StrategyMerger.getSplitResults(var12);

               for (int var14 = 0; var14 < var13.size(); var14++) {
                  ResultsGroup var15 = (ResultsGroup)var13.get(var14);
                  var4.add(var15, false);
               }
            } finally {
               var12.releaseLock("onSplitStrategies");
            }
         }

         var4.refreshGrid();
         var3.getProgress().update("Split strategies", 100.0, null);
         WebSocketNotification.sendSuccess(L.t("Strategies split successfully", new Object[0]), new String[]{"SQUANT"});
      } catch (Exception var20) {
         if (var3 != null) {
            var3.getProgress().update("Split strategies", 100.0, L.t("Splitting strategies failed", new Object[0]) + " - " + var20.getMessage(), null);
         }

         if (var4 != null) {
            var4.refreshGrid();
         }

         return apiErrorJSON(L.t("Splitting reports failed.", new Object[0]), var20);
      }

      var2.put("success", L.t("Merging strategies started.", new Object[0]));
      return var2.toString();
   }

   private String onMoveToPM(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         final SQProject var4 = ProjectEngine.get(var3);
         final Databank var5 = this.getDatabank(var1);
         String var6 = this.tryGetParam(var1, "strategies")[0];
         final SQProject var7 = ProjectEngine.get("PortfolioMaster");
         final Databank var8 = (Databank)var7.getDatabanks().get("Simple strategies");
         if (var8 == null) {
            throw new Exception(String.format("Databank '%s' doesn't exist in project Portfolio Master.", "Simple strategies"));
         }

         boolean var9 = false;
         final String[] var10 = var6.split(",");
         if (var10.length == 1 && var10[0].equals("all")) {
            var9 = true;
         }

         final boolean var11 = var9;
         (new Thread() {
            @Override
            public void run() {
               try {
                  Exception var1x = null;
                  var7.bestResults.disable();
                  var4.bestResults.disable();
                  var7.publisher.resetLastData("progress-channel");
                  if (var11) {
                     ArrayList var40 = var5.getRecordKeys();
                     int var41 = var40.size();

                     for (int var42 = 0; var42 < var40.size(); var42++) {
                        try {
                           double var5x = (double)var42 / var41 * 100.0;
                           ResultsGroup var44 = var5.getLocked((String)var40.get(var42), "onMoveToPM");
                           ResultsGroup var8x = null;

                           try {
                              var8x = var44.clone();
                           } finally {
                              var44.releaseLock("onMoveToPM");
                           }

                           var8.add(var8x, false);
                           var7.getProgress().update("Move to Portfolio Master", var5x, null);
                        } catch (Exception var36) {
                           ProjectServlet.Log.error("Moving strategy to PortfolioMaster failed", var36);
                           var1x = var36;
                        }
                     }
                  } else {
                     int var2x = var10.length;

                     for (int var3x = 0; var3x < var10.length; var3x++) {
                        try {
                           double var4x = (double)var3x / var2x * 100.0;
                           ResultsGroup var6x = var5.getLocked(var10[var3x], "onMoveToPM");
                           ResultsGroup var7x = null;

                           try {
                              var7x = var6x.clone();
                           } finally {
                              var6x.releaseLock("onMoveToPM");
                           }

                           var5.remove(var7x.getName(), true, true, false, false, "onMoveToPM");
                           var8.add(var7x, false);
                           var7.getProgress().update("Move to Portfolio Master", var4x, null);
                        } catch (Exception var34) {
                           ProjectServlet.Log.error("Moving strategy to PortfolioMaster failed", var34);
                           var1x = var34;
                        }
                     }
                  }

                  var5.refreshGrid();
                  var8.refreshGrid();
                  var7.getProgress().update("Move to Portfolio Master", 100.0, var1x == null ? null : "Some strategies were not loaded because of errors.");
                  var5.updateBestResults();
                  var8.updateBestResults();
               } catch (Exception var37) {
                  ProjectServlet.Log.error("Moving strategies to PortfolioMaster failed", var37);
                  var7.getProgress().update("Move to Portfolio Master", 100.0, "Moving strategies failed. " + var37.getMessage());
               } catch (OutOfMemoryError var38) {
                  var7.getProgress().update("Move to Portfolio Master", 100.0, var38.getMessage());
                  var5.refreshGrid();
                  var5.updateBestResults();
                  var8.refreshGrid();
                  var8.updateBestResults();
               } finally {
                  var7.bestResults.enable();
                  var4.bestResults.enable();
               }
            }
         }).start();
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot load strategies to Portfolio Master.", new Object[0]), var12);
      }

      var2.put("success", L.t("Strategies moved.", new Object[0]));
      return var2.toString();
   }

   private String onMoveToPC(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         final SQProject var4 = ProjectEngine.get(var3);
         final Databank var5 = this.getDatabank(var1);
         String var6 = this.tryGetParam(var1, "strategies")[0];
         final SQProject var7 = ProjectEngine.get("PortfolioComposer");
         final Databank var8 = (Databank)var7.getDatabanks().get("Results");
         if (var8 == null) {
            throw new Exception(String.format("Databank '%s' doesn't exist in project PortfolioComposer.", "Results"));
         }

         boolean var9 = false;
         final String[] var10 = var6.split(",");
         if (var10.length == 1 && var10[0].equals("all")) {
            var9 = true;
         }

         final boolean var11 = var9;
         (new Thread() {
            @Override
            public void run() {
               try {
                  Exception var1x = null;
                  var7.bestResults.disable();
                  var4.bestResults.disable();
                  var7.publisher.resetLastData("progress-channel");
                  if (var11) {
                     ArrayList var40 = var5.getRecordKeys();
                     int var41 = var40.size();

                     for (int var42 = 0; var42 < var40.size(); var42++) {
                        try {
                           double var5x = (double)var42 / var41 * 100.0;
                           ResultsGroup var44 = var5.getLocked((String)var40.get(var42), "onMoveToPC");
                           ResultsGroup var8x = null;

                           try {
                              var8x = var44.clone();
                           } finally {
                              var44.releaseLock("onMoveToPC");
                           }

                           var8.add(var8x, false);
                           var7.getProgress().update("Move to Portfolio Composer", var5x, null);
                        } catch (Exception var36) {
                           ProjectServlet.Log.error("Moving strategy to PortfolioComposer failed", var36);
                           var1x = var36;
                        }
                     }
                  } else {
                     int var2x = var10.length;

                     for (int var3x = 0; var3x < var10.length; var3x++) {
                        try {
                           double var4x = (double)var3x / var2x * 100.0;
                           ResultsGroup var6x = var5.getLocked(var10[var3x], "onMoveToPC");
                           ResultsGroup var7x = null;

                           try {
                              var7x = var6x.clone();
                           } finally {
                              var6x.releaseLock("onMoveToPC");
                           }

                           var5.remove(var7x.getName(), true, true, false, false, "onMoveToPC");
                           var8.add(var7x, false);
                           var7.getProgress().update("Move to Portfolio Composer", var4x, null);
                        } catch (Exception var34) {
                           ProjectServlet.Log.error("Moving strategy to PortfolioComposer failed", var34);
                           var1x = var34;
                        }
                     }
                  }

                  var5.refreshGrid();
                  var8.refreshGrid();
                  var7.getProgress().update("Move to Portfolio Composer", 100.0, var1x == null ? null : "Some strategies were not loaded because of errors.");
                  var5.updateBestResults();
                  var8.updateBestResults();
               } catch (Exception var37) {
                  ProjectServlet.Log.error("Moving strategies to PortfolioComposer failed", var37);
                  var7.getProgress().update("Move to Portfolio Composer", 100.0, "Moving strategies failed. " + var37.getMessage());
               } catch (OutOfMemoryError var38) {
                  var7.getProgress().update("Move to Portfolio Composer", 100.0, var38.getMessage());
                  var5.refreshGrid();
                  var5.updateBestResults();
                  var8.refreshGrid();
                  var8.updateBestResults();
               } finally {
                  var7.bestResults.enable();
                  var4.bestResults.enable();
               }
            }
         }).start();
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot load strategies to PortfolioComposer.", new Object[0]), var12);
      }

      var2.put("success", L.t("Strategies moved.", new Object[0]));
      return var2.toString();
   }

   private void createPortfolio(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "projectName")[0];
         SQProject var3 = ProjectEngine.get(var2);
         String var4 = this.tryGetParam(var1, "strategies")[0];
         String[] var5 = var4.split(",");
         String var6 = this.tryGetParam(var1, "initialBalanceType")[0];
         Databank var7 = this.getDatabank(var1);
         if (var5.length == 1 && var5[0].equals("all")) {
            var5 = new String[var7.size()];
            var7.getRecordKeys().toArray(var5);
         }

         var3.publisher.resetLastData("progress-channel");
         ArrayList var8 = new ArrayList();

         try {
            for (String var12 : var5) {
               ResultsGroup var13 = var7.getLocked(var12, "onCreatePortfolio");
               var8.add(var13);
            }

            var3.getProgress().update("Create Portfolio", -1.0, null, L.t("Merging strategies", new Object[0]));
            ResultsGroup var21 = ResultsGroup.merge(var8, new String[]{"AdditionalMarket", "CrossCheck_"}, var3);
            var3.getProgress().update("Create Portfolio", -1.0, null, L.t("Creating portfolio result", new Object[0]));
            var21.createPortfolioResult(var6, var3);
            Element var22 = var21.portfolio().getStrategyXml();
            if (var22 != null) {
               var22.setAttribute("mergedResult", "true");
            }

            var7.add(var21, true);
            var7.updateBestResults(var21);
            WebSocketNotification.sendSuccess(L.t("Portfolio created '%s'", new Object[]{var21.getName()}), new String[]{"SQUANT"});
         } finally {
            for (ResultsGroup var16 : var8) {
               var16.releaseLock("onCreatePortfolio");
            }

            var3.getProgress().update("Create Portfolio", 100.0, null);
         }
      } catch (Exception var20) {
         Log.error("Creating portfolio failed.", var20);
      }
   }

   private void mergeStrategies(Map<String, String[]> var1) {
      SQProject var2 = null;

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         var2 = ProjectEngine.get(var3);
         String var4 = this.tryGetParam(var1, "strategies")[0];
         String[] var5 = var4.split(",");
         String var6 = this.tryGetParam(var1, "name")[0];
         String var7 = this.tryGetParam(var1, "databank")[0];
         Databank var8 = (Databank)var2.getDatabanks().get(var7);
         String var9 = this.tryGetParam(var1, "mergeType")[0];
         String var10 = this.tryGetParam(var1, "simulatedInitialCapital")[0];
         int var11 = Integer.parseInt(this.tryGetParam(var1, "signalsConditionsPct")[0]);
         String var12 = this.tryGetParam(var1, "signalsStrategy")[0];
         Databank var13 = this.getDatabank(var1);
         if (var5.length == 1 && var5[0].equals("all")) {
            var5 = new String[var13.size()];
            var13.getRecordKeys().toArray(var5);
         }

         var2.publisher.resetLastData("progress-channel");
         ArrayList var14 = new ArrayList();

         try {
            for (String var18 : var5) {
               ResultsGroup var19 = var13.getLocked(var18, "onMergeStrategies");
               var14.add(var19);
            }

            var2.getProgress().update("Merge strategies", -1.0, null, L.t("Merging strategies", new Object[0]));
            ResultsGroup var27 = null;
            switch (var9) {
               case "simulated":
                  var27 = StrategyMerger.createSimulatedResult(var2, var14, var6, var10);
                  break;
               case "merged":
                  var27 = StrategyMerger.createMergedResult(var2, var14, var6);
                  break;
               case "signals":
                  var27 = StrategyMerger.createFuzzyResult(var2, var14, var6, var11, var12);
            }

            var8.add(var27, true);
            var8.updateBestResults(var27);
            WebSocketNotification.sendSuccess(L.t("Portfolio created '%s'", new Object[]{var27.getName()}), new String[]{"SQUANT"});
         } finally {
            for (ResultsGroup var22 : var14) {
               var22.releaseLock("onMergeStrategies");
            }

            var2.getProgress().update("Merge strategies", 100.0, null);
         }
      } catch (Throwable var26) {
         Log.error("Creating portfolio failed.", var26);
         if (var2 != null) {
            var2.getProgress().update("Merge strategies", 100.0, L.t("Merging strategies failed", new Object[0]) + " - " + var26.getMessage(), null);
         }
      }
   }

   private String onGetDataItems(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      boolean var3 = false;

      try {
         var3 = Boolean.parseBoolean(this.tryGetParam(var1, "algoWizard")[0]);
      } catch (Exception var16) {
      }

      ResultsGroup var4 = null;
      String var5 = null;

      try {
         if (var3) {
            var4 = (ResultsGroup)Program.get("Loader").call("loadFile", new Object[]{SQPaths.algoWizardStrategyPath});
            DataItemsGenerator.get(var2, var4);
         } else {
            var5 = this.tryGetParam(var1, "reportName")[0];
            Databank var6 = this.getDatabank(var1);

            try {
               var4 = var6.getLocked(var5, "onGetDataItems");
               DataItemsGenerator.get(var2, var4);
            } catch (RecordNotFoundException var15) {
               JSONObject var8 = new JSONObject();
               var8.put("error", L.t("Strategy '%s' doesn't exist.", new Object[]{var5}));
               var8.put("strDoesntExist", true);
               return var8.toString();
            } finally {
               if (var4 != null) {
                  var4.releaseLock("onGetDataItems");
               }
            }
         }
      } catch (Exception var18) {
         return apiErrorJSON(L.t("Cannot get data items.", new Object[0]), var18);
      }

      var2.put("success", L.t("Data items listed.", new Object[0]));
      return var2.toString();
   }

   private String onSaveReports(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      SQProject var3 = null;

      try {
         String var4 = this.getParam(var1, "folder", SQPaths.strategiesDirPath);
         String var5 = this.getParam(var1, "strategies", "all");
         String var6 = this.getParam(var1, "fileName", "not set");
         String var7 = this.getParam(var1, "extension[name]", "sqx");
         String var8 = this.getParam(var1, "prefix", "");
         String var9 = this.getParam(var1, "sufix", "");
         boolean var10 = Boolean.parseBoolean(this.getParam(var1, "handleMagicNumbers", "false"));
         int var11 = Integer.parseInt(this.getParam(var1, "startingNumber", "12345"));
         String[] var12 = var5.split(",");
         Databank var13 = this.getDatabank(var1);
         var3 = ProjectEngine.get(var13.getProject());
         if (var12.length == 1 && var12[0].equals("all")) {
            var12 = new String[var13.size()];
            var13.getRecordKeys().toArray(var12);
         }

         var13.getReportSaver().resetCounting();
         if (!MainApp.runInConsoleWithoutActiveWebserver()) {
            for (int var14 = 0; var14 < var12.length; var14++) {
               String var15 = var12[var14];
               String var16 = var4 + "/" + (var12.length <= 1 && !var6.equals("not set") ? var6 : var8 + var15 + var9) + "." + var7;

               try {
                  if (var7.equals("java")) {
                     var16 = var4
                        + "/"
                        + (var12.length <= 1 && !var6.equals("not set") ? SQUtils.correctClassName(var6) : SQUtils.correctClassName(var8 + var15 + var9))
                        + "."
                        + var7;
                  }
               } catch (Exception var26) {
               }

               if (new File(var16).exists()) {
                  var3.userConfirm
                     .awaitConfirmation(
                        L.t("Overwrite confirm", new Object[0]),
                        L.t("There already are strategies with the same name in the target folder. Do you want to overwrite them?", new Object[0])
                     );
                  if (!var3.userConfirm.isConfirmed()) {
                     var3.getProgress().update("save", 100.0, null);
                     var2.put("success", L.t("Canceled.", new Object[0]));
                     return var2.toString();
                  }
                  break;
               }
            }
         }

         new File(var4).mkdirs();

         for (int var32 = 0; var32 < var12.length; var32++) {
            String var33 = var12[var32];
            String var34 = var4 + "/" + (var12.length <= 1 && !var6.equals("not set") ? var6 : var8 + var33 + var9) + "." + var7;

            try {
               if (var7.equals("java")) {
                  var34 = var4
                     + "/"
                     + (var12.length <= 1 && !var6.equals("not set") ? SQUtils.correctClassName(var6) : SQUtils.correctClassName(var8 + var33 + var9))
                     + "."
                     + var7;
               }
            } catch (Exception var27) {
            }

            ResultsGroup var17 = null;

            try {
               var17 = var13.getLocked(var33, "onSaveReports");

               try {
                  var13.getReportSaver().save(var17, var7, var34, var10, var11, var1);
               } catch (Exception var28) {
                  Log.error("Error while saving strategy '" + var33 + "'", var28);
                  continue;
               }
            } catch (Exception var29) {
               Log.warn("Strategy '" + var33 + "' no longer exists (was probably replaced by a better strategy with the same fingerprint)");
               continue;
            } finally {
               if (var17 != null) {
                  var17.releaseLock("onSaveReports");
               }
            }

            var3.getProgress().update("save", (double)var32 / var12.length * 100.0, null, L.t("Saved %d / %d", new Object[]{var32, var12.length}));
         }

         var3.getProgress().update("save", 100.0, null);
      } catch (Exception var31) {
         if (var3 != null) {
            var3.getProgress().update("save", 100.0, null);
         }

         return apiErrorJSON(L.t("Saving reports failed.", new Object[0]), var31);
      }

      var2.put("success", L.t("Reports saved.", new Object[0]));
      return var2.toString();
   }

   private String onSaveSingleFileJava(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = SQPaths.userDirPath;

         try {
            var3 = this.tryGetParam(var1, "folder")[0];
         } catch (Exception var19) {
         }

         String var4 = this.tryGetParam(var1, "filePath")[0];
         Object var5 = null;
         Object var6 = null;

         try {
            JSONArray var7 = this.tryGetJsonArray(var1, "extension");
            if (var7.length() == 0) {
               throw new Exception();
            }

            int var8 = var7.length();
            var5 = new String[var8];
            var6 = new String[var8];

            for (int var9 = 0; var9 < var8; var9++) {
               JSONObject var10 = var7.getJSONObject(var9);
               ((Object[])var5)[var9] = var10.getString("name");
               ((Object[])var6)[var9] = var10.getString("description");
            }
         } catch (Exception var20) {
            var5 = new String[]{this.tryGetParam(var1, "extension[name]")[0]};
            String[] var10000 = new String[]{this.tryGetParam(var1, "extension[description]")[0]};
         }

         String var25 = this.tryGetParam(var1, "fileName")[0];
         Databank var26 = this.getDatabank(var1);
         var26.getReportSaver().resetCounting();
         ResultsGroup var27 = var26.getLocked(var25, "onSaveSingleFileJava");
         int var28 = var4.lastIndexOf(46);
         String var11 = null;
         if (var28 != -1 && !this.os.isUnix()) {
            var11 = SQUtils.getExtension(var4);
         } else if (var5 != null && ((Object[])var5).length == 1) {
            var11 = (String)((Object[])var5)[0];
            var4 = var4 + "." + var11;
         }

         try {
            File var12 = new File(var4);
            var26.getReportSaver().save(var27, var11, var4, false, 0, var1);
            var2.put("folder", var12.getParent());
         } finally {
            var27.releaseLock("onSaveSingleFileJava");
         }
      } catch (Exception var21) {
         return apiErrorJSON(L.t("Saving reports failed.", new Object[0]), var21);
      }

      var2.put("success", L.t("File saved.", new Object[0]));
      return var2.toString();
   }

   private String onGetConfig(Map<String, String[]> var1) {
      JsonCreator var2 = new JsonCreator();
      var2.beginObject();

      try {
         String var3 = ((String[])var1.get("projectName"))[0];
         var2.putRaw("config", ProjectEngine.get(var3).toJSON(), true);
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot get project settings.", new Object[0]), var4);
      }

      var2.put("success", L.t("XML config returned.", new Object[0]), false);
      var2.endObject(false);
      return var2.toJson();
   }

   private String onCheckResources(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("projectName"))[0];
         SQProject var4 = ProjectEngine.get(var3);
         Element var5 = var4.checkUnresolvedResources();
         if (var5 != null) {
            var2.put("hasUnresolvedResources", true);
            var2.put("resourcesXML", XMLUtil.xmlToStringRaw(var5));
         }

         var2.put("config", var4.toJSON());
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot get project settings.", new Object[0]), var6);
      }

      var2.put("success", L.t("XML config returned.", new Object[0]));
      return var2.toString();
   }

   private String onGetTemplateConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "taskName")[0];
         String var4 = this.tryGetParam(var1, "taskType")[0];
         String var5 = this.tryGetParam(var1, "fileName")[0];
         String var6 = null;

         try {
            var6 = this.tryGetParam(var1, "sampleName")[0];
         } catch (Exception var13) {
         }

         boolean var7 = var6 != null;
         File var8 = this.getTemplateFile(var5, var7);
         String var9 = SQFileManager.loadFile(var8.getAbsolutePath());
         Element var10 = XMLUtil.stringToXmlElement(var9);
         if (var10.getName().equals("Blocks")) {
            var2.put("xmlConfig", var9);
         } else {
            Element var11 = this.prepareTaskConfig(var10, var4);
            var11.setAttribute("name", var3);
            var11.setAttribute("templateFile", var5);
            if (var7) {
               var11.setAttribute("sampleName", var6);
            }

            Element var12 = ProjectResources.checkTaskResources(var11);
            if (var12 != null) {
               var2.put("resourcesXML", XMLUtil.xmlToStringRaw(var12));
            }

            var2.put("xmlConfig", XMLUtil.xmlToStringRaw(var11));
         }
      } catch (Exception var14) {
         return apiErrorJSON(L.t("Cannot get task config settings.", new Object[0]), var14);
      }

      var2.put("success", L.t("XML config returned.", new Object[0]));
      return var2.toString();
   }

   private String onLoadConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "file")[0];
         if (!var3.endsWith(".cfx")) {
            var3 = var3 + ".cfx";
         }

         String var4 = SQFileManager.loadFile(var3);
         Element var5 = XMLUtil.stringToXmlElement(var4);
         if (var5.getName() != "Task") {
            throw new Exception("Invalid task config, missing Task element.");
         }

         Element var6 = var5.getChild("Settings");
         if (var6 == null) {
            throw new Exception("Invalid task config, missing Settings element.");
         }

         String var7 = this.tryGetParam(var1, "projectName")[0];
         SQProject var8 = ProjectEngine.get(var7);
         if (var8.getTasksCount() == 0) {
            throw new Exception("The project is empty.");
         }

         ISQTask var9 = var8.getTask(0);
         if (!XMLUtil.getAttr(var5, "type", "NA").equals(var9.getType())) {
            throw new Exception("Cannot apply config of different type.");
         }

         String var10 = var9.getTaskXMLFileName();
         Element var11 = var9.getConfig();
         var11.removeContent();

         for (Content var13 : var6.getContent()) {
            var11.addContent(var13.clone());
         }

         SQFileManager.updateTaskConfig(var8.getProjectConfigFile(), var10, var11);
      } catch (Exception var14) {
         return apiErrorJSON("Cannot load config. ", var14);
      }

      var2.put("success", "Config loaded.");
      return var2.toString();
   }

   private String onSaveConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "file")[0];
         if (!var3.endsWith(".cfx")) {
            var3 = var3 + ".cfx";
         }

         String var4 = this.tryGetParam(var1, "projectName")[0];
         SQProject var5 = ProjectEngine.get(var4);
         if (var5.getTasksCount() == 0) {
            throw new Exception("The project is empty.");
         }

         ISQTask var6 = var5.getTask(0);
         Element var7 = var5.getConfig().getChild("Tasks").getChild("Task").clone();
         Element var8 = var6.getConfig().clone();
         var7.addContent(var8);
         String var9 = XMLUtil.elementToString(var7);
         SQFileManager.saveFile(var3, var9);
      } catch (Exception var10) {
         return apiErrorJSON("Cannot save config. ", var10);
      }

      var2.put("success", "Config saved.");
      return var2.toString();
   }

   private Element prepareTaskConfig(Element var1, String var2) throws Exception {
      String[] var3 = ProjectConfigHelper.getTaskSettings(var2);
      String[] var4 = new String[var3.length + 1];

      for (int var5 = 0; var5 < var3.length; var5++) {
         var4[var5] = var3[var5];
      }

      var4[var4.length - 1] = "Resources";
      String var16 = null;
      if (!var1.getName().equals("Task")) {
         throw new Exception(L.t("Selected config is not a task template.", new Object[0]));
      }

      Element var6 = var1.clone().detach();
      var6.setAttribute("type", var2);
      var6.setAttribute("active", "true");
      Element var7 = XMLUtil.getChildElem(var6, "Settings");
      var7.removeContent();
      switch (var2) {
         case "Build":
            var16 = "Builder";
            break;
         case "Retest":
            var16 = "Retester";
            break;
         case "Optimize":
            var16 = "Optimizer";
      }

      Element var17 = XMLUtil.getChildElem(var1, "Settings");

      for (Element var10 : var17.getChildren()) {
         String var11 = var10.getName();
         boolean var12 = false;

         for (int var13 = 0; var13 < var4.length; var13++) {
            String var14 = var4[var13];
            if (var14 != null && var14.equals(var11)) {
               var4[var13] = null;
               var12 = true;
               break;
            }
         }

         if (var12) {
            var7.addContent(var10.clone().detach());
         }
      }

      if (var16 == null) {
         return var6;
      }

      DefaultProjectConfig var19 = ProjectEngine.get(var16).getDefaultConfig();
      Element var20 = var19.taskConfig;

      for (Element var22 : var20.getChildren()) {
         String var23 = var22.getName();
         if (var23.equals("Databanks")) {
            var7.addContent(var22.clone().detach());
         } else {
            for (int var24 = 0; var24 < var4.length; var24++) {
               String var15 = var4[var24];
               if (var15 != null && var23.equals(var15)) {
                  var7.addContent(var22.clone().detach());
               }
            }
         }
      }

      return var6;
   }

   private String onUpdateTemplateConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         String var4 = this.tryGetParam(var1, "xmlConfig")[0];
         Element var5 = XMLUtil.stringToElement(var4);
         if (!var5.getName().equals("Task")) {
            throw new Exception(L.t("Selected config is not a task config.", new Object[0]));
         }

         var3 = var5.getAttributeValue("templateFile");
         File var6 = this.getTemplateFile(var3, false);
         SQFileManager.saveFile(var6.getAbsolutePath(), var4, var5);
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot update template file '%s'.", new Object[]{var3}), var7);
      }

      var2.put("success", L.t("Template config updated.", new Object[0]));
      return var2.toString();
   }

   private String onApplySimpleTemplate(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "taskName")[0];
         String var5 = this.tryGetParam(var1, "strategyType")[0];
         SQProject var6 = ProjectEngine.get(var3);
         ISQTask var7 = var6.getTaskByName(var4);
         Element var8 = var7.getConfig();
         File var9 = new File(SQPaths.simpleTemplatesDirPath + "/" + var5 + ".xml");
         if (!var9.exists()) {
            throw new Exception(L.t("Template file '%s' not found.", new Object[]{var9.getName()}));
         }

         Element var10 = XMLUtil.fileToXmlElement(var9);
         Element var11 = var10.getChild("Settings");
         var8.removeContent();

         for (Content var13 : var11.getContent()) {
            var8.addContent(var13.clone());
         }

         SQFileManager.updateWholeProjectConfig(var6);
         var2.put("xmlConfig", XMLUtil.xmlToStringRaw(var6.getConfig()));
         var2.put("taskXMLFile", var7.getTaskXMLFileName());
         var2.put("taskXML", XMLUtil.xmlToStringRaw(var8));
      } catch (Exception var14) {
         return apiErrorJSON(L.t("Cannot apply template config.", new Object[0]), var14);
      }

      var2.put("success", L.t("Template config applied.", new Object[0]));
      return var2.toString();
   }

   private String onGetCurrentTemplateSettings(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "taskName")[0];
         SQProject var5 = ProjectEngine.get(var3);
         Element var6 = var5.getTaskSettingsByName(var4);
         String var7 = var6.getAttributeValue("sampleName");
         if (var7 != null && !var7.isEmpty()) {
            Element var8 = TaskConfigWorker.getTemplateSettings(var7);
            var2.put("templateConfig", XMLUtil.xmlToStringRaw(var8));
         } else {
            Log.error("No template set in XML");
         }
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot apply template config.", new Object[0]), var9);
      }

      var2.put("success", L.t("Template config applied.", new Object[0]));
      return var2.toString();
   }

   private String onResetTaskConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "taskName")[0];
         String var5 = this.tryGetParam(var1, "configName")[0];
         SQProject var6 = ProjectEngine.get(var3);
         ISQTask var7 = var6.getTaskByName(var4);
         Element var8 = var7.getDefaultConfig(var5).getChild("Settings");
         Element var9 = var7.getConfig();
         var9.removeContent();
         List var10 = var8.getChildren();

         for (int var11 = 0; var11 < var10.size(); var11++) {
            var9.addContent(((Element)var10.get(var11)).clone());
         }

         Element var13 = ProjectConfigHelper.getTaskConfigByName(var6.getConfig(), var4);
         var13.setAttribute("sampleName", "Custom");
         var13.setAttribute("templateFile", "");
         SQFileManager.updateWholeProjectConfig(var6);
         var2.put("xmlConfig", XMLUtil.xmlToStringRaw(var6.getConfig()));
         var2.put("taskXMLFile", var7.getTaskXMLFileName());
         var2.put("taskXML", XMLUtil.xmlToStringRaw(var9));
         var2.put("simpleSettings", "Custom");
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot apply template config.", new Object[0]), var12);
      }

      var2.put("success", L.t("Template config applied.", new Object[0]));
      return var2.toString();
   }

   private File getTemplateFile(String var1, boolean var2) throws Exception {
      File var3 = new File(var1);
      if (!var3.exists()) {
         if (!var1.endsWith(".cfx")) {
            var1 = var1 + ".cfx";
         }

         var3 = new File(var1);
      }

      if (!var3.exists()) {
         String var4 = (var2 ? SQPaths.buildTemplatesDirPath : SQPaths.configsDirPath) + "/" + var1;
         var3 = new File(var4);
         if (!var3.exists()) {
            throw new Exception(String.format("Config file '%s' not found", var4));
         }
      }

      return var3;
   }

   private synchronized String onUpdateTaskXML(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;
      String var4 = null;

      try {
         var3 = this.tryGetParam(var1, "projectName")[0];
         var4 = this.tryGetParam(var1, "taskXMLFile")[0];
         String var5 = this.tryGetParam(var1, "xmlConfig")[0];
         Element var6 = XMLUtil.stringToElement(var5);
         SQProject var7 = ProjectEngine.get(var3);
         var7.checkProjectStatus("update");
         ISQTask var8 = var7.getTaskByXMLFile(var4);
         boolean var9 = false;
         if (var8.getType().equals("Build")) {
            Element var10 = ProjectConfigHelper.getTaskConfigByXMLFile(var7.getConfig(), var4);
            String var11 = var10.getAttributeValue("sampleName");
            boolean var12 = TaskConfigWorker.isCustomSettings(var11, var6);
            JSONObject var13 = new JSONObject();
            var13.put("strategyType", var12 ? "Custom" : var11);
            var2.put("simpleSettings", var13);
            String var14 = var12 ? "Custom" : var11;
            if (var14 != null && !var14.equals(var11)) {
               var10.setAttribute("sampleName", var14);
               var9 = true;
            }
         }

         Element var16 = var8.getConfig();
         var16.removeContent();

         for (Content var18 : var6.getContent()) {
            var16.addContent(var18.clone());
         }

         if (var9) {
            SQFileManager.updateWholeProjectConfig(var7);
         } else {
            ProjectResources.addResources(var8);
            SQFileManager.updateTaskConfig(var7.getProjectConfigFile(), var4, var8.getConfig());
         }
      } catch (Exception var15) {
         return apiErrorJSON(L.t("Cannot update settings of task file '%s', project '%s'.", new Object[]{var4, var3}), var15);
      }

      var2.put("success", L.t("Task XML config updated.", new Object[0]));
      return var2.toString();
   }

   private synchronized String onUpdateProjectXML(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "xmlConfig")[0];
         Element var5 = XMLUtil.stringToElement(var4);
         SQProject var6 = ProjectEngine.get(var3);
         var6.updateConfig(var5);
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot update settings of project '%s'.", new Object[]{var3}), var7);
      }

      var2.put("success", L.t("Project XML config updated.", new Object[0]));
      return var2.toString();
   }

   private Databank getDatabank(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "projectName")[0];
      String var3 = this.tryGetParam(var1, "databankName")[0];
      SQProject var4 = ProjectEngine.get(var2);
      if (!var4.getDatabanks().containsKey(var3)) {
         throw new Exception(L.t("Databank '%s' doesn't exist.", new Object[]{var3}));
      } else {
         return (Databank)var4.getDatabanks().get(var3);
      }
   }

   private String onRetest(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("strategies"))[0];
         String var4 = ((String[])var1.get("srcProjectName"))[0];
         String var5 = ((String[])var1.get("srcDatabankName"))[0];
         final boolean var6 = Boolean.parseBoolean(((String[])var1.get("move"))[0]);
         MainApp.settings().set("retestMove", var6 + "");
         boolean var7 = false;
         String var8 = null;

         try {
            var8 = ((String[])var1.get("srcTaskName"))[0];
         } catch (Exception var16) {
         }

         final SQProject var9 = ProjectEngine.get("Retester");
         final Databank var10 = (Databank)var9.getDatabanks().get("Results");
         final SQProject var11 = ProjectEngine.get(var4);
         final Databank var12 = (Databank)var11.getDatabanks().get(var5);
         final String[] var13 = var3.split(",");
         if (var13.length == 1 && var13[0].equals("all")) {
            var7 = true;
         }

         if (var8 != null) {
            try {
               Element var14 = ProjectEngine.get(var4).getTaskSettingsByName(var8);
               if (var14 == null) {
                  throw new Exception(L.t("Task settings not found.", new Object[0]));
               }

               var2.put("newConfig", XMLUtil.xmlToStringRaw(var14));
            } catch (Exception var15) {
               throw new Exception(L.t("Cannot get config of task '%s'.", new Object[]{var8}));
            }
         }

         final boolean var18 = var7;
         (new Thread() {
            @Override
            public void run() {
               try {
                  Exception var1x = null;
                  var9.bestResults.disable();
                  var11.bestResults.disable();
                  var9.publisher.resetLastData("progress-channel");
                  if (var18) {
                     ArrayList var40 = var12.getRecordKeys();
                     int var41 = var40.size();

                     for (int var42 = 0; var42 < var40.size(); var42++) {
                        try {
                           double var5x = (double)var42 / var41 * 100.0;
                           ResultsGroup var44 = var12.getLocked((String)var40.get(var42), "onRetest");
                           ResultsGroup var8x = null;

                           try {
                              var8x = var44.clone();
                           } finally {
                              var44.releaseLock("onRetest");
                           }

                           if (var6) {
                              var12.remove(var8x.getName(), true, true, false, false, "onRetest");
                           }

                           var10.add(var8x, false);
                           var9.getProgress().update("retest", var5x, null);
                        } catch (Exception var36) {
                           ProjectServlet.Log.error("Moving strategy to retester failed", var36);
                           var1x = var36;
                        }
                     }
                  } else {
                     int var2x = var13.length;

                     for (int var3x = 0; var3x < var13.length; var3x++) {
                        try {
                           double var4x = (double)var3x / var2x * 100.0;
                           ResultsGroup var6x = var12.getLocked(var13[var3x], "onRetest");
                           ResultsGroup var7x = null;

                           try {
                              var7x = var6x.clone();
                           } finally {
                              var6x.releaseLock("onRetest");
                           }

                           if (var6) {
                              var12.remove(var7x.getName(), true, true, false, false, "onRetest");
                           }

                           var10.add(var7x, false);
                           var9.getProgress().update("retest", var4x, null);
                        } catch (Exception var34) {
                           ProjectServlet.Log.error("Moving strategy to retester failed", var34);
                           var1x = var34;
                        }
                     }
                  }

                  var12.refreshGrid();
                  var10.refreshGrid();
                  var9.getProgress().update("retest", 100.0, var1x == null ? null : "Some strategies were not loaded because of errors.");
                  var12.updateBestResults();
                  var10.updateBestResults();
               } catch (Exception var37) {
                  ProjectServlet.Log.error("Moving strategies to retester failed", var37);
                  var9.getProgress().update("retest", 100.0, "Moving strategies failed. " + var37.getMessage());
               } catch (OutOfMemoryError var38) {
                  var9.getProgress().update("retest", 100.0, var38.getMessage());
                  var12.refreshGrid();
                  var12.updateBestResults();
                  var10.refreshGrid();
                  var10.updateBestResults();
               } finally {
                  var9.bestResults.enable();
                  var11.bestResults.enable();
               }
            }
         }).start();
      } catch (Exception var17) {
         return apiErrorJSON(L.t("Cannot load strategies to retest.", new Object[0]), var17);
      }

      var2.put("success", L.t("Strategies loaded.", new Object[0]));
      return var2.toString();
   }

   private String onConfirm(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("projectName"))[0];
         boolean var4 = Boolean.parseBoolean(((String[])var1.get("confirmed"))[0]);
         ProjectEngine.get(var3).confirm(var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Confirmation failed.", new Object[0]), var5);
      }

      var2.put("success", L.t("Action confirmed.", new Object[0]));
      return var2.toString();
   }

   private String onLoadGridData(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      boolean var3 = false;

      try {
         var3 = Boolean.parseBoolean(((String[])var1.get("refreshAction"))[0]);
      } catch (Exception var20) {
      }

      try {
         String var4 = ((String[])var1.get("projectName"))[0];
         String var5 = ((String[])var1.get("databankName"))[0];
         if (var3) {
            Log.info("Refresh called on databank " + var4 + "/" + var5 + "...");
         }

         boolean var6 = false;

         try {
            var6 = Boolean.parseBoolean(((String[])var1.get("first1000"))[0]);
         } catch (Exception var19) {
         }

         long var7 = Long.parseLong(((String[])var1.get("lastChangeTime"))[0]);
         Databank var9 = (Databank)ProjectEngine.get(var4).getDatabanks().get(var5);
         if (var9 == null) {
            throw new Exception(L.t("Databank '%s' doesn't exist.", new Object[]{var5}));
         }

         if (var3) {
            Log.info("Total strategies in databank: " + var9.size());
         }

         JSONArray var10 = new JSONArray();
         var2.put("loadTime", System.currentTimeMillis());
         if (var9.getLastChangeTime() <= var7) {
            var2.put("nothingChanged", true);
         } else {
            ArrayList var11 = var9.getBufferData(var6);

            for (int var12 = 0; var12 < var11.size(); var12++) {
               Object[] var13 = (Object[])var11.get(var12);
               JSONArray var14 = new JSONArray();

               for (Object var18 : var13) {
                  var14.put(var18);
               }

               var10.put(var14);
            }

            var2.put("gridData", var10);
         }

         var2.put("totalCount", var9.size());
      } catch (Exception var21) {
         return apiErrorJSON(L.t("Loading strategies failed.", new Object[0]), var21);
      }

      if (var3) {
         Log.info("Sending strategies to frontend...");
      }

      var2.put("success", L.t("Strategies loaded.", new Object[0]));
      return var2.toString();
   }

   private String onSetDatabankColumnValue(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("value"))[0];
         String var4 = ((String[])var1.get("column"))[0];
         String var5 = ((String[])var1.get("strategyName"))[0];
         Databank var6 = this.getDatabank(var1);
         ResultsGroup var7 = var6.getLocked(var5, "onSetDatabankColumnValue");

         try {
            DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var4);
            var8.setValue(var7, var3);
         } finally {
            if (var7 != null) {
               var7.releaseLock("onSetDatabankColumnValue");
            }
         }
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Cannot change databank column value.", new Object[0]), var13);
      }

      var2.put("success", L.t("Databank column value changed.", new Object[0]));
      return var2.toString();
   }

   private String onSetDatabankSynchronization(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "syncType")[0];
         Databank var4 = this.getDatabank(var1);
         var4.setSyncType(var3);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot change synchronization settings.", new Object[0]), var5);
      }

      var2.put("success", L.t("Synchronization settings changed.", new Object[0]));
      return var2.toString();
   }

   private String onSyncfromfiles(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         final Databank var3 = this.getDatabank(var1);
         var3.loadStrategies(new IProgressListener() {
            public void onError(double var1, String var3x) {
               CLILogger.log(L.t("Loading strategies to databank '%s' failed - %s", new Object[]{var3.getName(), var3x}));
            }

            public void onDone() {
               CLILogger.log(L.t("Loaded %d strategies to databank %s", new Object[]{var3.size(), var3.getName()}));
            }

            public void onProgress(double var1) {
            }
         });
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot synchronize databank.", new Object[0]), var4);
      }

      return var2.toString();
   }

   private String onSynchronizeDatabank(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         Databank var3 = this.getDatabank(var1);
         var3.synchronizeNow(String.format("API request project/synchronizeDatabank %s/%s.", var3.getProject(), var3.getName()));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot synchronize databank.", new Object[0]), var4);
      }

      var2.put("success", L.t("Synchronization started.", new Object[0]));
      return var2.toString();
   }

   private String onPrintToLog(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "message")[0];
         boolean var4 = Boolean.parseBoolean(((String[])var1.get("isError"))[0]);
         if (var4) {
            Log.error("UI error: " + var3);
         } else {
            Log.info("UI message: " + var3);
         }
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot log message.", new Object[0]), var5);
      }

      var2.put("success", L.t("Message logged.", new Object[0]));
      return var2.toString();
   }

   private String onCutStrategies(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         Databank var3 = this.getDatabank(var1);
         String[] var4 = this.getParam(var1, "strategies", "all").split(",");
         if (var4[0].equals("all")) {
            ArrayList var5 = var3.getRecordKeys();
            var4 = var5.toArray(new String[var5.size()]);
         }

         SQProject var7 = ProjectEngine.get(var3.getProject());
         if (!var7.getName().equals("Builder") && var7.isRunning()) {
            throw new Exception(L.t("Project is running.", new Object[0]));
         }

         StrategiesActionCache.cutFrom(var3, var4);
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot cut strategies.", new Object[0]), var6);
      }

      var2.put("success", L.t("Strategies cut.", new Object[0]));
      return var2.toString();
   }

   private String onCopyStrategies(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         Databank var3 = this.getDatabank(var1);
         String[] var4 = this.getParam(var1, "strategies", "all").split(",");
         if (var4[0].equals("all")) {
            ArrayList var5 = var3.getRecordKeys();
            var4 = var5.toArray(new String[var5.size()]);
         }

         StrategiesActionCache.copyFrom(var3, var4);
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot copy strategies.", new Object[0]), var6);
      }

      var2.put("success", L.t("Strategies copied.", new Object[0]));
      return var2.toString();
   }

   private String onPasteStrategies(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         Databank var3 = this.getDatabank(var1);
         SQProject var4 = ProjectEngine.get(var3.getProject());
         if (!var4.getName().equals("Builder") && var4.isRunning()) {
            throw new Exception(L.t("Project is running.", new Object[0]));
         }

         StrategiesActionCache.pasteTo(var3);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot paste strategies.", new Object[0]), var5);
      }

      var2.put("success", L.t("Strategies pasted.", new Object[0]));
      return var2.toString();
   }

   private String onResolveResources(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"configXML", "resourcesXML", "actionType"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "configXML");
      String var4 = this.tryGetParamValue(var1, "resourcesXML");
      String var5 = null;

      try {
         var5 = this.tryGetParamValue(var1, "filePath");
      } catch (Exception var15) {
      }

      try {
         Element var6 = XMLUtil.stringToElement(var3);
         Element var7 = XMLUtil.stringToElement(var4);
         JSONArray var8 = new JSONArray();
         HashMap var9 = null;
         if (var5 != null) {
            if (SQProject.projectIsOlderThan136(var6)) {
               var9 = SQFileManager.convertOldProjectConfig(var6);
            } else {
               var9 = SQFileManager.loadTaskConfigs(new File(var5));
            }
         }

         boolean var10 = ProjectResources.resolveResources(var7, var6, var9, var8, null);
         if (var9 != null) {
            JSONObject var11 = new JSONObject();

            for (String var13 : var9.keySet()) {
               Element var14 = (Element)var9.get(var13);
               if (var10) {
                  ProjectResources.addResources(var14);
               }

               var11.put(var13, XMLUtil.elementToString(var14));
            }

            var2.put("taskConfigs", var11);
         }

         var2.put("continueLoading", var10);
         var2.put("newSymbols", var8);
         var2.put("configXML", XMLUtil.elementToString(var6));
         var2.put("success", L.t("Resources resolved.", new Object[0]));
      } catch (Exception var16) {
         return apiErrorJSON(L.t("Cannot resolve resources.", new Object[0]), var16);
      }

      return var2.toString();
   }

   private String onCancelResolving() {
      JSONObject var1 = new JSONObject();
      ProjectResources.cancelImport();
      var1.put("success", L.t("Project import canceled.", new Object[0]));
      return var1.toString();
   }

   private String onCheckCustomResources(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"configXML", "actionType"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "configXML");
      ArrayList var4 = new ArrayList();

      try {
         JSONObject var5 = this.tryGetJsonObject(var1, "taskConfigs");
         Iterator var6 = var5.keys();

         while (var6.hasNext()) {
            String var7 = (String)var6.next();
            var4.add(XMLUtil.stringToElement(var5.getString(var7)));
         }
      } catch (Exception var10) {
      }

      int var11 = Integer.parseInt(this.tryGetParamValue(var1, "actionType"));

      try {
         Element var12 = XMLUtil.stringToElement(var3);
         Element var13 = null;
         if (var11 != 3 && var11 != 4) {
            var4.add(var12);
            var13 = ProjectCustomResources.checkProjectResources(var4);
         } else {
            Element var8 = var12.getChild("Strategy");
            var13 = ProjectCustomResources.checkStrategyResources(var8);
         }

         var2.put("success", L.t("Custom resources checked.", new Object[0]));
         var2.put("resourcesXML", XMLUtil.elementToString(var13));
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot resolve custom resources.", new Object[0]), var9);
      }

      return var2.toString();
   }

   private String onCheckTemplateResources(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"filePath"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "filePath");

      try {
         IProgram var4 = Program.get("Loader");
         var3 = var3.replace("\\", "/");
         var3 = var3.contains("/") ? var3 : SQPaths.strategyTemplatesDirPath + "/" + var3;
         ResultsGroup var5 = (ResultsGroup)var4.call("loadFile", new Object[]{var3});
         Element var6 = var5.getStrategyXml();
         Element var7 = var6.getChild("Strategy");
         Element var8 = ProjectCustomResources.checkStrategyResources(var7);
         var2.put("success", L.t("Custom resources checked.", new Object[0]));
         var2.put("configXML", XMLUtil.elementToString(var6));
         var2.put("resourcesXML", XMLUtil.elementToString(var8));
      } catch (Exception var9) {
         return apiErrorJSON(L.t("Cannot resolve custom resources.", new Object[0]), var9);
      }

      return var2.toString();
   }

   private String onResolveCustomResources(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"configXML", "resourcesXML", "actionType"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "configXML");
      String var4 = this.tryGetParamValue(var1, "resourcesXML");
      int var5 = Integer.parseInt(this.tryGetParamValue(var1, "actionType"));
      String var6 = null;

      try {
         var6 = this.tryGetParamValue(var1, "filePath");
      } catch (Exception var24) {
      }

      try {
         Element var7 = XMLUtil.stringToElement(var4);
         Element var8 = ProjectCustomResources.resolveStrategyResources(var7, XMLUtil.stringToElement(var3));
         switch (var5) {
            case 0:
               HashMap var29 = new HashMap();

               try {
                  if (SQProject.projectIsOlderThan136(var8)) {
                     var29 = SQFileManager.convertOldProjectConfig(var8);
                  } else {
                     JSONObject var30 = this.tryGetJsonObject(var1, "taskConfigs");
                     Iterator var32 = var30.keys();

                     while (var32.hasNext()) {
                        String var34 = (String)var32.next();
                        var29.put(var34, XMLUtil.stringToElement(var30.getString(var34)));
                     }
                  }
               } catch (Exception var26) {
                  Log.error("Error while preparing task configs", var26);
                  return apiErrorJSON(L.t("Error while preparing task configs.", new Object[0]), var26);
               }

               var8.setAttribute("version", MainApp.getAppVersion());
               String var31 = var8.getAttributeValue("name");
               if (var31 == null) {
                  return apiErrorJSON(L.t("Corrupted Project config file. Missing project name.", new Object[0]), null);
               }

               if (var6 == null) {
                  return apiErrorJSON(L.t("Project file path not specified.", new Object[0]), null);
               }

               SQProject var33 = ProjectEngine.add(var31, new File(var6), var8, var29);
               var2.put("loaded", true);
               var2.put("projectXML", XMLUtil.elementToString(var8));
               var2.put("success", String.format("Project loaded '%s'.", var33.getName()));
               break;
            case 1:
            case 2:
               var2.put("taskXML", XMLUtil.elementToString(var8));
               var2.put("success", L.t("Custom resources resolved.", new Object[0]));
               break;
            case 3:
               JarOutputStream var12 = null;

               try {
                  Manifest var35 = new Manifest();
                  var35.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
                  var12 = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(var6)), var35);
                  JarEntry var36 = new JarEntry("strategy_Portfolio.xml");
                  var12.putNextEntry(var36);
                  Document var37 = new Document(var8.clone());
                  BufferedWriter var16 = new BufferedWriter(new OutputStreamWriter(var12, "UTF8"));
                  XMLOutputter var17 = new XMLOutputter(Format.getPrettyFormat());
                  var17.output(var37, var16);
                  var2.put("success", L.t("Custom resources resolved.", new Object[0]));
                  break;
               } finally {
                  if (var12 != null) {
                     var12.close();
                  }
               }
            case 4:
               var2.put("success", L.t("Custom resources resolved.", new Object[0]));
               break;
            case 5:
               HashMap var9 = new HashMap();

               try {
                  JSONObject var13 = this.tryGetJsonObject(var1, "taskConfigs");
                  Iterator var14 = var13.keys();

                  while (var14.hasNext()) {
                     String var15 = (String)var14.next();
                     var9.put(var15, XMLUtil.stringToElement(var13.getString(var15)));
                  }
               } catch (Exception var27) {
                  Log.error("Error while preparing task configs", var27);
                  return apiErrorJSON(L.t("Error while preparing task configs.", new Object[0]), var27);
               }

               String var10 = var8.getAttributeValue("name");
               if (var10 == null) {
                  return apiErrorJSON(L.t("Missing project name.", new Object[0]), null);
               }

               SQProject var11 = ProjectEngine.get(var10);
               SQFileManager.updateTaskConfigurations(var11, var9);
               var2.put("projectXML", XMLUtil.elementToString(var8));
               var11.setUnresolvedResources(false);
               var2.put("success", L.t("Project resources resolved.", new Object[0]));
               break;
            default:
               return apiErrorJSON(L.t("Cannot resolve resources - unknown action type %s.", new Object[]{var5}), null);
         }
      } catch (Exception var28) {
         return apiErrorJSON(L.t("Cannot resolve custom resources.", new Object[0]), var28);
      }

      return var2.toString();
   }

   private String onStatus(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         SQProject var4 = ProjectEngine.get(var3);
         JSONObject var5 = var4.getInfo();
         ProjectRunInfo var6 = var4.getTrackingInfo();
         String var7 = SQUtils.d2(100.0 * var6.strategiesAcceptedPct);
         String var8 = SQUtils.d2(100.0 * var6.strategiesRejectedPct);
         if (var6.strategiesAccepted > 0L && var7.equals("0.00")) {
            var7 = "0.01";
            var8 = "99.99";
         }

         if (var3.equals("Builder")) {
            CLILogger.log(L.t("Strategies generated", new Object[0]), var6.totalJobsDone + "", 50);
            CLILogger.log(L.t("Time per strategy", new Object[0]), SQUtils.formatDuration(var6.timePerStrategy), 50);
            CLILogger.log(L.t("Time per accepted strategy", new Object[0]), SQUtils.formatDuration(var6.timePerAcceptedStrategy), 50);
            CLILogger.log(L.t("Rejected", new Object[0]), var8 + " %", 50);
            CLILogger.log(L.t("Accepted", new Object[0]), var7 + " %", 50);
            CLILogger.log(L.t("Strategies per hour", new Object[0]), this.formatIntOrDouble(var6.strategiesPerHour), 50);
            CLILogger.log(L.t("Accepted strategies per hour", new Object[0]), this.formatIntOrDouble(var6.acceptedStrategiesPerHour), 50);
            CLILogger.log(L.t("Running time so far", new Object[0]), SQUtils.formatDuration(var6.taskRunningTime), 50);
            CLILogger.log(L.t("In databank", new Object[0]), var5.getInt("strategies") + "", 50);
         } else if (var3.equals("Retester")) {
            CLILogger.log(L.t("Total tested", new Object[0]), var6.totalJobsDone + "", 50);
            CLILogger.log(L.t("Time per strategy", new Object[0]), SQUtils.formatDuration(var6.timePerStrategy), 50);
            CLILogger.log(L.t("Strategies per hour", new Object[0]), this.formatIntOrDouble(var6.strategiesPerHour), 50);
            CLILogger.log(L.t("In databank", new Object[0]), var5.getInt("strategies") + "", 50);
            CLILogger.log(L.t("Failed", new Object[0]), var6.strategiesFailed + "", 50);
            CLILogger.log(L.t("Passed", new Object[0]), var6.strategiesPassed + "", 50);
            CLILogger.log(L.t("Running time so far", new Object[0]), SQUtils.formatDuration(var6.totalProjectRunningTime), 50);
            CLILogger.log(L.t("Estimated time to finish", new Object[0]), SQUtils.formatDuration(var6.timeToFinish), 50);
         } else if (var3.equals("Optimizer")) {
            CLILogger.log(L.t("Total steps", new Object[0]), var6.totalSteps + "", 50);
            CLILogger.log(L.t("Step", new Object[0]), var6.step + "", 50);
            CLILogger.log(L.t("Running time so far", new Object[0]), SQUtils.formatDuration(var6.taskRunningTime), 50);
            CLILogger.log(L.t("Estimated time to finish", new Object[0]), SQUtils.formatDuration(var6.timeToFinish), 50);
         } else {
            CLILogger.log(L.t("Strategies generated", new Object[0]), var6.totalJobsDone + "", 50);
            CLILogger.log(L.t("Time per strategy", new Object[0]), SQUtils.formatDuration(var6.timePerStrategy), 50);
            CLILogger.log(L.t("Time per accepted strategy", new Object[0]), SQUtils.formatDuration(var6.timePerAcceptedStrategy), 50);
            CLILogger.log(L.t("Rejected", new Object[0]), var8 + " %", 50);
            CLILogger.log(L.t("Accepted", new Object[0]), var7 + " %", 50);
            CLILogger.log(L.t("Failed", new Object[0]), var6.strategiesFailed + "", 50);
            CLILogger.log(L.t("Passed", new Object[0]), var6.strategiesPassed + "", 50);
            CLILogger.log(L.t("Strategies per hour", new Object[0]), this.formatIntOrDouble(var6.strategiesPerHour), 50);
            CLILogger.log(L.t("Accepted strategies per hour", new Object[0]), this.formatIntOrDouble(var6.acceptedStrategiesPerHour), 50);
            CLILogger.log(L.t("Running time so far", new Object[0]), SQUtils.formatDuration(var6.totalProjectRunningTime), 50);
            CLILogger.log(L.t("In databank", new Object[0]), var5.getInt("strategies") + "", 50);
         }

         return var2.toString();
      } else {
         throw new Exception(L.t("Not implemented.", new Object[0]));
      }
   }

   private String onList() throws Exception {
      JSONObject var1 = new JSONObject();
      List var2 = ProjectEngine.list();
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            CLILogger.log(((SQProject)var2.get(var3)).getName());
         }
      } else {
         JSONArray var7 = new JSONArray();

         for (SQProject var5 : var2) {
            JSONObject var6 = new JSONObject();
            var6.put("name", var5.getName());
            var7.put(var6);
         }

         var1.put("projects", var7);
         var1.put("success", L.t("Projects listed.", new Object[0]));
      }

      return var1.toString();
   }

   private String formatIntOrDouble(double var1) {
      return var1 > 1000.0 ? String.format("%.0f", var1) : String.format("%.2f", var1);
   }

   private String onMergeWFStrategies(final Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      (new Thread() {
         @Override
         public void run() {
            ProjectServlet.this.mergeWFStrategies(var1);
         }
      }).start();
      var2.put("success", L.t("Merging WF strategies started.", new Object[0]));
      return var2.toString();
   }

   private void mergeWFStrategies(Map<String, String[]> var1) {
      SQProject var2 = null;

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         var2 = ProjectEngine.get(var3);
         String var4 = this.tryGetParam(var1, "strategies")[0];
         String[] var5 = var4.split(",");
         String var6 = "Portfolio";
         String var7 = this.tryGetParam(var1, "databankName")[0];
         Databank var8 = (Databank)var2.getDatabanks().get(var7);
         Databank var9 = this.getDatabank(var1);
         if (var5.length == 1 && var5[0].equals("all")) {
            var5 = new String[var9.size()];
            var9.getRecordKeys().toArray(var5);
         }

         var2.publisher.resetLastData("progress-channel");
         ArrayList var10 = new ArrayList();

         try {
            for (String var14 : var5) {
               ResultsGroup var15 = var9.getLocked(var14, "onMergeStrategies");
               var10.add(var15);
            }

            var2.getProgress().update("Merge strategies", -1.0, null, L.t("Merging WF strategies", new Object[0]));
            ResultsGroup var23 = StrategyMerger.createSimulatedWFResult(var2, var10, var6);
            var8.add(var23, true);
            var8.updateBestResults(var23);
            WebSocketNotification.sendSuccess(L.t("Portfolio created '%s'", new Object[]{var23.getName()}), new String[]{"SQUANT"});
         } finally {
            for (ResultsGroup var18 : var10) {
               var18.releaseLock("onMergeStrategies");
            }

            var2.getProgress().update("Merge strategies", 100.0, null);
         }
      } catch (Throwable var22) {
         Log.error("Creating portfolio failed.", var22);
         if (var2 != null) {
            var2.getProgress().update("Merge strategies", 100.0, L.t("Merging strategies failed", new Object[0]) + " - " + var22.getMessage(), null);
         }
      }
   }
}
