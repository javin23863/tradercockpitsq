package com.strategyquant.plugin.TaskManager.impl.Projects;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.file.SQFileManager;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.ProjectNameComparator;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMProjectsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(TMProjectsServlet.class);
   private static final Pattern FORBIDDEN_PROJECT_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");
   private static final Pattern RESERVED_PROJECT_NAME = Pattern.compile("^(nul|prn|con|lpt[0-9]|com[0-9])(\\.|$).*", 2);
   private ProjectNameComparator projectNameComparator = new ProjectNameComparator();

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "listProjects":
            return this.onListProjects();
         case "createProject":
            return this.onCreateProject(var2);
         case "openProject":
            return this.onOpenProject(var2);
         case "saveProject":
            return this.onSaveProject(var2);
         case "removeProject":
            return this.onRemoveProject(var2);
         case "cloneProject":
            return this.onCloneProject(var2);
         case "renameProject":
            return this.onRenameProject(var2);
         case "addTask":
            return this.onAddTask(var2);
         case "removeTask":
            return this.onRemoveTask(var2);
         case "getAvailableTasks":
            return this.onGetAvailableTasks();
         case "moveTask":
            return this.onMoveTask(var2);
         case "renameTask":
            return this.onRenameTask(var2);
         case "cloneTask":
            return this.onCloneTask(var2);
         case "activateTask":
            return this.onActivateTask(var2);
         case "modifySymbolsList":
            return this.onModifySymbolsList(var2);
         case "modifySymbols":
            return this.onModifySymbols(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onListProjects() throws Exception {
      JSONObject var1 = new JSONObject();
      List var2 = ProjectEngine.list();
      var2.sort(this.projectNameComparator);
      JSONArray var3 = new JSONArray();

      for (SQProject var5 : var2) {
         if (!var5.isAppProject()) {
            var3.put(var5.getInfo());
         }
      }

      var1.put("projects", var3);
      var1.put("success", L.t("Projects listed.", new Object[0]));
      return var1.toString();
   }

   private String onCreateProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName"});
      JSONObject var2 = new JSONObject();
      String var3 = normalizeProjectName(((String[])var1.get("projectName"))[0]);
      validateProjectName(var3);
      if (ProjectEngine.projectExists(var3)) {
         throw new Exception(L.t("Project with the same name already exists.", new Object[0]));
      }

      SQProject var4 = ProjectEngine.add(var3);
      var2.put("success", L.t("Project created %s.", new Object[]{var4.getName()}));
      var2.put("projectName", var4.getName());
      return var2.toString();
   }

   private String onOpenProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"file"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("file"))[0];
      boolean var4 = false;
      if (!var3.endsWith(".cfx")) {
         var3 = var3 + ".cfx";
      }

      try {
         var4 = Boolean.parseBoolean(((String[])var1.get("loadAsIs"))[0]);
      } catch (Exception var11) {
      }

      File var5 = new File(var3);
      Element var6 = SQFileManager.loadProjectConfig(var5);
      if (!var6.getName().equals("Project")) {
         return apiErrorJSON(L.t("Cannot load config. Selected config is not a project template", new Object[0]), null);
      }

      if (var1.containsKey("projectName")) {
         var6.setAttribute("name", normalizeProjectName(this.getParam(var1, "projectName")[0]));
      }

      String var7 = normalizeProjectName(var6.getAttributeValue("name"));
      if (var7 == null) {
         return apiErrorJSON(L.t("Corrupted Project config file. Missing project name.", new Object[0]), null);
      }

      validateProjectName(var7);
      HashMap var8 = null;
      if (var4) {
         if (SQProject.projectIsOlderThan136(var6)) {
            var8 = SQFileManager.convertOldProjectConfig(var6);
         }

         SQProject var9 = ProjectEngine.add(var7, var5, var6, var8);
         var2.put("success", String.format("Project loaded '%s'.", var9.getName()));
         var2.put("projectName", var9.getName());
      } else {
         Element var13 = ProjectResources.checkProjectResources(var5, var6);
         if (var13 == null) {
            if (SQProject.projectIsOlderThan136(var6)) {
               var8 = SQFileManager.convertOldProjectConfig(var6);
            }

            SQProject var10 = ProjectEngine.add(var7, var5, var6, var8);
            var2.put("success", L.t("Project loaded '%s'.", new Object[]{var10.getName()}));
            var2.put("projectName", var10.getName());
         } else {
            var2.put("configXML", XMLUtil.elementToString(var6));
            var2.put("resourcesXML", XMLUtil.elementToString(var13));
            var2.put("success", L.t("Resources info returned.", new Object[0]));
            var7 = ProjectEngine.generateProjectName(var7);
            var2.put("projectName", var7);
         }
      }

      return var2.toString();
   }

   private String onSaveProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "file"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0];
      String var4 = ((String[])var1.get("file"))[0];
      if (!var4.endsWith(".cfx")) {
         var4 = var4 + ".cfx";
      }

      SQProject var5 = ProjectEngine.get(var3);
      SQFileManager.saveProjectFile(var5, new File(var4));
      var2.put("success", L.t("Project saved.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0];
      ProjectEngine.remove(var3);
      var2.put("success", L.t("Project removed.", new Object[0]));
      return var2.toString();
   }

   private String onRenameProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"oldName", "newName"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("oldName"))[0];
      String var4 = normalizeProjectName(((String[])var1.get("newName"))[0]);
      validateProjectName(var4);
      ProjectEngine.rename(var3, var4);
      var2.put("success", L.t("Project renamed.", new Object[0]));
      return var2.toString();
   }

   private String onCloneProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0];
      SQProject var4 = ProjectEngine.get(var3);
      Element var5 = var4.getConfig();
      SQProject var6 = ProjectEngine.add(var3, var4.getProjectConfigFile(), var5.clone(), null);
      var6.getDatabanks().loadStrategies();
      var2.put("success", L.t("Project cloned.", new Object[0]));
      return var2.toString();
   }

   private static String normalizeProjectName(String var0) {
      return var0 == null ? null : var0.trim();
   }

   private static void validateProjectName(String var0) throws Exception {
      if (var0 != null && !var0.isEmpty()) {
         if (var0.startsWith(".")) {
            throw new Exception(L.t("Project name cannot start with dot.", new Object[0]));
         }

         if (FORBIDDEN_PROJECT_NAME_CHARS.matcher(var0).find()) {
            throw new Exception(L.t("Project name contains forbidden characters: \\ / : * ? \" < > |", new Object[0]));
         }

         if (RESERVED_PROJECT_NAME.matcher(var0).matches()) {
            throw new Exception(L.t("Project name uses a reserved system name.", new Object[0]));
         }

         for (int var1 = 0; var1 < var0.length(); var1++) {
            char var2 = var0.charAt(var1);
            if (Character.isWhitespace(var2) && var2 != ' ') {
               throw new Exception(L.t("Project name cannot contain tabs or new lines.", new Object[0]));
            }
         }
      } else {
         throw new Exception(L.t("Project name cannot be empty.", new Object[0]));
      }
   }

   private String onAddTask(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "taskName", "taskType"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0];
      String var4 = ((String[])var1.get("taskName"))[0];
      String var5 = ((String[])var1.get("taskType"))[0];
      int var6 = -1;

      try {
         var6 = Integer.parseInt(((String[])var1.get("index"))[0]);
      } catch (Exception var9) {
      }

      SQProject var7 = ProjectEngine.get(var3);
      this.tryUpdateProjectConfig(var7, var1);
      ISQTask var8 = var7.addTask(var4, var5, var6);
      if (var8 == null) {
         return apiErrorJSON(L.t("Task not created. Please check task with the same name doesn't already exist.", new Object[0]), null);
      }

      var2.put("xmlConfig", XMLUtil.xmlToStringRaw(var7.getConfig()));
      var2.put("taskXML", XMLUtil.elementToString(var8.getConfig()));
      var2.put("taskXMLFile", var8.getTaskXMLFileName());
      var2.put("success", L.t("Task added.", new Object[0]));
      return var2.toString();
   }

   private String onRemoveTask(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "taskName"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0];
      String var4 = ((String[])var1.get("taskName"))[0];
      SQProject var5 = ProjectEngine.get(var3);
      this.tryUpdateProjectConfig(var5, var1);
      ISQTask var6 = var5.removeTask(var4);
      var2.put("xmlConfig", XMLUtil.xmlToStringRaw(var5.getConfig()));
      var2.put("taskXMLFile", var6.getTaskXMLFileName());
      var2.put("success", L.t("Task removed.", new Object[0]));
      return var2.toString();
   }

   private String onGetAvailableTasks() throws Exception {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (ISQTask var4 : SQPluginManager.getPlugins(ISQTask.class)) {
         if (!var4.getType().equals("NeuralNetworkTrainer")) {
            var2.put(new JSONObject().put("type", var4.getType()).put("name", var4.getName()));
         }
      }

      var1.put("tasks", var2);
      var1.put("success", L.t("Available tasks listed.", new Object[0]));
      return var1.toString();
   }

   private String onMoveTask(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "taskName", "toIndex"});
      String var2 = ((String[])var1.get("projectName"))[0];
      String var3 = ((String[])var1.get("taskName"))[0];
      int var4 = Integer.parseInt(((String[])var1.get("toIndex"))[0]);
      SQProject var5 = ProjectEngine.get(var2);
      if (var5 == null) {
         throw new Exception(L.t("Project with name '%s' does not exist.", new Object[]{var2}));
      }

      this.tryUpdateProjectConfig(var5, var1);
      var5.moveTask(var3, var4);
      JSONObject var6 = new JSONObject();
      var6.put("xmlConfig", XMLUtil.xmlToStringRaw(var5.getConfig()));
      var6.put("success", L.t("Task moved.", new Object[0]));
      return var6.toString();
   }

   private String onRenameTask(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "taskName", "taskTitle"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0];
      String var4 = ((String[])var1.get("taskName"))[0];
      String var5 = ((String[])var1.get("taskTitle"))[0];
      SQProject var6 = ProjectEngine.get(var3);
      this.tryUpdateProjectConfig(var6, var1);
      var6.setTaskTitle(var4, var5);
      var2.put("xmlConfig", XMLUtil.xmlToStringRaw(var6.getConfig()));
      var2.put("success", L.t("Task renamed.", new Object[0]));
      return var2.toString();
   }

   private String onCloneTask(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "taskName"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0];
      String var4 = ((String[])var1.get("taskName"))[0];
      SQProject var5 = ProjectEngine.get(var3);
      this.tryUpdateProjectConfig(var5, var1);
      String var6 = this.getParam(var1, "position", "end");
      ISQTask var7 = var5.cloneTask(var4, var6);
      var2.put("taskType", var7.getType());
      var2.put("taskName", var7.getName());
      var2.put("xmlConfig", XMLUtil.xmlToStringRaw(var5.getConfig()));
      var2.put("xmlConfig", XMLUtil.xmlToStringRaw(var5.getConfig()));
      var2.put("taskXML", XMLUtil.elementToString(var7.getConfig()));
      var2.put("taskXMLFile", var7.getTaskXMLFileName());
      var2.put("success", L.t("Task cloned.", new Object[0]));
      return var2.toString();
   }

   private String onActivateTask(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "taskName", "active"});
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("projectName"))[0];
      String var4 = ((String[])var1.get("taskName"))[0];
      boolean var5 = Boolean.parseBoolean(((String[])var1.get("active"))[0]);
      SQProject var6 = ProjectEngine.get(var3);
      this.tryUpdateProjectConfig(var6, var1);
      var6.setTaskActive(var4, var5);
      var2.put("success", L.t("Task status changed.", new Object[0]));
      return var2.toString();
   }

   private void tryUpdateProjectConfig(SQProject var1, Map<String, String[]> var2) {
      try {
         String var3 = ((String[])var2.get("projectXML"))[0];
         if (var3 != null) {
            Element var4 = XMLUtil.stringToElement(var3);
            var1.updateConfig(var4);
         }
      } catch (Exception var5) {
         Log.warn("Saving project config failed", var5);
      }
   }

   private String onModifySymbolsList(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName"});
      String var2 = ((String[])var1.get("projectName"))[0];
      JSONObject var3 = new JSONObject();
      ArrayList var4 = new ArrayList();
      SQProject var5 = ProjectEngine.get(var2);

      for (ISQTask var7 : var5.getTasks()) {
         Element var8 = var7.getConfig();
         Element var9 = var8.getChild("Data");
         if (var9 != null) {
            this.parseSymbols(var9, var4);
            Element var10 = XMLUtil.tryAddElement(var8, "CrossChecks");
            Element var11 = XMLUtil.tryAddElement(var10, "RetestOnAdditionalMarkets");
            Element var12 = XMLUtil.tryAddElement(var11, "Settings");
            if (var12 != null) {
               try {
                  this.parseSymbols(var12, var4);
               } catch (Exception var14) {
                  Log.debug("Error while modifying symbols. Exc.", var14);
               }
            }
         }
      }

      JSONArray var15 = new JSONArray();

      for (String var17 : var4) {
         var15.put(var17);
      }

      var3.put("symbols", var15);
      var3.put("success", L.t("Symbols listed.", new Object[0]));
      return var3.toString();
   }

   private String onModifySymbols(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "symbols"});
      String var2 = ((String[])var1.get("projectName"))[0];
      JSONArray var3 = new JSONArray(((String[])var1.get("symbols"))[0]);
      JSONArray var4 = new JSONArray();

      for (int var5 = 0; var5 < var3.length(); var5++) {
         JSONObject var6 = var3.getJSONObject(var5);
         if (var6.has("symbolFrom") && var6.has("symbolTo") && !var6.isNull("symbolFrom") && !var6.isNull("symbolTo")) {
            String var7 = var6.getString("symbolFrom");
            String var8 = var6.getString("symbolTo");
            if (var7 != null && var8 != null) {
               if (!var7.contains("/") || !var8.contains("/")) {
                  throw new Exception(L.t("Symbols must be in format SYMBOL/TF.", new Object[0]));
               }

               String var9 = var7.split("/")[0].trim();
               String var10 = var7.split("/")[1].trim();
               String var11 = var8.split("/")[0].trim();
               String var12 = var8.split("/")[1].trim();
               if (!var9.equals("*") && DataManager.getDataInfo("History", var9) == null) {
                  throw new Exception(L.t("Symbol with name '%s' doesn't exist.", new Object[]{var9}));
               }

               if (!var11.equals("*") && DataManager.getDataInfo("History", var11) == null) {
                  throw new Exception(L.t("Symbol with name '%s' doesn't exist.", new Object[]{var11}));
               }

               if (!var10.equals("*") && !TimeframeManager.TFExists(var10)) {
                  throw new Exception(L.t("Timeframe '%s' doesn't exist.", new Object[]{var10}));
               }

               if (!var12.equals("*") && !TimeframeManager.TFExists(var12)) {
                  throw new Exception(L.t("Timeframe '%s' doesn't exist.", new Object[]{var12}));
               }

               var4.put(var6);
            }
         }
      }

      if (var4.length() == 0) {
         throw new Exception(L.t("No symbols to change.", new Object[0]));
      }

      ArrayList var17 = new ArrayList();
      JSONObject var18 = new JSONObject();
      SQProject var19 = ProjectEngine.get(var2);

      for (int var20 = 0; var20 < var19.getTasks().size(); var20++) {
         ISQTask var21 = var19.getTask(var20);
         Element var22 = var21.getConfig();
         Element var23 = var22.getChild("Data");
         if (var23 != null) {
            this.modifySymbols(var22, var23, var4, var17);
            Element var24 = XMLUtil.tryAddElement(var22, "CrossChecks");
            Element var13 = XMLUtil.tryAddElement(var24, "RetestOnAdditionalMarkets");
            Element var14 = XMLUtil.tryAddElement(var13, "Settings");
            if (var14 != null) {
               try {
                  this.modifySymbols(var22, var14, var4, var17);
               } catch (Exception var16) {
                  Log.debug("Error while modifying symbols. Exc.", var16);
               }

               Element var15 = var22.getChild("Options");
               this.updateSessions(var15, var4);
               ProjectResources.addResources(var21);
            }
         }
      }

      SQFileManager.updateWholeProjectConfig(var19);
      var18.put("success", L.t("Symbols changed.", new Object[0]));
      return var18.toString();
   }

   private void updateSessions(Element var1, JSONArray var2) {
      Element var3 = var1 == null ? null : var1.getChild("BuildTradingOptions");
      Element var4 = var3 == null ? null : var3.getChild("Params");
      if (var4 != null) {
         Element var5 = null;

         for (Element var7 : var4.getChildren("Param")) {
            if ("MarketOpenSession".equals(var7.getAttributeValue("key"))) {
               var5 = var7;
               break;
            }
         }

         if (var5 != null) {
            String var15 = var5.getTextTrim();
            if (var15 != null && !var15.isEmpty()) {
               for (int var16 = 0; var16 < var2.length(); var16++) {
                  JSONObject var8 = var2.getJSONObject(var16);
                  if (var8.has("symbolFrom") && var8.has("symbolTo") && !var8.isNull("symbolFrom") && !var8.isNull("symbolTo")) {
                     String var9 = var8.getString("symbolFrom");
                     String var10 = var8.getString("symbolTo");
                     if (var9 != null && var10 != null && var9.contains("/") && var10.contains("/")) {
                        String var11 = var9.split("/")[0].trim();
                        String var12 = var10.split("/")[0].trim();
                        if (!var11.equals("*") && !var12.equals("*")) {
                           String var13 = this.resolveMarketOpenSessionForSymbol(var11);
                           if (var13 != null && var15.equals(var13)) {
                              String var14 = this.resolveMarketOpenSessionForSymbol(var12);
                              if (var14 != null && !var14.equals(var15)) {
                                 var5.setText(var14);
                              }

                              return;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private String resolveMarketOpenSessionForSymbol(String var1) {
      if (var1 != null && !var1.isEmpty()) {
         DataInfo var2 = DataManager.getDataInfo("History", var1);
         if (var2 == null) {
            return null;
         }

         String var3 = var2.instrument;
         String var4 = var2.symbolInfo != null ? var2.symbolInfo.description : null;
         if ((var4 == null || var4.isEmpty()) && var3 != null) {
            try {
               InstrumentInfo var5 = InstrumentManager.getInstrumentInfo(var3);
               if (var5 != null) {
                  var4 = var5.description;
               }
            } catch (Exception var9) {
               Log.debug("Unable to resolve instrument description for session remap. Instrument: {}", var3, var9);
            }
         }

         ArrayList var10 = SessionManager.getSessions();
         if (var10 != null && !var10.isEmpty()) {
            if (var3 != null && !var3.isEmpty()) {
               for (Session var7 : var10) {
                  if (var3.equals(var7.getSessionName())) {
                     return var7.getSessionName();
                  }
               }
            }

            if (var4 != null && !var4.isEmpty()) {
               for (Session var12 : var10) {
                  String var8 = var12.getSessionName();
                  if (var8 != null && var8.startsWith(var4)) {
                     return var8;
                  }
               }
            }

            return null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private void parseSymbols(Element var1, List<String> var2) {
      String var6 = null;
      String var7 = null;
      Element var8 = var1.getChild("Setups");
      List var9 = var8.getChildren("Setup");

      for (int var10 = 0; var10 < var9.size(); var10++) {
         Element var11 = (Element)var9.get(var10);
         List var12 = var11.getChildren("Chart");

         for (int var13 = 0; var13 < var12.size(); var13++) {
            String var3;
            if (var13 == 0) {
               var6 = ((Element)var12.get(var13)).getAttributeValue("symbol");
               var7 = ((Element)var12.get(var13)).getAttributeValue("timeframe");
               var3 = var6 + "/" + var7;
            } else {
               String var4 = ((Element)var12.get(var13)).getAttributeValue("symbol");
               String var5 = ((Element)var12.get(var13)).getAttributeValue("timeframe");
               if (var4.equals("Same as main chart")) {
                  var4 = var6;
               }

               if (var5.equals("Same as main data")) {
                  var5 = var7;
               }

               var3 = var4 + "/" + var5;
            }

            if (!var2.contains(var3)) {
               var2.add(var3);
            }
         }
      }
   }

   private void updateSpreadInCrossCheckRetestHigherPrecision(Element var1, double var2) throws JDOMException, IOException {
      Element var4 = var1.getChild("CrossChecks");
      if (var4 != null) {
         Element var5 = var4.getChild("RetestWithHigherPrecision");
         if (var5 != null) {
            Element var6 = var5.getChild("Settings");
            if (var6 != null) {
               boolean var7 = XMLUtil.getBoolean(var6, "CustomSpread", false);
               if (!var7) {
                  Element var8 = XMLUtil.stringToElement("<Spread>" + var2 + "</Spread>");
                  var6.removeChild("Spread");
                  var6.addContent(var8);
               }
            }
         }
      }
   }

   private void modifySymbols(Element var1, Element var2, JSONArray var3, List<String> var4) throws Exception {
      Element var7 = var2.getChild("Setups");
      List var8 = var7.getChildren("Setup");

      for (int var9 = 0; var9 < var8.size(); var9++) {
         Element var10 = (Element)var8.get(var9);
         String var11 = null;
         String var12 = null;
         List var13 = var10.getChildren("Chart");

         for (int var14 = 0; var14 < var13.size(); var14++) {
            Element var15 = (Element)var13.get(var14);
            String var5;
            String var6;
            if (var14 == 0) {
               var11 = var15.getAttributeValue("symbol");
               var12 = var15.getAttributeValue("timeframe");
               var5 = var11;
               var6 = var12;
            } else {
               var5 = var15.getAttributeValue("symbol");
               var6 = var15.getAttributeValue("timeframe");
               if (var5.equals("Same as main chart")) {
                  var5 = var11;
               }

               if (var6.equals("Same as main data")) {
                  var6 = var12;
               }
            }

            for (int var16 = 0; var16 < var3.length(); var16++) {
               JSONObject var17 = var3.getJSONObject(var16);
               if (var17.has("symbolFrom") && var17.has("symbolTo") && !var17.isNull("symbolFrom") && !var17.isNull("symbolTo")) {
                  String var18 = var17.getString("symbolFrom");
                  String var19 = var17.getString("symbolTo");
                  boolean var20 = var17.getBoolean("applyDefaultSettings");
                  if (var18 != null && var19 != null && var18.contains("/") && var19.contains("/")) {
                     String var21 = var18.split("/")[0].trim();
                     String var22 = var18.split("/")[1].trim();
                     String var23 = var19.split("/")[0].trim();
                     String var24 = var19.split("/")[1].trim();
                     if (var21.equals("*")) {
                        var21 = var5;
                     }

                     if (var22.equals("*")) {
                        var22 = var6;
                     }

                     if (var23.equals("*")) {
                        var23 = var5;
                     }

                     if (var24.equals("*")) {
                        var24 = var6;
                     }

                     if (var5.equals(var21) && var6.equals(var22)) {
                        var15.setAttribute("symbol", var23);
                        var15.setAttribute("timeframe", var24);
                        if (var20) {
                           DataInfo var25 = DataManager.getDataInfo("History", var23);
                           var15.setAttribute("spread", var25.symbolInfo.defaultSpread + "");
                           var10.setAttribute("slippage", var25.symbolInfo.defaultSlippage + "");

                           try {
                              Element var26 = XMLUtil.stringToElement(var25.symbolInfo.commissions);
                              var10.getChild("Commissions").removeContent();
                              var10.getChild("Commissions").addContent(var26);
                           } catch (Exception var27) {
                              throw new Exception(L.t("Error while applying default commission.", new Object[0]) + " " + var27.getMessage(), var27);
                           }

                           try {
                              if (var25.symbolInfo.swap != null) {
                                 Element var29 = XMLUtil.stringToElement(var25.symbolInfo.swap);
                                 var10.removeChild("Swap");
                                 var10.addContent(var29);
                              }
                           } catch (Exception var28) {
                              throw new Exception(L.t("Error while applying default swap.", new Object[0]) + " " + var28.getMessage(), var28);
                           }

                           if (var14 == 0) {
                              this.updateSpreadInCrossCheckRetestHigherPrecision(var1, var25.symbolInfo.defaultSpread);
                           }
                        }

                        if (!var4.contains(var23)) {
                           var4.add(var23);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
