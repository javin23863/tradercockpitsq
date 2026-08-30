package com.strategyquant.plugin.Settings.impl.Optimization;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.Variables;
import com.strategyquant.tradinglib.optimization.OptimizationSettings;
import com.strategyquant.tradinglib.optimization.Parameter;
import com.strategyquant.tradinglib.optimization.ParametersSettings;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectCustomResources;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import com.strategyquant.tradinglib.strategy.StrategyLoader;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(OptimizationServlet.class);
   private static final String LOCK_OPTSERVLET = "OptimizationServlet";
   private static OptimizationServlet instance;

   public OptimizationServlet() {
      instance = this;
   }

   public static OptimizationServlet getInstance() {
      return instance;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "loadStrategyToOptimize":
            return this.onLoadStrategyToOptimize(var2);
         case "loadStrategy":
            return this.onLoadStrategy(var2);
         case "getNumberOfTests":
            return this.onGetNumberOfTests(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onLoadStrategyToOptimize(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      SQProject var3 = null;

      try {
         String var4 = this.tryGetParam(var1, "projectName")[0];
         String var5 = this.tryGetParam(var1, "taskName")[0];
         String var6 = this.tryGetParam(var1, "filePath")[0];
         String var7 = this.tryGetParam(var1, "clearResults")[0];
         boolean var8 = var7.equals("true");
         String var9 = new File(var6).getName();
         var3 = ProjectEngine.get(var4);
         Databank var10 = ProjectConfigHelper.getOutputDatabank(var4, var5);
         ResultsGroup var11 = null;
         StrategyLoader.init();
         var11 = StrategyLoader.loadStrategy(var3, var9, var6, 99.0);
         if (var11 == null) {
            throw new Exception(L.t("Error while loading.", new Object[0]));
         }

         var3.getProgress().update("load", 100.0, null);

         for (String var13 : var10.getRecordKeys()) {
            if (var8 || var13.equals(var11.getName())) {
               var10.remove(var13, true, true, false, true, "OptimizationServlet");
            }
         }

         var10.add(var11, true);
         var10.updateBestResults(var11);
         if (var11.getLastSettings() != null) {
            var2.put("lastSettingsXml", var11.getLastSettings());
         }
      } catch (Exception var14) {
         Log.error("Cannot load strategy. ", var14);
         if (var3 != null) {
            var3.getProgress().update("load", 100.0, var14.getMessage());
         }

         return apiErrorJSON(L.t("Cannot load strategy.", new Object[0]), var14);
      }

      var2.put("success", L.t("Report(s) loaded.", new Object[0]));
      return var2.toString();
   }

   private String onLoadStrategy(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      SQProject var3 = null;
      String var4 = null;

      try {
         String var5 = this.tryGetParam(var1, "projectName")[0];
         String var6 = this.tryGetParam(var1, "taskName")[0];
         var4 = this.tryGetParam(var1, "filePath")[0];
         String var7 = this.tryGetParam(var1, "symmetricVariables")[0];
         boolean var8 = var7.equals("true");
         var3 = ProjectEngine.get(var5);
         Element var9 = var3.getTaskSettingsByName(var6);
         Databank var10 = ProjectConfigHelper.getOutputDatabank(var5, var9);
         String var11 = new File(var4).getName();
         String var12 = SQUtils.stripExtension(var11);
         Element var13 = null;
         ResultsGroup var14 = null;

         try {
            var14 = var10.getLocked(var12, "OptimizationServlet");
            var13 = StrategyXMLModifier.getUpdatedStrategyXML(var14);
         } catch (Exception var20) {
            StrategyLoader.init();
            var14 = StrategyLoader.loadStrategy(var3, var12, var4, 99.0);
            if (var14 == null) {
               throw new Exception(L.t("Strategy not loaded.", new Object[0]));
            }

            var3.getProgress().update("load", 100.0, null);
            if (var10.isLoaded()) {
               var10.add(var14, true);
               var10.updateBestResults(var14);
            }

            var13 = StrategyXMLModifier.getUpdatedStrategyXML(var14);
         } finally {
            if (var14 != null) {
               var14.releaseLock("OptimizationServlet");
            }
         }

         if (var13 != null) {
            this.changeSymmetry(var9, var13, var8, var2);
         }

         Element var15 = ProjectCustomResources.checkStrategyResources(var13);
         var2.put("xmlConfig", XMLUtil.elementToString(var13));
         var2.put("resourcesXML", XMLUtil.elementToString(var15));
      } catch (Exception var22) {
         if (var3 != null) {
            if (var4 != null) {
               var3.getProgress().update("load", 100.0, L.t("Failed to load strategy parameters from file '%s', file does not exist?!", new Object[]{var4}));
            } else {
               var3.getProgress().update("load", 100.0, L.t("Failed to load strategy parameters, file does not exist?!", new Object[0]));
            }
         }

         return apiErrorJSON(L.t("Cannot get strategy's variables.", new Object[0]), var22);
      }

      var2.put("success", L.t("Variables returned.", new Object[0]));
      return var2.toString();
   }

   private String onGetNumberOfTests(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "projectName")[0];
         String var4 = this.tryGetParam(var1, "databankName")[0];
         String var5 = this.tryGetParam(var1, "taskConfig")[0];
         Element var6 = XMLUtil.stringToElement(var5);
         long var7 = 0L;

         try {
            OptimizationSettings var9 = new OptimizationSettings(var3);
            Element var10 = var6.getChild("Optimization");
            if (var10 == null) {
               var2.put("success", L.t("Number of tests returned.", new Object[0]));
               var2.put("numberOfTests", "N/A");
               return var2.toString();
            }

            var9.setFromXML(var6, var10);
            if (!var9.singleStrategy) {
               var2.put("success", L.t("Number of tests returned.", new Object[0]));
               var2.put("numberOfTests", "N/A");
               return var2.toString();
            }

            var7 = this.getSingleStrategyCombinations(var9.strategyFile, var6);
            var2.put("settingsXML", XMLUtil.elementToString(var6));
            var2.put("numberOfTests", var7 >= 0L ? var7 : "More than 2^64");
         } catch (Exception var11) {
            Log.error("Cannot get number of tests. ", var11);
            var2.put("numberOfTests", "N/A");
         }
      } catch (Exception var12) {
         Log.error("Cannot get number of tests. ", var12);
         return apiErrorJSON(L.t("Cannot get number of tests.", new Object[0]), var12);
      }

      var2.put("success", L.t("Number of tests returned.", new Object[0]));
      return var2.toString();
   }

   private long getSingleStrategyCombinations(File var1, Element var2) throws Exception {
      ResultsGroup var3 = (ResultsGroup)Program.get("Loader").call("loadFile", new Object[]{var1.getAbsolutePath()});
      return this.getCombinationsCount(var3, var2);
   }

   private long getAllStrategiesCombinations(String var1, String var2, Element var3) throws Exception {
      Databank var4 = (Databank)ProjectEngine.get(var1).getDatabanks().get(var2);
      Element var5 = var3.getChild("Settings");
      long var6 = 0L;
      if (var4.size() == 0) {
         return 0L;
      }

      ArrayList var8 = var4.getRecordKeys();

      for (int var9 = 0; var9 < var8.size(); var9++) {
         ResultsGroup var10 = null;

         try {
            var10 = var4.getLocked((String)var8.get(var9), "OptimizationServlet");
            var6 = Math.addExact(var6, this.getCombinationsCount(var10, var5));
            if (var6 < 0L) {
               return -1L;
            }
         } catch (ArithmeticException var18) {
            return -1L;
         } catch (Exception var19) {
         } finally {
            if (var10 != null) {
               var10.releaseLock("OptimizationServlet");
            }
         }
      }

      return var6;
   }

   private long getCombinationsCount(ResultsGroup var1, Element var2) throws Exception {
      ParametersSettings var3 = new ParametersSettings();
      var3.setFromXML(var2, var1.getStrategyXml());
      return var3.combinationsCount;
   }

   private void changeSymmetry(Element var1, Element var2, boolean var3, JSONObject var4) throws Exception {
      if (var2 == null) {
         throw new Exception(L.t("Record doesn't contain any editable strategy.", new Object[0]));
      }

      ParametersSettings var5 = new ParametersSettings();
      var5.setFromXML(var1, var2);
      StrategyBase var6 = StrategyBase.createXmlStrategy(var2.clone().detach());
      var6.transformToVariables(var3 && var6.isSymmetryEnabled(), var5.paramTypes);
      Variables var7 = var6.variables();
      if (this.shouldSort(var7)) {
         var7.sortByName();
      }

      boolean var8 = false;

      try {
         var8 = var2.getChild("Strategy").getAttributeValue("pickerEditor").equals("true");
      } catch (Exception var19) {
      }

      JSONArray var9 = new JSONArray();

      for (int var10 = 0; var10 < var7.size(); var10++) {
         Variable var11 = (Variable)var7.get(var10);
         if (var11.isAutoGenerated() || var11.isMakeExternal()) {
            String var12 = var11.getId();
            String var13 = var11.getParamType();
            byte var14 = var11.getInternalType();
            String var15 = var11.getName();
            String var16 = var11.getValue();
            if (var14 != 3
               && !var15.contains("EntrySignal")
               && !var15.contains("ExitSignal")
               && !var15.contains("ReplaceExistingOrders")
               && var14 != 0
               && !var15.startsWith("Magic")
               && (
                  !var8
                     || var13 == null
                     || !var13.equals("ParamTypeTradingOptions")
                     || var12.startsWith("StockpickerOptions")
                        && !var12.contains("PickerStoreLogs")
                        && !var12.contains("PickerEntryType")
                        && !var12.contains("PickerExitType")
               )
               && (var8 || !var12.startsWith("StockpickerOptions"))) {
               JSONObject var17 = new JSONObject();
               var17.put("id", var11.getId());
               var17.put("use", true);
               var17.put("type", var11.getType());
               var17.put("name", var15);
               var17.put("paramType", var11.getParamType());
               if (var11.getInternalType() == 2) {
                  var17.put("start", "true");
                  var17.put("stop", "false");
                  var17.put("step", "");
               } else {
                  Parameter var18 = ParametersSettings.createParameter(var11, 30, 30, 5);
                  var17.put("start", var18.getStart());
                  var17.put("stop", var18.getStop());
                  var17.put("step", var18.getStep());
               }

               var17.put("value", var16);
               var9.put(var17);
            }
         }
      }

      var4.put("variables", var9);
      var4.put("symmetryEnabled", var6.isSymmetryEnabled());
   }

   private boolean shouldSort(Variables var1) {
      int var2 = 0;
      int var3 = 0;

      for (int var4 = 0; var4 < var1.size(); var4++) {
         Variable var5 = (Variable)var1.get(var4);
         if (var5.isAutoGenerated() || var5.isMakeExternal()) {
            if (var5.isAutoGenerated()) {
               var2++;
            } else {
               var3++;
            }
         }
      }

      return var3 <= var2;
   }
}
