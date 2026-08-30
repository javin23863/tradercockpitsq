package com.strategyquant.plugin.App.impl.SQXBusiness;

import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;
import com.strategyquant.lib.sqxbusiness.MQLMarketConst;
import com.strategyquant.lib.sqxbusiness.SQXBusinessMainSettings;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.optimization.Parameter;
import com.strategyquant.tradinglib.optimization.ParametersSettings;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQXBusinessServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SQXBusinessServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      if (var1.equals("listProjects") || var1.equals("loadMainSettings") || MainApp.v571hfnsHw().aDm88fRJB2() && !MainApp.v571hfnsHw().a1wUchdumV()) {
         switch (var1) {
            case "checkFileExists":
               return this.onCheckFileExists(var2);
            case "loadMainSettings":
               return this.onLoadMainSettings();
            case "saveMainSettings":
               return this.onSaveMainSettings(var2);
            case "listProjects":
               return this.onListProjects();
            case "addProject":
               return this.onAddProject(var2);
            case "updateProject":
               return this.onUpdateProject(var2);
            case "removeProject":
               return this.onRemoveProject(var2);
            case "uploadStrategyFile":
               return this.onUploadStrategyFile(var2);
            case "uploadLogo":
               return this.onUploadLogo(var2);
            case "uploadResource":
               return this.onUploadResource(var2);
            case "deleteResource":
               return this.onDeleteResource(var2);
            case "loadParameters":
               return this.onLoadParameters(var2);
            case "loadEAOptions":
               return this.onLoadEAOptions(var2);
            case "clearLog":
               return this.onClearLog(var2);
            case "build":
               return this.onBuild(var2);
            case "stop":
               return this.onStop(var2);
            case "pause":
               return this.onPause(var2);
            case "resume":
               return this.onResume(var2);
            default:
               throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
         }
      } else {
         return apiErrorJSON(L.t("SQX business functionality is available for ultimate users only!", new Object[0]), null);
      }
   }

   private String onCheckFileExists(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"path"});
      String var2 = this.tryGetParamValue(var1, "path");
      return new JSONObject().put("success", L.t("File checked", new Object[0])).put("exists", new File(var2).exists()).toString();
   }

   private String onLoadMainSettings() {
      JSONObject var1 = new JSONObject();
      var1.put("mt4Path", SQXBusinessMainSettings.getMT4Path());
      var1.put("mt5Path", SQXBusinessMainSettings.getMT5Path());
      return var1.toString();
   }

   private String onSaveMainSettings(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"mt4Path", "mt5Path"});
      String var2 = this.tryGetParamValue(var1, "mt4Path");
      String var3 = this.tryGetParamValue(var1, "mt5Path");

      try {
         SQXBusinessMainSettings.save(var2, var3);
         return new JSONObject().put("success", L.t("Settings saved", new Object[0])).toString();
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Error while saving settings", new Object[0]), var5);
      }
   }

   private String onListProjects() {
      JSONObject var1 = new JSONObject();

      try {
         JSONArray var2 = new JSONArray();

         for (String var4 : MQLMarketProjects.list().keySet()) {
            JSONObject var5 = new JSONObject();
            var5.put("name", var4);
            var5.put("config", XMLUtil.elementToString(MQLMarketProjects.list().get(var4)));
            var2.put(var5);
         }

         var1.put("projects", var2);
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Error while loading SQX Business projects", new Object[0]), var6);
      }

      var1.put("success", L.t("Projects listed", new Object[0]));
      return var1.toString();
   }

   private String onAddProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"name", "config"});

      try {
         String var2 = this.tryGetParamValue(var1, "name");
         String var3 = this.tryGetParamValue(var1, "config");
         MQLMarketProjects.add(var2, XMLUtil.stringToElement(var3));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot add project", new Object[0]), var4);
      }

      JSONObject var5 = new JSONObject();
      var5.put("success", L.t("Project added", new Object[0]));
      return var5.toString();
   }

   private String onUpdateProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"name", "config"});

      try {
         String var2 = this.tryGetParamValue(var1, "name");
         String var3 = this.tryGetParamValue(var1, "config");
         String var4 = null;

         try {
            var4 = this.tryGetParamValue(var1, "oldName");
         } catch (Exception var6) {
         }

         MQLMarketProjects.update(var2, XMLUtil.stringToElement(var3), var4);
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot update project", new Object[0]), var7);
      }

      JSONObject var8 = new JSONObject();
      var8.put("success", L.t("Project updated", new Object[0]));
      return var8.toString();
   }

   private String onRemoveProject(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"name"});

      try {
         String var2 = this.tryGetParamValue(var1, "name");
         MQLMarketProjects.remove(var2);
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot update project", new Object[0]), var3);
      }

      JSONObject var4 = new JSONObject();
      var4.put("success", L.t("Project removed", new Object[0]));
      return var4.toString();
   }

   private String onUploadStrategyFile(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"fileName", "file", "projectName"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "projectName");
      String var4 = this.tryGetParamValue(var1, "fileName");
      String var5 = this.tryGetParamValue(var1, "file");

      try {
         byte[] var6 = Base64.getDecoder().decode(var5.getBytes());
         String var7 = MQLMarketConst.getProjectDirPath(var3);
         String var8 = var7 + "/" + var4;
         File[] var9 = new File(var7).listFiles();

         for (int var10 = 0; var10 < var9.length; var10++) {
            File var11 = var9[var10];
            String var12 = var11.getName();
            if (!var11.isDirectory() && !var12.equals("config.xml") && !var12.startsWith("logo") && !var12.endsWith("ex4") && !var12.endsWith("ex5")) {
               var11.delete();
            }
         }

         FileUtils.writeByteArrayToFile(new File(var8), var6);
      } catch (Exception var13) {
         return apiErrorJSON(L.t("Uploading file failed", new Object[0]), var13);
      }

      return var2.toString();
   }

   private String onUploadLogo(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"fileName", "file", "projectName"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "projectName");
      String var4 = this.tryGetParamValue(var1, "fileName");
      String var5 = this.tryGetParamValue(var1, "file");

      try {
         String var6 = SQUtils.getExtension(var4);
         String var7 = "logo." + var6;
         byte[] var8 = Base64.getDecoder().decode(var5.getBytes());
         String var9 = MQLMarketConst.getProjectDirPath(var3);
         String var10 = var9 + "/" + var7;
         File[] var11 = new File(var9).listFiles();

         for (int var12 = 0; var12 < var11.length; var12++) {
            File var13 = var11[var12];
            String var14 = var13.getName();
            if (!var13.isDirectory() && var14.startsWith("logo")) {
               var13.delete();
            }
         }

         FileUtils.writeByteArrayToFile(new File(var10), var8);
         var2.put("success", L.t("Logo saved", new Object[0]));
         var2.put("logoSrc", "data:image/png;base64," + var5);
      } catch (IOException var15) {
         return apiErrorJSON(L.t("Uploading file failed", new Object[0]), var15);
      }

      return var2.toString();
   }

   private String onUploadResource(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"fileName", "file", "projectName"});
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "projectName");
      String var4 = this.tryGetParamValue(var1, "fileName");
      String var5 = this.tryGetParamValue(var1, "file");
      String var6 = "";

      try {
         var6 = this.tryGetParamValue(var1, "oldResource");
      } catch (Exception var10) {
      }

      try {
         byte[] var7 = Base64.getDecoder().decode(var5.getBytes());
         String var8 = MQLMarketConst.getProjectDirPath(var3) + "/resources";
         if (!var6.isEmpty()) {
            new File(var8 + "/" + var6).delete();
         }

         File var9 = new File(var8 + "/" + var4);
         if (var9.exists()) {
            throw new Exception(L.t("File '%s' already exists<br>Please delete the old resource or choose another file", new Object[]{var4}));
         }

         var9.getParentFile().mkdirs();
         FileUtils.writeByteArrayToFile(var9, var7);
         var2.put("success", L.t("Resource saved", new Object[0]));
      } catch (IOException var11) {
         return apiErrorJSON(L.t("Uploading resource failed", new Object[0]), var11);
      }

      return var2.toString();
   }

   private String onDeleteResource(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"resourceName", "projectName"});
      String var2 = this.tryGetParamValue(var1, "projectName");
      String var3 = this.tryGetParamValue(var1, "resourceName");
      String var4 = MQLMarketConst.getProjectDirPath(var2);
      File var5 = new File(var4 + "/resources/" + var3);
      if (var5.exists() && !var5.delete()) {
         return apiErrorJSON(L.t("Unable to delete resource file from disk", new Object[0]), null);
      }

      JSONObject var6 = new JSONObject();
      var6.put("success", L.t("Resource file deleted", new Object[0]));
      return var6.toString();
   }

   private String onLoadParameters(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"fileName", "projectName"});
      String var2 = this.tryGetParamValue(var1, "projectName");
      String var3 = this.tryGetParamValue(var1, "fileName");
      String var4 = MQLMarketConst.getProjectDirPath(var2);
      String var5 = var4 + "/" + var3;
      JSONObject var6 = new JSONObject();
      var6.put("success", L.t("Parameters loaded", new Object[0]));

      try {
         if (!new File(var5).exists()) {
            throw new Exception(L.t("Strategy file doesn't exist", new Object[0]));
         }

         Element var7 = MQLMarketProjects.list().get(var2);
         if (var7 == null) {
            throw new Exception(L.t("Project config not found", new Object[0]));
         }

         JSONArray var8 = new JSONArray();
         StrategyBase var9 = EAParameters.getStrategyBase(var7);
         if (var9 != null) {
            Variables var10 = var9.variables();
            var10.sortByName();

            for (int var11 = 0; var11 < var10.size(); var11++) {
               Variable var12 = (Variable)var10.get(var11);
               byte var13 = var12.getInternalType();
               String var14 = var12.getName();
               String var15 = var12.getValue();
               if (var13 != 3
                  && !var14.contains("EntrySignal")
                  && !var14.contains("ExitSignal")
                  && !var14.contains("ReplaceExistingOrders")
                  && var13 != 0
                  && !var14.equals("MagicNumber")) {
                  JSONObject var16 = new JSONObject();
                  var16.put("id", var12.getId());
                  var16.put("use", true);
                  var16.put("type", var12.getType());
                  var16.put("name", var14);
                  var16.put("paramType", var12.getParamType());
                  if (var12.getInternalType() == 2) {
                     var16.put("start", "true");
                     var16.put("stop", "false");
                     var16.put("step", "");
                  } else {
                     Parameter var17 = ParametersSettings.createParameter(var12, 30, 30, 5);
                     var16.put("start", var17.getStart());
                     var16.put("stop", var17.getStop());
                     var16.put("step", var17.getStep());
                  }

                  var16.put("value", var15);
                  var8.put(var16);
               }
            }
         }

         var6.put("variables", var8);
      } catch (Exception var18) {
         return apiErrorJSON(L.t("Cannot load strategy parameters", new Object[0]), var18);
      }

      return var6.toString();
   }

   private String onLoadEAOptions(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "fileName"});
      String var2 = this.tryGetParamValue(var1, "projectName");
      String var3 = this.tryGetParamValue(var1, "fileName");
      String var4 = MQLMarketConst.getProjectDirPath(var2);
      String var5 = var4 + "/" + var3;
      JSONObject var6 = new JSONObject();
      var6.put("success", L.t("EA options loaded", new Object[0]));

      try {
         if (!new File(var5).exists()) {
            throw new Exception(L.t("Strategy file doesn't exist", new Object[0]));
         }

         String var7 = SourceCodeGenerator.MetaTrader5;
         SourceCodeGenerator var8 = SourceCodeGenerators.getInstance().getGeneratorFromName(var7);
         if (var8 == null) {
            String var13 = L.t("Source code generator '%s' not found", new Object[]{var7});
            Log.error(var13);
            throw new Exception(var13);
         }

         ResultsGroup var9 = (ResultsGroup)Program.get("Loader").call("loadFile", new Object[]{var5});
         Element var10 = StrategyXMLModifier.getUpdatedStrategyXML(var9);
         String var11 = var8.getSource(var9.getName(), var10);
         var6.put("options", this.loadEAOptions(var11));
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot load strategy options", new Object[0]), var12);
      }

      return var6.toString();
   }

   private JSONArray loadEAOptions(String var1) throws Exception {
      JSONArray var2 = new JSONArray();
      int var3 = var1.indexOf("string CustomComment");
      if (var3 < 0) {
         throw new Exception("Source code start point not found");
      }

      int var4 = var1.indexOf("void OnTick()");
      if (var4 < 0) {
         throw new Exception("Source code end point not found");
      }

      String var5 = var1.substring(var3, var4);
      String[] var6 = var5.split("\n");

      for (int var7 = 0; var7 < var6.length; var7++) {
         String var8 = var6[var7].trim();
         if (var8.startsWith("input")) {
            String[] var9 = var8.split("=");
            if (var9.length != 2) {
               Log.error("Unrecognized EA option '" + var8 + "' - invalid parts length");
            } else {
               String[] var10 = var9[0].split(" ");
               if (var10.length != 3) {
                  Log.error("Unrecognized EA option '" + var8 + "' - invalid words length");
               } else {
                  JSONObject var11 = new JSONObject();
                  var11.put("type", var10[1]);
                  var11.put("name", var10[2]);
                  var11.put("enabled", true);
                  String var12 = var9[1].trim();
                  int var13 = var12.indexOf(";");
                  if (var13 < 0) {
                     Log.error("Unrecognized EA option '" + var8 + "' - ; not found");
                  } else {
                     var12 = var12.substring(0, var13);
                     var11.put("value", var12.replace("\"", ""));
                     var2.put(var11);
                  }
               }
            }
         }
      }

      return var2;
   }

   private String onBuild(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"projectName", "type", "platform"});
      String var2 = this.tryGetParamValue(var1, "projectName");
      String var3 = this.tryGetParamValue(var1, "type");
      String var4 = this.tryGetParamValue(var1, "platform");

      try {
         Element var5 = MQLMarketProjects.list().get(var2);
         if (var5 == null) {
            throw new Exception("Project config not found");
         }

         MQLMarketBuildExecutor.execute(var2, var5, var3, var4);
         JSONObject var6 = new JSONObject();
         var6.put("success", L.t("Build job started", new Object[0]));
         return var6.toString();
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot build project", new Object[0]), var7);
      }
   }

   private String onClearLog(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"jobID"});
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "jobID");
         SQXBusinessBuildReporter.clearLog(var3);
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Error while clearing log", new Object[0]), var4);
      }

      var2.put("success", L.t("Log cleared", new Object[0]));
      return var2.toString();
   }

   private String onStop(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"jobGroupID", "jobID"});
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "jobGroupID");
         String var4 = this.tryGetParamValue(var1, "jobID");
         SQGrid.getGridClient().stop(var3, var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Error while stopping build", new Object[0]), var5);
      }

      var2.put("success", L.t("Build stopped", new Object[0]));
      return var2.toString();
   }

   private String onPause(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"jobGroupID", "jobID"});
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "jobGroupID");
         String var4 = this.tryGetParamValue(var1, "jobID");
         SQGrid.getGridClient().pause(var3, var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Error while pausing build", new Object[0]), var5);
      }

      var2.put("success", L.t("Build paused", new Object[0]));
      return var2.toString();
   }

   private String onResume(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"jobGroupID", "jobID"});
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "jobGroupID");
         String var4 = this.tryGetParamValue(var1, "jobID");
         SQGrid.getGridClient().restart(var3, var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Error while resuming build", new Object[0]), var5);
      }

      var2.put("success", L.t("Build resumed", new Object[0]));
      return var2.toString();
   }
}
