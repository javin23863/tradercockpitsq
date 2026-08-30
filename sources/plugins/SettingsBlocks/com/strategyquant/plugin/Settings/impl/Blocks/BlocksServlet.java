package com.strategyquant.plugin.Settings.impl.Blocks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.utils.WizardConfigWorker;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.file.SQFileManager;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlocksServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(BlocksServlet.class);
   private static final ArrayList<String> signalKeys = new ArrayList<>();

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.onList();
         case "loadFromFile":
            return this.onLoadFromFile(var2);
         case "saveToFile":
            return this.onSaveToFile(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onList() throws Exception {
      JSONObject var1 = new JSONObject();

      try {
         Element var2 = WizardConfigWorker.getWizardConfig();
         signalKeys.clear();
         var1.put("orderTypes", this.getOrderTypes(var2));
         var1.put("indicators", this.getIndicators(var2));
         var1.put("priceRanges", this.getPriceRanges(var2));
         var1.put("priceValues", this.getPriceValues(var2));
         var1.put("operators", this.getOperators(var2));
         var1.put("simpleRules", this.getSimpleRules(var2));
         var1.put("others", this.getOthers(var2));
         var1.put("exitTypes", this.getExitTypes(var2));
         var1.put("formulas", this.getFormulas(var2));
         var1.put("parameterSets", this.getParameterSets(var2));
         var1.put("customBlocks", CustomBlocks.getAsBuildingBlocks());
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot list building blocks.", new Object[0]), var3);
      }

      var1.put("success", L.t("Blocks listed.", new Object[0]));
      return var1.toString();
   }

   private String onLoadFromFile(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "filePath");
         var2.put("blockSettings", SQFileManager.loadFile(var3));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot load block settings.", new Object[0]), var4);
      }

      var2.put("success", L.t("Settings loaded.", new Object[0]));
      return var2.toString();
   }

   private String onSaveToFile(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParamValue(var1, "filePath");
         String var4 = this.tryGetParamValue(var1, "fileContent");
         SQFileManager.saveFile(var3, var4);
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot save block settings.", new Object[0]), var5);
      }

      var2.put("success", L.t("Settings saved.", new Object[0]));
      return var2.toString();
   }

   private JSONArray getOrderTypes(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();

      for (Element var4 : WizardConfigWorker.getElementsFromElement(var1, "Actions", null)) {
         var2.put(WizardConfigWorker.getBlockInfo(var4, 1));
      }

      return var2;
   }

   private JSONArray getIndicators(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();

      for (Element var4 : WizardConfigWorker.getElementsFromCategory(var1, "Indicators", "indicator")) {
         var2.put(WizardConfigWorker.getBlockInfo(var4, 1));
      }

      return var2;
   }

   private JSONArray getPriceRanges(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();

      for (Element var4 : WizardConfigWorker.getElementsFromCategory(var1, "Indicators", "priceRange")) {
         var2.put(WizardConfigWorker.getBlockInfo(var4, 1));
      }

      return var2;
   }

   private JSONArray getPriceValues(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();

      for (Element var4 : WizardConfigWorker.getElementsFromCategory(var1, "Price", "priceValue")) {
         var2.put(WizardConfigWorker.getBlockInfo(var4, 1));
      }

      return var2;
   }

   private JSONArray getOperators(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();

      for (Element var4 : WizardConfigWorker.getElementsFromCategory(var1, "Comparisons", "operators")) {
         var2.put(WizardConfigWorker.getBlockInfo(var4, 1));
      }

      JSONObject var5 = new JSONObject();
      var5.put("key", "AND");
      var5.put("name", "And");
      var5.put("group", "Other");
      var5.put("weight", 1);
      var5.put("use", false);
      JSONObject var6 = new JSONObject();
      var6.put("key", "OR");
      var6.put("name", "Or");
      var6.put("group", "Other");
      var6.put("weight", 1);
      var6.put("use", false);
      var2.put(var5);
      var2.put(var6);
      return var2;
   }

   private JSONArray getSimpleRules(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();

      for (Element var4 : WizardConfigWorker.getElementsFromElement(var1, "Conditions", "simpleRules")) {
         JSONObject var5 = WizardConfigWorker.getBlockInfo(var4, 1);
         var2.put(var5);
         signalKeys.add(var5.getString("key"));
      }

      return var2;
   }

   private JSONArray getOthers(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();

      for (Element var4 : WizardConfigWorker.getElementsFromElement(var1, "Comparisons", "other")) {
         var2.put(WizardConfigWorker.getBlockInfo(var4, 1));
      }

      for (Element var7 : WizardConfigWorker.getElementsFromElement(var1, "Conditions", "other")) {
         var2.put(WizardConfigWorker.getBlockInfo(var7, 1));
      }

      for (Element var8 : WizardConfigWorker.getElementsFromElement(var1, "Values", "other")) {
         var2.put(WizardConfigWorker.getBlockInfo(var8, 1));
      }

      return var2;
   }

   private JSONArray getExitTypes(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();
      Element var3 = XMLUtil.findFirst(var1, "ExitRules");

      for (Element var5 : var3.getChildren("ExitRule")) {
         try {
            var2.put(WizardConfigWorker.getSpecialItemInfo(var5));
         } catch (Exception var7) {
            Log.error("Cannot get exit rule details. ", var7);
         }
      }

      return var2;
   }

   private JSONArray getFormulas(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();
      Element var3 = XMLUtil.findFirst(var1, "Formulas");

      for (Element var5 : var3.getChildren("Formula")) {
         try {
            var2.put(WizardConfigWorker.getSpecialItemInfo(var5));
         } catch (Exception var7) {
            Log.error("Cannot get formula details. ", var7);
         }
      }

      return var2;
   }

   private JSONArray getParameterSets(Element var1) throws Exception {
      JSONArray var2 = new JSONArray();
      Element var3 = var1.getChild("ParameterSets");
      if (var3 != null) {
         for (Element var5 : var3.getChildren("Item")) {
            JSONObject var6 = new JSONObject();
            JSONArray var7 = new JSONArray();
            String var8 = var5.getAttributeValue("key");

            for (Element var10 : var5.getChildren("Set")) {
               JSONObject var11 = new JSONObject();

               try {
                  int var12 = Integer.parseInt(var10.getAttributeValue("weight"));
                  var12 = var12 >= 0 ? var12 : (signalKeys.contains(var8) ? 10 : 1);
                  var11.put("weight", var12);
                  var11.put("values", var10.getAttributeValue("values"));
                  var7.put(var11);
               } catch (Exception var13) {
                  Log.error("Invalid parameter set settings. Block: '" + var8 + "', settings: '" + var10.getAttributeValue("values") + "'", var13);
               }
            }

            var6.put("block", var8);
            var6.put("paramSets", var7);
            var2.put(var6);
         }
      }

      return var2;
   }

   public String loadStockpickerDefaultBlocks() {
      try {
         File var1 = new File(SQPaths.simpleTemplatesDirPath + "/DefaultStockpicker.xml");
         if (!var1.exists()) {
            throw new Exception(L.t("Template file '%s' not found.", new Object[]{var1.getName()}));
         }

         Element var2 = XMLUtil.fileToXmlElement(var1);
         Element var3 = var2.getChild("Settings");
         Element var4 = var3.getChild("Blocks");
         return XMLUtil.elementToString(var4);
      } catch (Exception var5) {
         Log.error("Cannot load stockpicker default blocks.", var5);
         return new JSONObject().toString();
      }
   }
}
