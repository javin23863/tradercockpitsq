package com.strategyquant.plugin.Results.impl.DatabankActions;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.databank.DatabankExport;
import com.strategyquant.tradinglib.options.TradingOptionsList;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import com.strategyquant.tradinglib.project.websocket.WebSocketNotification;
import com.strategyquant.tradinglib.propertygrid.IPGParameter;
import com.strategyquant.tradinglib.strategyConfig.StrategyConfig;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankActionsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DatabankActionsServlet.class);
   private StrategyConfig strategyConfig = new StrategyConfig();

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getStrategyXml":
            return this.onGetStrategyXml(var2);
         case "saveStrategyXml":
            return this.onSaveStrategyXml(var2);
         case "exportDatabankToCSV":
            return this.onExportDatabankToCSV(var2);
         case "getStrategyParameters":
            return this.onGetStrategyParameters(var2);
         case "setStrategyParameters":
            return this.onSetStrategyParameters(var2);
         case "compareStrategies":
            return this.onCompareStrategies(var2);
         default:
            throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
      }
   }

   private String onCompareStrategies(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"project", "databank", "strategies"});
      String var2 = this.tryGetParam(var1, "strategies")[0];
      JSONObject var3 = new JSONObject();
      new JSONArray();
      String[] var5 = var2.split(",");
      Databank var6 = this.getDatabank(var1);
      ResultsGroup var7 = null;
      ResultsGroup var8 = null;

      JSONArray var4;
      try {
         var7 = var6.getLocked(var5[0], "DACompareStrategies");
         var8 = var6.getLocked(var5[1], "DACompareStrategies");
         String var9 = var7.getLastSettings();
         String var10 = var8.getLastSettings();
         var3.put("strategy1", var7.getName());
         var3.put("strategy2", var8.getName());
         Element var11 = XMLUtil.stringToElement(var9);
         Element var12 = XMLUtil.stringToElement(var10);
         ArrayList var13 = new ArrayList();

         for (Element var15 : var11.getChildren()) {
            if (!var13.contains(var15.getName())) {
               var13.add(var15.getName());
            }
         }

         for (Element var20 : var12.getChildren()) {
            if (!var13.contains(var20.getName())) {
               var13.add(var20.getName());
            }
         }

         var4 = this.strategyConfig.print(var13, var11, var12);
      } finally {
         if (var7 != null) {
            var7.releaseLock("DACompareStrategies");
         }

         if (var8 != null) {
            var8.releaseLock("DACompareStrategies");
         }
      }

      var3.put("settings", var4);
      var3.put("success", L.t("Listed.", new Object[0]));
      return var3.toString();
   }

   private String onExportDatabankToCSV(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      this.checkParamExists(var1, new String[]{"projectName", "databankName", "path"});
      String var3 = this.tryGetParam(var1, "projectName")[0];
      String var4 = this.tryGetParam(var1, "databankName")[0];
      boolean var5 = false;

      try {
         var5 = Boolean.parseBoolean(this.tryGetParam(var1, "useComma")[0]);
         MainApp.settings().set("CSVExportUseComma", String.valueOf(var5));
      } catch (Exception var13) {
      }

      SQProject var6 = ProjectEngine.get(var3);
      if (!var6.getDatabanks().containsKey(var4)) {
         throw new Exception(L.t("Databank '%s' doesn't exist.", new Object[]{var4}));
      }

      final Databank var7 = (Databank)var6.getDatabanks().get(var4);
      String var8 = this.getParam(var1, "strategies", "all");
      if (var8.equals("all")) {
         ArrayList var9 = var7.getRecordKeys();
         var8 = String.join(",", var9);
      }

      final String var14 = this.getParam(var1, "view", null);
      final String var10 = ((String[])var1.get("path"))[0];
      final boolean var11 = var5;
      final String var12 = var8;
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         this.exportDatabankContents(var7, var10, var12, var11, var14);
         var2.put("success", L.t("Databank contents exported.", new Object[0]));
      } else {
         (new Thread() {
            @Override
            public void run() {
               try {
                  DatabankActionsServlet.this.exportDatabankContents(var7, var10, var12, var11, var14);
                  WebSocketNotification.sendSuccess(L.t("Databank content exported", new Object[0]), new String[]{"SQUANT"});
               } catch (Exception var2x) {
                  WebSocketNotification.sendError(var2x.getMessage(), new String[]{"SQUANT"});
               }
            }
         }).run();
         var2.put("success", L.t("Exporting databank contents.", new Object[0]));
      }

      return var2.toString();
   }

   private void exportDatabankContents(Databank var1, String var2, String var3, boolean var4, String var5) throws Exception {
      DatabankExport var6 = new DatabankExport();
      if (var2.endsWith("xlsx")) {
         var6.toXlsx(var1, var2, var3, var4, var5);
      } else {
         var6.toCsv(var1, var2, var3, var4, var5, ";");
      }
   }

   private String onSaveStrategyXml(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"project", "databank", "strategy", "xml"});
      String var2 = ((String[])var1.get("strategy"))[0];
      String var3 = ((String[])var1.get("xml"))[0];
      Databank var4 = this.getDatabank(var1);
      ResultsGroup var5 = null;

      try {
         var5 = var4.getLocked(var2, "DASonSaveStrategyXml");
      } catch (Exception var12) {
         throw new Exception(L.t("Strategy '%s' doesn't exist in databank now!", new Object[]{var2}), var12);
      }

      try {
         Element var6 = XMLUtil.stringToXmlElement(SQUtils.fixRenamedActionParams(var3));
         var5.portfolio().addStrategyXml(var6);
         String var7 = new File(var4.getResultsFilePath(var5)).getPath();
         var4.getReportSaver().save(var5, "sqx", var7, false, 0);
      } finally {
         var5.releaseLock("DASonSaveStrategyXml");
      }

      JSONObject var14 = new JSONObject();
      var14.put("success", L.t("Strategy saved successfully.", new Object[0]));
      return var14.toString();
   }

   private String onGetStrategyXml(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"app", "project", "databank", "strategy"});
      String var2 = this.tryGetParam(var1, "app")[0];
      String var3 = this.tryGetParam(var1, "project")[0];
      String var4 = this.tryGetParam(var1, "databank")[0];
      String var5 = ((String[])var1.get("strategy"))[0];
      Databank var6 = this.getDatabank(var1);
      ResultsGroup var7 = var6.getLocked(var5, "DASonGetStrategyXml");
      Element var8 = null;

      try {
         var8 = var7.portfolio().getStrategyXml();
      } finally {
         var7.releaseLock("DASonGetStrategyXml");
      }

      if (var8 == null) {
         Log.error("Exc.", "Cannot edit strategy. strategyXml is null.");
         throw new Exception(L.t("Result doesn't contain editable strategy!", new Object[0]));
      } else {
         String var9 = XMLUtil.elementToString(var8);
         JSONObject var10 = new JSONObject();
         var10.put("app", var2);
         var10.put("project", var3);
         var10.put("databank", var4);
         var10.put("strategy", var5);
         var10.put("xml", var9);
         var10.put("lastSettings", var7.getLastSettings());
         var10.put("success", L.t("Sources listed.", new Object[0]));
         return var10.toString();
      }
   }

   private String onGetStrategyParameters(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"project", "databank", "strategy", "symmetricVariables"});
      String var2 = ((String[])var1.get("strategy"))[0];
      boolean var3 = Boolean.parseBoolean(((String[])var1.get("symmetricVariables"))[0].toString());
      MainApp.settings().set("editStrategyParamsSymmetry", String.valueOf(var3));
      Databank var4 = this.getDatabank(var1);
      ResultsGroup var5 = var4.getLocked(var2, "DASonGetStrategyParameters");
      Element var6 = null;

      try {
         var6 = StrategyXMLModifier.getUpdatedStrategyXML(var5);
      } finally {
         var5.releaseLock("DASonGetStrategyParameters");
      }

      if (var6 == null) {
         Log.error("Exc.", "Cannot edit strategy. Strategy XML is null.");
         throw new Exception(L.t("Result doesn't contain editable strategy!", new Object[0]));
      }

      StrategyBase var7 = StrategyBase.createXmlStrategy(var6);
      var7.transformToVariables(var3, this.buildAllParamTypes());
      Variables var8 = var7.variables();
      JSONObject var9 = new JSONObject();
      JSONArray var10 = new JSONArray();

      for (int var11 = 0; var11 < var8.size(); var11++) {
         Variable var12 = (Variable)var8.get(var11);
         JSONObject var13 = new JSONObject();
         var13.put("name", var12.getName());
         var13.put("value", var12.getValue());
         var13.put("type", var12.getType());
         var10.put(var13);
      }

      var9.put("variables", var10);
      var9.put("success", L.t("Parameters listed.", new Object[0]));
      return var9.toString();
   }

   private String onSetStrategyParameters(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"project", "databank", "strategy", "symmetricVariables", "parameters"});
      String var2 = ((String[])var1.get("strategy"))[0];
      String var3 = ((String[])var1.get("parameters"))[0];
      boolean var4 = true;
      Databank var5 = this.getDatabank(var1);
      ResultsGroup var6 = var5.getLocked(var2, "DASonSetStrategyParameters");
      Element var7 = null;

      try {
         var7 = var6.getStrategyXml();
      } finally {
         var6.releaseLock("DASonSetStrategyParameters");
      }

      if (var7 == null) {
         Log.error("Exc.", "Cannot edit strategy. Strategy XML is null.");
         throw new Exception(L.t("Result doesn't contain editable strategy!", new Object[0]));
      }

      String[] var8 = var3.split(",");
      HashMap var9 = new HashMap();

      for (int var10 = 0; var10 < var8.length; var10++) {
         String[] var11 = var8[var10].split("=");
         String var12 = var11[0];
         var9.put(var12, var11[1]);
         if ((var12.startsWith("Long") || var12.startsWith("Short")) && !var12.contains("Signal") && !var12.contains("signal")) {
            var4 = false;
         }
      }

      for (Element var47 : XMLUtil.getNestedElements(var7, "Param")) {
         String var13 = var47.getAttributeValue("variable");
         if (var13 != null && var13.equals("true")) {
            String var14 = var47.getTextTrim();
            if (var14 != null && var9.containsKey(var14)) {
               var47.setText((String)var9.get(var14));
               var47.setAttribute("variable", "false");
            }
         }
      }

      StrategyBase var46 = StrategyBase.createXmlStrategy(var7);
      var46.transformToVariables(var4, this.buildAllParamTypes());
      Variables var48 = var46.variables();

      for (int var49 = 0; var49 < var48.size(); var49++) {
         Variable var52 = (Variable)var48.get(var49);
         if (var9.containsKey(var52.getName())) {
            var52.setFromString((String)var9.get(var52.getName()));
         }
      }

      var46.transformToNumbers();
      var6 = var5.getLocked(var2, "DASonSetStrategyParameters2");

      try {
         try {
            Element var50 = XMLUtil.stringToElement(var6.getLastSettings());
            Element var53 = var50.getChild("Options").getChild("BuildTradingOptions").getChild("Params");
            List var15 = var53.getChildren("Param");
            List var16 = TradingOptionsList.getInstance().getAvailableClasses();

            for (String var18 : var9.keySet()) {
               boolean var19 = false;

               for (int var20 = 0; var20 < var16.size() && !var19; var20++) {
                  TradingOption var21 = (TradingOption)var16.get(var20);
                  String var22 = var21.getClass().getSimpleName();
                  ArrayList var23 = var21.getParams();

                  for (int var24 = 0; var24 < var23.size() && !var19; var24++) {
                     IPGParameter var25 = (IPGParameter)var23.get(var24);
                     String var26 = var25.getKey();
                     if (var25.getName().equals(var18)) {
                        for (int var27 = 0; var27 < var15.size(); var27++) {
                           Element var28 = (Element)var15.get(var27);
                           String var29 = var28.getAttributeValue("className");
                           String var30 = var28.getAttributeValue("key");
                           if (var29 != null && var30 != null && var29.equals(var22) && var30.equals(var26)) {
                              String var31 = (String)var9.get(var18);
                              if (var25.getType() == 10) {
                                 var31 = "" + SQTime.HHMMToMinutes(Integer.parseInt(var31)) * 60;
                              }

                              var28.setText(var31);
                              var19 = true;
                              break;
                           }
                        }
                     }
                  }
               }
            }

            var6.setLastSettings(XMLUtil.elementToString(var50));
         } catch (Exception var40) {
            Log.error("Cannot apply trading options params to last settings", var40);
         }

         var6.portfolio().addStrategyXml(var46.getStrategyXml());
         var6.updated = true;
      } finally {
         var6.releaseLock("DASonSetStrategyParameters2");
      }

      var6.specialValues().setString("OptimizationParameters", var3);
      var5.update(var6.getName(), var6, true, "DASonSetStrategyParameters3");
      JSONObject var51 = new JSONObject();
      var51.put("success", L.t("Parameters set.", new Object[0]));
      return var51.toString();
   }

   private Databank getDatabank(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "project")[0];
      String var3 = this.tryGetParam(var1, "databank")[0];
      SQProject var4 = ProjectEngine.get(var2);
      if (!var4.getDatabanks().containsKey(var3)) {
         throw new Exception(L.t("Databank '%s' doesn't exist.", new Object[]{var3}));
      } else {
         return (Databank)var4.getDatabanks().get(var3);
      }
   }

   private ValuesMap buildAllParamTypes() {
      ValuesMap var1 = new ValuesMap();
      var1.set("ParamTypePeriod", true);
      var1.set("ParamTypeShift", true);
      var1.set("ParamTypeConstant", true);
      var1.set("ParamTypeOtherParam", true);
      var1.set("ParamTypeEntryLevel", true);
      var1.set("ParamTypeEntryLogic", true);
      var1.set("ParamTypeExitUsed", true);
      var1.set("ParamTypeExitUnused", true);
      var1.set("ParamTypeBoolean", true);
      var1.set("ParamTypeTradingOptions", true);
      return var1;
   }
}
