package com.strategyquant.plugin.Results.impl.SourceCode;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.metatrader4.MT4Utils;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.settings.ParametrizationSettings;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;
import com.strategyquant.lib.sourcecode.XMLIndicatorsAnalyzer;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StrategySaver;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.tradinglib.strategy.xml.StrategyFixer;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceCodeServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SourceCodeServlet.class);
   private static final String TYPE_TRADESTATION = "EasyLanguage for Tradestation / MultiCharts (*.el)";
   private static final String CODE_TRADESTATION = "EasyLanguage for Tradestation (*.el)";
   private static final String CODE_MULTICHARTS = "EasyLanguage for MultiCharts (*.pla)";
   private static final String TYPE_PSEUDO_CODE = "Pseudo Code(*.TXT)";
   private static final String LOCK_SOURCESERVLET = "SourceCodeServlet";
   private static IResultsGroupProvider rgProvider;

   public SourceCodeServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "print":
            return this.onPrint(var2);
         case "getDataPath":
            return this.onGetDataPath(var2);
         case "saveMTPaths":
            return this.onSaveMTPaths(var2);
         case "saveEA":
            return this.onSaveEA(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   public JSONArray onListMM() {
      JSONArray var1 = new JSONArray();
      JSONObject var2 = new JSONObject();
      var2.put("name", "From strategy");
      var2.put("value", "fromStrategy");
      var1.put(var2);

      for (MoneyManagementMethod var4 : MoneyManagementMethodsList.get().getAvailableClasses()) {
         var2 = new JSONObject();
         var2.put("name", var4.getName());
         var2.put("value", var4.getClass().getSimpleName());
         var1.put(var2);
      }

      return var1;
   }

   public synchronized JSONArray onList() throws Exception {
      JSONArray var1 = new JSONArray();
      List var2 = SourceCodeGenerators.getInstance().getAvailableGenerators();
      JSONObject var3 = new JSONObject();
      var3.put("name", "EasyLanguage for Tradestation / MultiCharts (*.el)");
      var3.put("key", "EasyLanguage");
      JSONObject var4 = new JSONObject();
      var4.put("name", "EasyLanguage for Tradestation (*.el)");
      var4.put("types", "el");
      JSONObject var5 = new JSONObject();
      var5.put("name", "EasyLanguage for MultiCharts (*.pla)");
      var5.put("types", "pla");
      var3.put("extension", new JSONArray().put(var4).put(var5));
      var3.put("desc", L.tsq("Saves Strategy EasyLanguage code."));
      var1.put(var3);

      for (SourceCodeGenerator var7 : var2) {
         JSONObject var8 = new JSONObject();
         String var9 = var7.getName();
         if (!var9.contains("EasyLanguage")) {
            var8.put("name", var9);
            var8.put("key", var7.getKey());
            String[] var10 = var7.getFileExtension();
            JSONObject var11 = new JSONObject();
            var11.put("name", var10[0]);
            var11.put("types", var10[1]);
            var8.put("extension", var11);
            var8.put("desc", var7.getDescription());
            var1.put(var8);
         }
      }

      if (!"BACKTESTNODE".equals(MainApp.getProduct())) {
         JSONObject var12 = new JSONObject();
         var12.put("name", "Strategy XML");
         JSONObject var13 = new JSONObject();
         var13.put("name", "XML Document");
         var13.put("types", "xml");
         var12.put("extension", var13);
         var12.put("desc", L.tsq("Saves Strategy XML code."));
         var1.put(var12);
      }

      return var1;
   }

   private String onPrint(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var4 = null;
      String var5 = null;

      String var3;
      try {
         String[] var6 = this.getSourceCode(var1, false);
         var3 = var6[0];
         var4 = var6[1];
         var5 = var6[2];
      } catch (RecordNotFoundException var7) {
         Log.info("onPrint: " + var7.getMessage());
         return apiErrorJSONNoLog(null, var7);
      } catch (Exception var8) {
         Log.error("Exc.", var8);
         var3 = var8.getMessage();
      }

      var2.put("code", var3);
      var2.put("warning", var4);
      var2.put("learnMoreURL", var5);
      var2.put("success", "ok");
      return var2.toString();
   }

   private String[] getSourceCode(Map<String, String[]> var1, boolean var2) throws Exception {
      this.checkParamExists(var1, new String[]{"type", "symmetricVariables", "mmType"});
      String var3 = ((String[])var1.get("type"))[0].toString();
      if (var3.equals("EasyLanguage for Tradestation (*.el)") || var3.equals("EasyLanguage for MultiCharts (*.pla)")) {
         var3 = "EasyLanguage for Tradestation / MultiCharts (*.el)";
      }

      if (!var2) {
         AbstractBroker var4 = MainApp.v571hfnsHw().inygRmSMx7();
         if (var4 != null && var4.usesEAEncryption() && !var3.equals("Pseudo Code(*.TXT)")) {
            throw new Exception(L.t("Option not supported for current broker", new Object[0]));
         }
      }

      String var39 = null;
      String var5 = null;
      Map var6 = this.getParameters(null, var1);
      Map var7 = this.getRgParams(var6);
      double var8 = 1.0;
      double var10 = 0.0;
      Element var12 = this.getStrategyElement(var1);
      ResultsGroup var13 = null;

      try {
         var13 = rgProvider.get(var7, "SourceCodeServlet");
      } catch (Exception var36) {
         Log.debug("Unable to load ResultsGroup from URL params (It is ok for AW template)", var36);
      }

      if (var13 != null && !var3.equals("Strategy XML") && !var3.equals("Pseudo Code(*.TXT)")) {
         try {
            String var14 = var13.getLastSettings();
            if (var14 != null) {
               Element var15 = XMLUtil.stringToElement(var14);
               Element var16 = var15.getChild("Data").getChild("Setups").getChild("Setup");
               String var17 = var16.getAttributeValue("engine");
               if (var17.contains("(")) {
                  var17 = var17.substring(0, var17.indexOf("(") + 1);
               }

               if (!var3.contains(var17)) {
                  var39 = L.t(
                     "Note - this strategy was backtested with %s engine, but you are showing code for %s. Make sure you retest it with this engine before trading in your platform.",
                     new Object[]{var17, var3}
                  );
               }

               Element var18 = XMLUtil.getChildElem(var16, "Chart");
               String var19 = var18.getAttributeValue("symbol");
               InstrumentInfo var20 = DataManager.getDataInfo("History", var19).symbolInfo;
               var8 = var20.orderSizeMultiplier;
               var10 = var20.orderSizeStep;
            }
         } catch (Exception var35) {
            Log.error("Exc.", var35);
         }
      }

      String var42 = null;

      try {
         var42 = ((String[])var1.get("strategy"))[0].toString();
      } catch (Exception var34) {
         var42 = null;
      }

      try {
         boolean var44 = true;
         boolean var46 = Boolean.parseBoolean(((String[])var1.get("symmetricVariables"))[0].toString());
         String var48 = ((String[])var1.get("mmType"))[0].toString();
         ValuesMap var49 = this.getParametrizationSettings(var1);
         MainApp.settings().set("sourceCodeType", var3);
         MainApp.settings().set("sourceCodeVariables", var44 + "");
         MainApp.settings().set("sourceCodeSymmetry", var46 + "");
         String var50 = "NA";
         if (var12 == null) {
            throw new Exception(L.t("Strategy doesn't contain XML source code.", new Object[0]));
         }

         try {
            if (!var3.equals("Strategy XML") && !var3.equals("Pseudo Code(*.TXT)")) {
               String var51 = var12.getChild("Strategy").getAttributeValue("pickerEditor");
               if (var51 != null && var51.equals("true")) {
                  if ("BACKTESTNODE".equals(MainApp.getProduct())) {
                     var50 = L.t("Stockpicker strategies are not supported for this platform.", new Object[0])
                        + "\n\n"
                        + L.t("You can trade it by saving to My strategies and deploying to Live/Paper account from there", new Object[0]);
                  } else {
                     var50 = L.t("Stockpicker strategies are not supported for this platform.", new Object[0])
                        + "\n\n"
                        + L.t(
                           "You can see its Pseudo code, but live trading will be supported only in our new cloud trading platform https://algocloud.com",
                           new Object[0]
                        );
                  }

                  return new String[]{var50, null, null};
               }

               if (StrategyFixer.isTemplate(var12)) {
                  var50 = L.t("This is a template of a strategy, not a complete strategy. It is visible only in Pseudo code.", new Object[0]);
                  return new String[]{var50, null, null};
               }
            }
         } catch (Exception var33) {
            Log.error("Exc.", var33);
         }

         boolean var52 = this.recognizeMergedResult(var12);
         StrategyBase var21 = StrategyBase.createXmlStrategy(var12);
         if (var44) {
            var21.transformToVariables(var46, var49);
            var21.variables().sortByName();
         } else {
            var21.transformToNumbers();
         }

         if (var42 == null) {
            try {
               var42 = var12.getChild("options").getChild("StrategyName").getValue();
            } catch (Exception var32) {
               var42 = "AlgoWizard strategy";
            }
         }

         String var22 = var13 != null ? var13.getLastSettings() : null;
         var12 = StrategyXMLModifier.getUpdatedStrategyXML(var13, var12, var48, var13 != null ? var13.getName() : var42, var22, null);
         if (StrategySaver.isMetaTraderGenerator(var3)) {
            StrategySaver.normalizeExportSymbols(var12);
         }

         XMLIndicatorsAnalyzer.init();
         XMLIndicatorsAnalyzer.fixItemAttributes(var12);
         if (var3.equals("Strategy XML")) {
            CustomDataManager.addCDataIndySourceCodes(var12);
            CustomBlocks.addCBlockSourceCodes(var12);
            var50 = XMLUtil.elementToString(var12);
         } else {
            SourceCodeGenerator var23 = SourceCodeGenerators.getInstance().getGeneratorFromName(var3);
            if (var23 != null) {
               Log.debug("Generating {}", var23.getName());
               CustomDataManager.addCDataIndySourceCodes(var12);
               CustomBlocks.addCBlockSourceCodes(var12);
               if (this.checkMMAllowed(var12, var3)) {
                  var50 = var23.getSource(var42, var12, var8, var10);
                  if (var3.equals("EasyLanguage for Tradestation / MultiCharts (*.el)") && this.containsDuplicateEntries(var12)) {
                     var39 = L.t(
                        "Note - Tradestation/MultiCharts doesn't allow independent handling of multiple orders to the same direction, exits of these orders will be not applied correctly!",
                        new Object[0]
                     );
                     var5 = "https://strategyquant.com/doc/strategyquant/multi-orders-to-same-direction";
                  }
               } else {
                  var50 = L.t("Unable to generate source code - Selected money management method is allowed for Multicharts engine only", new Object[0]);
               }
            }
         }

         if (var52) {
            var39 = L.t(
               "Note - this is a merged portfolio. The strategy code here is the code of the very first startegy in portfolio. Merge functionality in SQ does not combine source codes of all strategies to one!",
               new Object[0]
            );
         }

         return new String[]{var50, var39, var5};
      } catch (Exception var37) {
         String var45 = var37.getMessage();
         if (var45.contains("Template not found for name")) {
            String var47 = this.fixMissingBlockText(var45, var3);
            var39 = String.format(L.t("Error! One or more blocks the strategy uses isn't implemented in %s.", new Object[0]), this.getCodeType(var3));
            return new String[]{var47, var39, var5};
         } else {
            Log.error("Exc.", var37);
            throw new Exception(L.t("An error occured while generating the strategy XML source code.\nExc. ", new Object[0]) + var45);
         }
      } finally {
         if (var13 != null) {
            var13.releaseLock("SourceCodeServlet");
         }
      }
   }

   private ArrayList<String> getInputsToVarsFromLadder(ResultsGroup var1) {
      ArrayList var2 = new ArrayList();
      WalkForwardMatrixResult var3 = null;

      try {
         var3 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
         if (var3 != null) {
            WalkForwardResult var4 = (WalkForwardResult)var3.getResultList().get(0);
            ArrayList var5 = var4.wfPeriods;
            if (var5.size() > 0) {
               WalkForwardPeriod var6 = (WalkForwardPeriod)var5.get(0);
               String[] var7 = var6.testParameters.split(",");

               for (String var11 : var7) {
                  var2.add(var11);
               }
            }
         }
      } catch (Exception var12) {
         Log.info("Cannot create Data ladder", var12);
      }

      return var2;
   }

   private void createLadderEntry(StringBuilder var1, WalkForwardPeriod var2) {
      int var3 = SQTime.getDay(var2.runFrom);
      int var4 = SQTime.getMonth(var2.runFrom) + 1;
      int var5 = SQTime.getYear(var2.runFrom) + 1900;
      int var6 = SQTime.getDay(var2.runTo);
      int var7 = SQTime.getMonth(var2.runTo) + 1;
      int var8 = SQTime.getYear(var2.runTo) + 1900;
      String var9 = String.format("if date >= ELDate(%d, %d, %d)  and date <= ELDate(%d, %d, %d) then begin\n", var4, var3, var5, var7, var6, var8);
      var1.append(var9);
      String[] var10 = var2.testParameters.split(",");

      for (String var14 : var10) {
         var1.append("        ");
         var1.append(var14);
         var1.append(";\n");
      }

      var1.append("end;\n");
   }

   private Element getStrategyElement(Map<String, String[]> var1) throws Exception {
      String var2 = null;

      try {
         var2 = ((String[])var1.get("strategyXML"))[0].toString();
      } catch (Exception var13) {
      }

      Map var3 = this.getParameters(null, var1);
      Map var4 = this.getRgParams(var3);
      Element var5 = null;
      ResultsGroup var6 = null;

      try {
         try {
            var6 = rgProvider.get(var4, "SourceCodeServlet");
         } catch (Exception var12) {
            Log.debug("Unable to load ResultsGroup from URL params (It is ok for AW template)", var12);
         }

         if (var2 != null && !var2.isEmpty()) {
            var5 = XMLUtil.stringToElement(var2);
         } else if (var6 != null) {
            var5 = var6.portfolio().getStrategyXml();
            if (var5 != null) {
               var5 = var5.clone();
            } else {
               Log.error("Cannot get strategy XML from ResultsGroup - getStrategyXml() returned null");
            }
         }
      } finally {
         if (var6 != null) {
            var6.releaseLock("SourceCodeServlet");
         }
      }

      return var5;
   }

   private boolean checkMMAllowed(Element var1, String var2) throws Exception {
      if (!var2.equals("EasyLanguage for Tradestation / MultiCharts (*.el)") && !var2.equals("Pseudo Code(*.TXT)")) {
         Element var3 = var1.getChild("Strategy").getChild("MoneyManagement");
         String var4 = var3.getAttributeValue("type");
         return !var4.equals("CryptoFixedAmount") && !var4.equals("CryptoFixedBalancePct");
      } else {
         return true;
      }
   }

   private String getCodeType(String var1) {
      switch (var1) {
         case "EasyLanguage for Tradestation / MultiCharts (*.el)":
            return "EasyLanguage";
         case "Pseudo Code(*.TXT)":
            return "PseudoCode";
         default:
            return "MQL code";
      }
   }

   private String fixMissingBlockText(String var1, String var2) {
      String var3 = L.t("Template inclusion failed (for parameter value \"./blocks/", new Object[0]);
      int var4 = var1.indexOf(var3);
      if (var4 < 0) {
         return var1;
      }

      int var5 = var1.indexOf(".tpl\"):", var4);
      if (var5 <= 0) {
         return var1;
      }

      String var6 = var1.substring(var4 + var3.length(), var5);
      StringBuilder var7 = new StringBuilder("\n");
      var7.append(L.t("This strategy contains block '%s' that is not implemented in %s.\n\n", new Object[]{var6, var2}));
      var7.append(
         L.t(
            "Every trading engine has different features and possibilities, and there are certain blocks that are specific to a given trading engine, and don't work on another.\n",
            new Object[0]
         )
      );
      var7.append(L.t("If you want to use this strategy as %s you have to alter this strategy so it doesn't use this block.\n\n", new Object[]{var2}));
      var7.append(
         L.t(
            "If this is a custom block created by you make sure you also added its template to /extend/Code for trading platform(s) you want to support.\n\n",
            new Object[0]
         )
      );
      var7.append("\n\n---------------------------- Original exception (for coders) is below ------------------------------\n\n");
      var7.append(var1);
      return var7.toString();
   }

   private boolean recognizeMergedResult(Element var1) {
      String var2 = var1.getAttributeValue("mergedResult");
      return var2 != null && var2.equals("true");
   }

   private ValuesMap getParametrizationSettings(Map<String, String[]> var1) {
      String var2 = ((String[])var1.get("type"))[0].toString();
      boolean var3 = this.getBool(var1, "periodParams");
      boolean var4 = this.getBool(var1, "constantsParams");
      boolean var5 = this.getBool(var1, "shiftParams");
      boolean var6 = this.getBool(var1, "otherParams");
      boolean var7 = this.getBool(var1, "entryParams");
      boolean var8 = this.getBool(var1, "entryLogic");
      boolean var9 = this.getBool(var1, "exitParamsUsed");
      boolean var10 = this.getBool(var1, "exitParamsUnused");
      boolean var11 = this.getBool(var1, "booleanParams");
      boolean var12 = this.getBool(var1, "symmetricVariables");
      boolean var13 = this.getBool(var1, "recommendedParams");
      JSONObject var14 = new JSONObject();
      var14.put("periodParams", var3);
      var14.put("constantsParams", var4);
      var14.put("shiftParams", var5);
      var14.put("otherParams", var6);
      var14.put("entryParams", var7);
      var14.put("entryLogic", var8);
      var14.put("exitParamsUsed", var9);
      var14.put("exitParamsUnused", var10);
      var14.put("booleanParams", var11);
      var14.put("symmetricVariables", var12);
      var14.put("recommendedParams", var13);
      MainApp.settings().set(ParametrizationSettings.getSettingKey(var2), var14.toString());
      ValuesMap var15 = new ValuesMap();
      var15.set("ParamTypePeriod", var3);
      var15.set("ParamTypeShift", var5);
      var15.set("ParamTypeExitUsed", var9);
      var15.set("ParamTypeConstant", var4);
      var15.set("ParamTypeOtherParam", var6);
      var15.set("ParamTypeEntryLevel", var7);
      var15.set("ParamTypeEntryLogic", var8);
      var15.set("ParamTypeExitUnused", var10);
      var15.set("ParamTypeBoolean", var11);
      var15.set("ParamTypeRecommended", var13);
      return var15;
   }

   private boolean getBool(Map<String, String[]> var1, String var2) {
      String[] var3 = (String[])var1.get(var2);
      return var3 == null ? false : Boolean.parseBoolean(var3[0]);
   }

   private String onGetDataPath(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = this.tryGetParamValue(var1, "installPath");
      boolean var4 = Boolean.parseBoolean(this.tryGetParamValue(var1, "isMT4"));
      File var5 = MT4Utils.getDataFolderFile();
      String var6 = var5.getAbsolutePath() + "\\" + MT4Utils.getTerminalHash(var3) + (var4 ? "/MQL4/Experts" : "/MQL5/Experts");
      var2.put("dataPath", var6);
      var2.put("success", L.t("Paths saved", new Object[0]));
      return var2.toString();
   }

   private String onSaveMTPaths(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      if (var1.get("mt4InstallPath") != null) {
         String var3 = this.tryGetParamValue(var1, "mt4InstallPath");
         MainApp.settings().set("mt4InstallPath", var3);
      }

      if (var1.get("mt5InstallPath") != null) {
         String var4 = this.tryGetParamValue(var1, "mt5InstallPath");
         MainApp.settings().set("mt5InstallPath", var4);
      }

      if (var1.get("mt4DataPath") != null) {
         String var5 = this.tryGetParamValue(var1, "mt4DataPath");
         MainApp.settings().set("mt4DataPath", var5);
      }

      if (var1.get("mt5DataPath") != null) {
         String var6 = this.tryGetParamValue(var1, "mt5DataPath");
         MainApp.settings().set("mt5DataPath", var6);
      }

      MainApp.settings().save();
      var2.put("success", L.t("Paths saved", new Object[0]));
      return var2.toString();
   }

   private String onSaveEA(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      Element var3 = this.getStrategyElement(var1);
      if (var3 == null) {
         throw new Exception(L.t("Strategy doesn't have any content", new Object[0]));
      }

      String var4 = var3.getChild("Strategy").getAttributeValue("pickerEditor");
      if (var4 != null && var4.equals("true")) {
         throw new Exception(
            L.t("Stockpicker strategies are not supported for MetraTrader platform.", new Object[0])
               + "\n"
               + L.t("You can trade stockpicker strategies live at <a target='_blank' href='https://algowizard.io'>algowizard.io</a>", new Object[0])
         );
      }

      boolean var5 = Boolean.parseBoolean(this.tryGetParamValue(var1, "isMT4"));
      String var6 = var5 ? MainApp.settings().get("mt4InstallPath") : MainApp.settings().get("mt5InstallPath");
      String var7 = var6 + "/metaeditor.exe";
      if (!new File(var7).exists()) {
         var7 = var6 + "/metaeditor64.exe";
         if (!new File(var7).exists()) {
            return apiErrorJSON(L.t("Cannot find metaeditor.exe. Please check your MetaTrader installation path", new Object[0]), null);
         }
      }

      String var8 = var5 ? MainApp.settings().get("mt4DataPath") : MainApp.settings().get("mt5DataPath");
      if (!new File(var8).exists()) {
         return apiErrorJSON(L.t("MetaTrader data folder doesn't exist. Please check your settings", new Object[0]), null);
      }

      Object var9 = null;

      try {
         var9 = ((String[])var1.get("strategy"))[0].toString();
      } catch (Exception var21) {
         var9 = "AlgoWizardStrategy";
      }

      String var10 = var8 + "/" + var9 + (var5 ? ".mq4" : ".mq5");
      File var11 = new File(var10);
      String var12 = var8 + "/" + var9 + (var5 ? ".ex4" : ".ex5");
      var1.put("type", new String[]{var5 ? SourceCodeGenerator.MetaTrader4 : SourceCodeGenerator.MetaTrader5});
      String var13 = this.getSourceCode(var1, true)[0];
      String var14 = this.tryGetParam(var1, "project")[0];
      if (var14.equals("AlgoWizard")) {
         var14 = "Retester";
      }

      SQProject var15 = ProjectEngine.get(var14);

      try {
         SQUtils.stringToFile(var11, var13);
         var15.getProgress().update("saveEA", 20.0, null);
         String var16 = var7 + " /compile:\"" + var10.replace("/", "\\") + "\" /log";
         Log.debug("---- Running command: " + var16);
         Process var17 = Runtime.getRuntime().exec(var16);
         if (!var17.waitFor(20L, TimeUnit.SECONDS)) {
            Log.error("Command '" + var16 + "' failed!");
            var11.delete();
            String var23 = L.t("MQL compilation timed out", new Object[0]);
            var15.getProgress().update("saveEA", 100.0, var23);
            return apiErrorJSON(var23, null);
         } else {
            var11.delete();
            File var18 = new File(var12);
            if (!var18.exists()) {
               String var19 = L.t("EA was not created. Compilation failed", new Object[0]);
               var15.getProgress().update("saveEA", 100.0, var19);
               return apiErrorJSON(var19, null);
            } else {
               var15.getProgress().update("saveEA", 100.0, null);
               Desktop.getDesktop().open(var18.getParentFile());
               var2.put("success", L.t("EA saved", new Object[0]));
               return var2.toString();
            }
         }
      } catch (Exception var20) {
         var15.getProgress().update("saveEA", 100.0, var20.getMessage());
         throw var20;
      }
   }

   private boolean containsDuplicateEntries(Element var1) {
      try {
         ArrayList var2 = XMLUtil.getNestedElements(var1, "Item");
         int var3 = 0;
         int var4 = 0;

         for (int var5 = 0; var5 < var2.size(); var5++) {
            Element var6 = (Element)var2.get(var5);
            String var7 = var6.getAttributeValue("key");
            if (var7 != null && var7.startsWith("EnterAt")) {
               Element var8 = XMLUtil.getItemParameterNoException(var6, "#Direction#");
               if (var8 != null) {
                  try {
                     int var9 = Integer.parseInt(var8.getText());
                     if (var9 == 1) {
                        var3++;
                     } else if (var9 == -1) {
                        var4++;
                     }
                  } catch (Exception var10) {
                  }
               }

               if (var3 > 1 || var4 > 1) {
                  return true;
               }
            }
         }
      } catch (Exception var11) {
         Log.error("Error while checking duplicate entries", var11);
      }

      return false;
   }
}
