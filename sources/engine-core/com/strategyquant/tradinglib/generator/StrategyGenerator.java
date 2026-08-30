package com.strategyquant.tradinglib.generator;

import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.customData.CustomDataTypes;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.annotations.Value;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.blocks.random.BlockParameter;
import com.strategyquant.tradinglib.blocks.random.BlocksConfig;
import com.strategyquant.tradinglib.blocks.random.ItemRandomValueRanges;
import com.strategyquant.tradinglib.blocks.random.RandomValueConfig;
import com.strategyquant.tradinglib.blocks.random.ReplacementConfig;
import com.strategyquant.tradinglib.blocks.random.ReplacementParameter;
import com.strategyquant.tradinglib.blocks.random.ReplacementsConfig;
import com.strategyquant.tradinglib.gp.strategies.FixNumberOfExitTypes;
import com.strategyquant.tradinglib.gp.strategies.FixStockpickerBlocks;
import com.strategyquant.tradinglib.gp.strategies.FixUnusedDependentFormulas;
import com.strategyquant.tradinglib.strategy.xml.StrategyFixer;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyGenerator extends GeneratorBase {
   public static final Logger Log = LoggerFactory.getLogger("StrategyGenerator");
   private BlocksConfig blocksConfig;
   private boolean free;
   private String architecture;
   private int fuzzyMinTrue;
   private int fuzzyMaxTrue;
   private ATMGenerateConfig atmGenerateConfig;
   private FixNumberOfExitTypes fixNumberOfExitTypes;

   StrategyGenerator(Element var1, Element var2, IRandomGenerator var3, SettingsMap var4, ATMGenerateConfig var5) throws JDOMException, IOException, Exception {
      this.elTemplate = var1;
      this.rng = var3;
      this.free = true;
      this.addTemplateVariablesToBuildSettings(var4);
      this.blocksConfig = BlocksConfig.getConfig();
      this.replacements = new ReplacementsConfig(var2, this.blocksConfig, this.rng, var4);
      this.atmGenerateConfig = var5;
      this.architecture = var4.getString("Architecture");
      this.fuzzyMinTrue = var4.getInt("FuzzyMinTrue", 60);
      this.fuzzyMaxTrue = var4.getInt("FuzzyMaxTrue", 80);
      this.fuzzyMaxTrue = var4.getInt("FuzzyMaxTrue", 80);
      this.fuzzyMaxTrue = var4.getInt("FuzzyMaxTrue", 80);
      this.fuzzyMaxTrue = var4.getInt("FuzzyMaxTrue", 80);
      this.fuzzyMaxTrue = var4.getInt("FuzzyMaxTrue", 80);
      this.fixNumberOfExitTypes = new FixNumberOfExitTypes(var4, var2);
      this.checkConfig();
   }

   private void checkConfig() throws GenerateException {
      Element var1 = this.elTemplate;
      HashMap var2 = new HashMap();
      checkUniqueRandomIdentifiers(var1, var2);
      ArrayList var3 = new ArrayList();
      this.getIdentifiers(var1, var3, var2, true);
      this.replacements.checkAllExist(var3);
      ArrayList var4 = new ArrayList();
      this.getIdentifiers(var1, var4, var2, false);

      for (String var6 : var4) {
         if (!SQUtils.isInStringArray(var6, var3)) {
            throw new GenerateException(
               String.format("Bad configuration - identifier %s referenced in Same/Opposite block or parameter, but it doesn't exist!", var6)
            );
         }
      }
   }

   public Element generate(String var1) throws GenerateException {
      this.generatedElements = new HashMap<>();
      Element var2 = this.elTemplate.clone();
      this.modifyFuzzyPercentage(var2);
      StrategyWithVariables var3 = new StrategyWithVariables(var2);
      this.findAndReplaceAndOrRndBlocks(var2);
      this.findAndReplaceRandomBlocks(var2, 1);
      this.findAndReplaceRandomParameters(var2, 1, 0);
      this.findAndReplaceSameBlocks(var2);
      this.findAndReplaceNegatedBlocks(var2, var3);
      this.fillGeneratedElements(var2);
      this.findAndReplaceSameOrOppositeParameters(var2, var3);
      this.deleteMarkedRules(var2);
      this.deleteMarkedExits(var2);
      this.fixBlocksAndParams(var2);
      this.fixFuzzySignalRuleConditions(var2);
      FixUnusedDependentFormulas.fix(var2);
      fixValuesInCDataIndy(var2);
      this.fixIsFormula(var2);
      this.addCustomBlocksData(var2, var3);
      StrategyFixer.setCurrentVersion(var2);
      StrategyFixer.removeAllowRandom(var2);
      StrategyFixer.removeRandomGenerateAttributes(var2);
      this.generateATM(var2);
      FixStockpickerBlocks.fixSPStrategy(var2, this.blocksConfig);
      boolean var4 = this.fixNumberOfExitTypes.fixStrategy(var2);
      if (var4) {
      }

      return var2;
   }

   private void fixIsFormula(Element var1) {
      if (var1.getName().equals("Param")) {
         Element var2 = var1.getChild("Formula");
         if (var2 != null) {
            var1.setAttribute("isFormula", "true");
         }
      }

      List var5 = var1.getChildren();
      if (var5 != null && var5.size() > 0) {
         for (int var3 = 0; var3 < var5.size(); var3++) {
            Element var4 = (Element)var5.get(var3);
            this.fixIsFormula(var4);
         }
      }
   }

   private void generateATM(Element var1) throws GenerateException {
      if (this.atmGenerateConfig != null) {
         if (!MainApp.v571hfnsHw().aDm88fRJB2()) {
            Log.info("ATM generation functionality is available only in Ultimate license!");
         } else {
            Element var2 = this.atmGenerateConfig.generate(this.rng);
            var1.addContent(var2);
         }
      }
   }

   public Element mutateATM(Element var1, IRandomGenerator var2) throws GenerateException {
      if (this.atmGenerateConfig == null) {
         return null;
      } else if (!MainApp.v571hfnsHw().aDm88fRJB2()) {
         Log.info("ATM generation functionality is available only in Ultimate license!");
         return null;
      } else {
         Element var3 = this.atmGenerateConfig.generate(var2);
         this.randomlyReplacePartOfATM(var1, var3);
         return var1;
      }
   }

   private void randomlyReplacePartOfATM(Element var1, Element var2) {
      var1.removeChild("ATMs");
      var1.addContent(var2);
   }

   private void addCustomBlocksData(Element var1, StrategyWithVariables var2) throws GenerateException {
      Element var3 = var1.getChild("Strategy");
      Element var4 = var3.getChild("CustomBlocks");
      if (var4 == null) {
         var4 = new Element("CustomBlocks");
         var4.setAttribute("genby", "SG");
         var3.addContent(var4);
      } else {
         var4.setAttribute("genby", "Exist");
      }

      HashMap var5 = new HashMap();
      this.fillCBMap(var3, var5);
      addBlocksToXML(var4, var5);
   }

   private void fillCBMap(Element var1, HashMap<String, Element> var2) throws GenerateException {
      if (var1.getName().equals("Item")) {
         String var3 = var1.getAttributeValue("key");
         if (var3 != null && var3.startsWith("CBlock")) {
            String var4 = var3;
            if (var4 != null) {
               Element var5 = CustomBlocks.getElement(var4);
               if (var5 == null) {
                  throw new GenerateException("Cannot find definition for Custom Block '" + var4 + "'");
               }

               var2.put(var4, var5);
            }
         }
      }

      List var6 = var1.getChildren();
      if (var6 != null && var6.size() > 0) {
         for (int var7 = 0; var7 < var6.size(); var7++) {
            Element var8 = (Element)var6.get(var7);
            this.fillCBMap(var8, var2);
         }
      }
   }

   public static void addBlocksToXML(Element var0, HashMap<String, Element> var1) {
      if (var1 != null) {
         for (Element var3 : var1.values()) {
            var0.addContent(var3.clone());
         }
      }
   }

   public static void fixValuesInCDataIndy(Element var0) throws GenerateException {
      if (var0.getName().equals("Item")) {
         String var1 = var0.getAttributeValue("key");
         if (var1 != null && var1.startsWith("CDataIndy")) {
            String var2 = CustomDataManager.getCDataIndyId(var1);
            if (var2 != null) {
               fixCIndyValues(var0, var2);
            }
         }
      }

      List var4 = var0.getChildren();
      if (var4 != null && var4.size() > 0) {
         for (int var5 = 0; var5 < var4.size(); var5++) {
            Element var3 = (Element)var4.get(var5);
            fixValuesInCDataIndy(var3);
         }
      }
   }

   private static void fixCIndyValues(Element var0, String var1) throws GenerateException {
      CustomDataInfo var2 = CustomDataManager.getDataInfo(var1);
      if (var2 == null) {
         Log.info("Cannot find custom data indicator '{}', ignoring!", var1);
      } else {
         try {
            var0.setAttribute("returnType", CustomDataTypes.translateDataType((byte)var2.dataType));
            if (var2.dataType == 10) {
               var0.setAttribute("categoryType", "simpleRules");
            } else {
               var0.setAttribute("categoryType", "indicator");
            }
         } catch (Exception var6) {
            throw new GenerateException("Error setting custom indocator return type! DataType=" + var2.dataType, var6);
         }

         Element var3 = XMLUtil.findFirstWithKey(var0, "Param", "#Value#");
         if (var3 != null) {
            try {
               String var4 = CustomDataManager.createCDataValues(var2.values, var2);
               var3.setAttribute("values", var4);
            } catch (Exception var5) {
               Log.error("Cannot add values to custom data indy", var5);
            }
         }
      }
   }

   private void modifyFuzzyPercentage(Element var1) {
      if (this.architecture != null && this.architecture.equals("sq4fuzzy")) {
         StrategyStandardTemplateSQ4Fuzzy.ramdomizeFuzzyPercentags(var1, this.fuzzyMinTrue, this.fuzzyMaxTrue, this.rng);
      }
   }

   private void fixFuzzySignalRuleConditions(Element var1) {
      Element var2 = var1.getChild("Strategy").getChild("Rules").getChild("Events");
      List var3 = var2.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         List var6 = var5.getChildren();

         for (int var7 = 0; var7 < var6.size(); var7++) {
            Element var8 = (Element)var6.get(var7);
            if (var8.getAttributeValue("type").equals("SignalFuzzy")) {
               this.fixFuzzySignals(var8);
            }
         }
      }
   }

   private void fixFuzzySignals(Element var1) {
      Element var2 = var1.getChild("signals");
      List var3 = var2.getChildren("signal");
      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Element var6 = (Element)var3.get(var5);
         var4.add(this.fixFuzzySignal(var6));
      }

      var2.removeChildren("signal");

      for (int var7 = 0; var7 < var4.size(); var7++) {
         var2.addContent((Content)var4.get(var7));
      }
   }

   private Element fixFuzzySignal(Element var1) {
      Element var2 = new Element("signal");
      var2.setAttribute("variable", var1.getAttributeValue("variable"));
      var2.setAttribute("minTrue", var1.getAttributeValue("minTrue"));
      ArrayList var3 = new ArrayList();
      this.getSignalRealConditionsList(var1, var3);

      for (int var4 = 0; var4 < var3.size(); var4++) {
         var2.addContent(((Element)var3.get(var4)).detach());
      }

      return var2;
   }

   private void getSignalRealConditionsList(Element var1, List<Element> var2) {
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (this.isRealCondition(var5)) {
            var2.add(var5);
         } else {
            List var6 = var5.getChildren();
            if (var6 != null) {
               this.getSignalRealConditionsList(var5, var2);
            }
         }
      }
   }

   private boolean isRealCondition(Element var1) {
      if (!var1.getName().equals("Item")) {
         return false;
      }

      String var2 = var1.getAttributeValue("key");
      return !var2.equals("AND") && !var2.equals("OR");
   }

   private void deleteMarkedRules(Element var1) {
      Element var2 = var1.getChild("Strategy").getChild("Rules").getChild("Events");
      List var3 = var2.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         List var6 = var5.getChildren();
         Iterator var7 = var6.iterator();

         while (var7.hasNext()) {
            Element var8 = (Element)var7.next();
            if (var8.getAttributeValue("type").contains("Signal")) {
               this.replaceDeleteWithFalse(var8);
            } else if (this.shouldDeleteThisRule(var8, false)) {
               var7.remove();
            }
         }
      }
   }

   private void deleteMarkedExits(Element var1) {
      Element var2 = var1.getChild("Strategy");
      String var3 = var2.getAttributeValue("engine");
      if (var3 == null || var3.equals("Stockpicker") || var3.equals("Single-asset cloud strategy")) {
         Element var4 = var1.getChild("Strategy").getChild("Rules").getChild("Events");
         List var5 = var4.getChildren();

         for (int var6 = 0; var6 < var5.size(); var6++) {
            Element var7 = (Element)var5.get(var6);
            this.deleteMarkedExitElements(var7);
         }
      }
   }

   private void deleteMarkedExitElements(Element var1) {
      List var2 = var1.getChildren();
      Iterator var3 = var2.iterator();

      while (var3.hasNext()) {
         Element var4 = (Element)var3.next();
         if (var4.getName().equals("Exit")) {
            if (this.shouldDeleteThisRule(var4, true)) {
               var3.remove();
            }
         } else {
            this.deleteMarkedExitElements(var4);
         }
      }
   }

   private void replaceDeleteWithFalse(Element var1) {
      List var2 = var1.getChildren();
      if (var2 != null) {
         for (Element var4 : var2) {
            if (var4.getName().equals("DeleteRule")) {
               var4.setName("Item");
               var4.setAttribute("key", "Boolean");
               var4.setAttribute("name", "(BOOL) Boolean");
               var4.setAttribute("display", "#Value#");
               var4.setAttribute("returnType", "number");
               var4.setAttribute("categoryType", "other");
               var4.setAttribute("notFirstValue", "true");
               Element var5 = new Element("Param");
               var4.addContent(var5);
               var5.setAttribute("key", "#Value#");
               var5.setAttribute("name", "Value");
               var5.setAttribute("type", "boolean");
               var5.setAttribute("defaultValue", "false");
               var5.setAttribute("controlType", "booleanVar");
               var5.setText("false");
               return;
            }

            this.replaceDeleteWithFalse(var4);
         }
      }
   }

   private boolean shouldDeleteThisRule(Element var1, boolean var2) {
      List var3 = var1.getChildren();
      if (var3 == null) {
         return false;
      }

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (var5.getName().equals("DeleteRule")) {
            if (!var2) {
               if (var1.getName().equals("Exit")) {
                  return false;
               }

               return true;
            }

            return true;
         }

         if (this.shouldDeleteThisRule(var5, var2)) {
            return true;
         }
      }

      return false;
   }

   void findAndReplaceAndOrRndBlocks(Element var1) throws GenerateException {
      RandomAndOrList var2 = new RandomAndOrList();
      this.findAndOrBlocks(var1, var2);
      var2.fixValues();
      var2.replaceValues(1, this.rng, "random");
      var2.replaceValues(2, this.rng, "copied");
   }

   void findAndReplaceRandomBlocks(Element var1, int var2) throws GenerateException {
      int var3 = 0;
      List var4 = var1.getChildren();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         if (var6.getName().equals("Item")) {
            String var7 = var6.getAttributeValue("key");
            if (var7.startsWith("Random")) {
               String var8 = this.getRandomBlockId(var6);
               ReplacementConfig var9 = this.replacements.get(var8);
               if (!var9.shouldGenerate(this.rng)) {
                  if (var9.deleteRule) {
                     Element var17 = new Element("DeleteRule");
                     var1.setContent(var3, var17);
                     this.generatedElements.put(var8, var17);
                  } else {
                     var1.removeContent(var3);
                  }
                  continue;
               }

               int var10 = RuleType.recognizeRuleTypeFromParents(var6);
               Element var11 = var9.generateRandomBlock(this.rng, var2, var10, null);
               this.handleActionParameters(var11, var6, "random", var8);
               var11.setAttribute("generated", "random");
               var11.setAttribute("randomId", var8);
               if (var9.replacementSuperType != 1 && var9.replacementSuperType != 2) {
                  var1.setContent(var3, var11);
                  this.generatedElements.put(var8, var11);
               } else {
                  this.applyAttributes(var11, var6);
                  Element var12 = this.getRandomIdParam(var6);
                  if (var12 != null) {
                     var6.removeContent(var12);
                  }

                  List var13 = var11.getChildren();
                  int var14 = var13.size();

                  for (int var15 = 0; var15 < var14; var15++) {
                     var6.addContent(((Element)var13.get(0)).detach());
                  }

                  this.generatedElements.put(var8, var6);
               }
            }
         }

         List var16 = var6.getChildren();
         if (var16 != null) {
            this.findAndReplaceRandomBlocks(var6, this.recognizeRulePart(var2, var6.getName()));
         }

         var3++;
      }
   }

   private int recognizeRulePart(int var1, String var2) {
      if (var1 != 1) {
         return var1;
      } else if (RulePart.isConditionElement(var2)) {
         return 2;
      } else {
         return RulePart.isActionElement(var2) ? 3 : var1;
      }
   }

   void findAndReplaceRandomParameters(Element var1, int var2, int var3) throws GenerateException {
      List var4 = var1.getChildren();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         if (var6.getName().equals("Item")) {
            String var7 = var6.getAttributeValue("returnType");
            if (var7 != null && var7.equals("order")) {
               String var8 = var6.getAttributeValue("generated");
               if (var8 != null) {
               }
            }

            ItemRandomValueRanges var17 = ItemRandomValueRanges.getRandomValueRanges(var6);
            Element var9 = null;
            List var10 = var6.getChildren("Param");

            for (int var11 = 0; var11 < var10.size(); var11++) {
               Element var12 = (Element)var10.get(var11);
               String var13 = var12.getAttributeValue("generate");
               if (var13 != null && var13.equals("random")) {
                  String var14 = findIdentifierInParams(var6, false);
                  if (var14 == null) {
                     var14 = var6.getAttributeValue("randomId");
                  }

                  if (var14 == null) {
                     this.randomGenerateParameter(var6, var12, var17);
                  } else {
                     if (var9 == null) {
                        ReplacementConfig var15 = this.replacements.get(var14);
                        var9 = var15.generateRandomBlock(this.rng, var2, var3, var17);
                     }

                     Element var18 = this.getParamElement(var12.getAttributeValue("key"), var9);
                     if (var18 != null) {
                        this.replaceParameter(var6, var18, "random", var14);
                     }

                     this.generatedElements.put(var14, var6);
                  }
               }
            }
         }

         List var16 = var6.getChildren();
         if (var16 != null) {
            this.findAndReplaceRandomParameters(var6, this.recognizeRulePart(var2, var6.getName()), var3);
         }
      }
   }

   private void randomGenerateParameter(Element var1, Element var2, ItemRandomValueRanges var3) throws GenerateException {
      String var4 = var2.getAttributeValue("key");
      String var5 = var2.getAttributeValue("type");
      String var6 = var3 == null ? null : var3.getForParam(var4);
      boolean var7 = var5.equals("double");
      if (var6 != null) {
         RandomValueConfig var9 = RandomValueConfig.parseRandomValue(var6, var7);
         if (var9 == null) {
            Log.error("Error parsing randomValue '" + var6 + "' in strategy template.");
            return;
         }

         if (var9.isValid()) {
            double var16 = var9.generateRandomValue(this.rng, var7);
            String var14;
            if (var7) {
               int var18 = var9.getDecimals(2);
               var14 = SQUtils.d2String(var16, var18);
            } else {
               var14 = Integer.toString((int)var16);
            }

            var2.setText(var14);
            return;
         }
      }

      String var15 = var1.getAttributeValue("key");
      ReplacementParameter var10 = this.replacements.findReplacementParamByKey(var15, var4);
      if (var10 != null) {
         String var13;
         if (var7) {
            var13 = var10.getDoubleParam(this.rng, -1.0, -1.0, null);
         } else {
            int var17 = var10.getIntParam(this.rng, null, null);
            var13 = Integer.toString(var17);
         }

         var2.setText(var13);
      } else {
         BlockDefinition var11 = this.blocksConfig.getBlock(var15);
         if (var11 != null) {
            BlockParameter var12 = var11.getParamByKey(var4);
            if (var12 == null) {
               throw new GenerateException("Unknown parameter " + var4 + " in block " + var15 + "");
            }

            String var8;
            if (var7) {
               var8 = this.generateRandomDoubleParam(var12);
            } else {
               var8 = this.generateRandomIntParam(var12);
            }

            var2.setText(var8);
         }
      }
   }

   private String generateRandomIntParam(BlockParameter var1) {
      int var2 = 10;
      if (var1.controlType.equals("combo")) {
         String var10 = var1.values;
         String[] var12 = var10.split(",");
         int var13 = this.rng.nextInt(var12.length);
         String var15 = var12[var13];
         String[] var16 = var15.split("=");
         return var16[1];
      }

      if (var1.key.equals("#TimeFrom#") || var1.key.equals("#TimeTo#")) {
         int var9 = Value.generateHH(this.rng, var1);
         int var11 = Value.generateMM(this.rng, var1);
         var2 = var9 * 100 + var11;
      } else if (!Value.isPredefined(var1.minValue) && !Value.isPredefined(var1.maxValue)) {
         int var3 = (int)var1.minValue;
         int var4 = (int)var1.maxValue;
         int var5 = (int)var1.step;
         if (var4 < var3) {
            int var6 = var3;
            var3 = var4;
            var4 = var6;
         }

         if (var4 == var3) {
            var2 = var4;
         } else {
            int var14 = (var4 - var3) / var5 + 1;
            if (var14 <= 0) {
            }

            int var7 = this.rng.nextInt(var14);
            var2 = var3 + var7 * var5;
         }
      } else {
         var2 = var1.iDefaultValue;
      }

      return Integer.toString(var2);
   }

   private String generateRandomDoubleParam(BlockParameter var1) {
      if (var1.controlType.equals("combo")) {
         String var14 = var1.values;
         String[] var3 = var14.split(",");
         int var15 = this.rng.nextInt(var3.length);
         String var16 = var3[var15];
         String[] var6 = var16.split("=");
         return var6[1];
      }

      int var4 = var1.decimals;
      if (var1.controlType.equals("jspinnerVar") || var1.controlType.equals("jspinner")) {
         if (!Value.isPredefined(var1.minValue) && !Value.isPredefined(var1.maxValue)) {
            double var5 = var1.minValue;
            double var7 = var1.maxValue;
            double var9 = var1.step;
            double var13;
            if (var5 == var7) {
               var13 = var7;
            } else {
               int var11 = (int)((var7 - var5) / var9) + 1;
               if (var11 < 0) {
                  if (var5 < 0.0) {
                     var5 = -10000.0;
                  } else {
                     var5 = 0.0;
                  }

                  if (var7 > 0.0) {
                     var7 = 10000.0;
                  } else {
                     var7 = 0.0;
                  }

                  if (var9 < 1.0) {
                     var9 = 1.0;
                  }

                  var11 = (int)((var7 - var5) / var9) + 1;
               }

               int var12 = this.rng.nextInt(var11);
               var13 = var5 + var12 * var9;
            }

            return SQUtils.d2String(var13, var4);
         }

         double var2 = var1.dDefaultValue;
      }

      return "10";
   }

   void fillGeneratedElements(Element var1) throws GenerateException {
      List var2 = var1.getChildren();
      if (var2 != null) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            if (var4.getName().equals("Item")) {
               String var5 = findIdentifierInParams(var4, false);
               if (var5 == null) {
                  continue;
               }

               if (!this.generatedElements.containsKey(var5)) {
                  this.generatedElements.put(var5, var4);
               }
            }

            this.fillGeneratedElements(var4);
         }
      }
   }

   void findAndReplaceSameOrOppositeParameters(Element var1, StrategyWithVariables var2) throws GenerateException {
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (var5.getName().equals("Item")) {
            String var6 = var5.getAttributeValue("returnType");
            if (var6 != null && var6.equals("order")) {
               String var7 = var5.getAttributeValue("generated");
               if (var7 != null) {
               }
            }

            List var22 = var5.getChildren("Param");

            for (int var8 = 0; var8 < var22.size(); var8++) {
               Element var9 = (Element)var22.get(var8);
               String var10 = var9.getAttributeValue("generate");
               if (var10 != null) {
                  if (var10.equals("same")) {
                     String var11 = var9.getAttributeValue("identification");
                     if (var11 == null) {
                        throw new GenerateException("Cannot find Replacement ID for Same parameter!");
                     }

                     Element var12 = this.generatedElements.get(var11);
                     if (var12 == null) {
                        throw new GenerateException(String.format("Block with Identification '%s' not found for parameter copy!", var11));
                     }

                     Element var13 = this.getParamElement(var9.getAttributeValue("key"), var12);
                     if (var13 != null) {
                        this.replaceParameter(var5, var13.clone(), "same", var11);
                     }
                  } else if (var10.equals("opposite")) {
                     String var23 = var9.getAttributeValue("identification");
                     if (var23 == null) {
                        throw new GenerateException("Cannot find Replacement ID for Opposite parameter!");
                     }

                     Element var24 = this.generatedElements.get("_Negated_" + var23);
                     if (var24 == null) {
                        Element var25 = this.generatedElements.get(var23);
                        if (var25 == null) {
                           throw new GenerateException(String.format("Block with Identification '%s' not found for parameter negation!", var23));
                        }

                        var25 = var25.clone();
                        String var15 = var25.getAttributeValue("key");

                        IBlock var14;
                        try {
                           var14 = Blocks.getBlockObject(var15, var2, var25);
                        } catch (BlockDefinitionException var19) {
                           throw new GenerateException("Cannot create block '" + var15 + "'from given config", var19);
                        } catch (Exception var20) {
                           throw new GenerateException(
                              "Cannot create block '"
                                 + var15
                                 + "' from given config. Exception: "
                                 + var20.getClass().getSimpleName()
                                 + " : "
                                 + var20.getMessage(),
                              var20
                           );
                        }

                        try {
                           IBlock var16 = this.replacements.negateBlock(var14, var2);
                           var24 = Blocks.generateBlockTreeXml(var16);
                           this.generatedElements.put("_Negated_" + var23, var24);
                        } catch (BlockDefinitionException | XmlStrategyException var18) {
                           throw new GenerateException(var18);
                        }
                     }

                     String var27 = var9.getAttributeValue("key");
                     Element var28 = this.getParamElement(var27, var24);
                     if (var28 != null) {
                        this.replaceParameter(var5, var28.clone(), "negated", var23);
                     } else if (var27.equals("#Price#")) {
                        String var29 = var5.getAttributeValue("key");
                        String var30 = var24.getAttributeValue("key");
                        String var17 = var5.getAttributeValue("generated");
                        if (!var29.equals(var30) && var17 != null && var17.equals("random")) {
                           this.copyAttributes(var24, var5);
                        }
                     }
                  }
               }
            }
         }

         List var21 = var5.getChildren();
         if (var21 != null) {
            this.findAndReplaceSameOrOppositeParameters(var5, var2);
         }
      }
   }

   private Element getParamElement(String var1, Element var2) {
      String var3 = var2.getAttributeValue("key");
      if (!var3.equals("AND") && !var3.equals("OR")) {
         List var4 = var2.getChildren("Param");

         for (int var5 = 0; var5 < var4.size(); var5++) {
            Element var6 = (Element)var4.get(var5);
            if (var6.getAttributeValue("key").equals(var1)) {
               return var6;
            }
         }

         return null;
      } else {
         return this.getParamElementAndOr(var1, var2);
      }
   }

   private Element getParamElementAndOr(String var1, Element var2) {
      List var3 = var2.getChildren("Item");
      if (var3.size() == 0) {
         return null;
      }

      if (var3.size() == 1) {
         return this.getParamElement(var1, (Element)var3.get(0));
      }

      int var4 = this.rng.nextInt(var3.size());
      return this.getParamElement(var1, (Element)var3.get(var4));
   }

   private void handleActionParameters(Element var1, Element var2, String var3, String var4) throws GenerateException {
      String var5 = var2.getAttributeValue("superType");
      if (var5 != null && var5.equals("action")) {
         for (Element var7 : var2.getChildren("Param")) {
            if (!var7.getAttributeValue("key").equals("#Identification#")) {
               String var8 = var7.getText();
               List var9 = var7.getChildren();
               String var10 = var7.getAttributeValue("generate");
               String var11 = var7.getAttributeValue("identification");
               if (var10 == null) {
                  if (!var8.equals("") || var9.size() > 0) {
                     this.replaceParameter(var1, var7, "fixed", null);
                  }
               } else if (var3.equals("random") && (var10.equals("opposite") || var10.equals("same"))) {
                  this.replaceParameterToGenerate(var1, var7, var10, var11);
               }
            }
         }
      }
   }

   private void replaceParameter(Element var1, Element var2, String var3, String var4) throws GenerateException {
      String var5 = var2.getAttributeValue("key");
      Element var6 = null;

      for (Element var8 : var1.getChildren("Param")) {
         if (var8.getAttributeValue("key").equals(var5)) {
            var6 = var8;
            break;
         }
      }

      if (var6 != null) {
         for (Attribute var11 : var2.getAttributes()) {
            var6.setAttribute(var11.getName(), var11.getValue());
         }

         var6.setAttribute("generated", var3);
         if (var4 != null) {
            var6.setAttribute("randomId", var4);
         }

         var6.removeAttribute("generate");
         var6.removeAttribute("identification");
         var6.removeContent();
         List var10 = var2.getChildren();

         for (int var12 = 0; var12 < var10.size(); var12++) {
            var6.addContent(((Element)var10.get(var12)).clone());
         }

         String var13 = var2.getText().trim();
         if (var13 != null && !var13.equals("")) {
            var6.setText(var13);
         }
      }
   }

   private void copyAttributes(Element var1, Element var2) {
      List var3 = var1.getAttributes();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Attribute var5 = (Attribute)var3.get(var4);
         var2.setAttribute(var5.getName(), var5.getValue());
      }

      var2.setAttribute("generated", "same");
   }

   private void replaceParameterToGenerate(Element var1, Element var2, String var3, String var4) throws GenerateException {
      String var5 = var2.getAttributeValue("key");
      Element var6 = null;

      for (Element var8 : var1.getChildren("Param")) {
         if (var8.getAttributeValue("key").equals(var5)) {
            var6 = var8;
            break;
         }
      }

      if (var6 != null) {
         for (Attribute var11 : var2.getAttributes()) {
            var6.setAttribute(var11.getName(), var11.getValue());
         }

         var6.removeContent();
         List var10 = var2.getChildren();

         for (int var12 = 0; var12 < var10.size(); var12++) {
            var6.addContent(((Element)var10.get(var12)).clone());
         }

         String var13 = var2.getText().trim();
         if (var13 != null && !var13.equals("")) {
            var6.setText(var13);
         }
      }
   }

   private void replaceParameterValue(Element var1, String var2, String var3) {
      for (Element var5 : var1.getChildren("Param")) {
         if (var5.getAttributeValue("key").equals(var2)) {
            var5.setText(var3);
            break;
         }
      }
   }

   private void findAndReplaceSameBlocks(Element var1) throws GenerateException {
      int var2 = 0;
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (var5.getName().equals("Item")) {
            String var6 = var5.getAttributeValue("key");
            if (var6.startsWith("Same")) {
               String var7 = this.getRandomBlockId(var5);
               Element var8 = this.generatedElements.get(var7);
               if (var8 == null) {
                  throw new GenerateException(String.format("Block with Identification '%s' not found for copy!", var7));
               }

               if (var8.getName().equals("DeleteRule")) {
                  Element var9 = new Element("DeleteRule");
                  var1.setContent(var2, var9);
                  continue;
               }

               var8 = var8.clone();
               var8.setAttribute("generated", "same");
               this.handleActionParameters(var8, var5, "same", var7);
               var1.setContent(var2, var8);
            }
         }

         List var10 = var5.getChildren();
         if (var10 != null) {
            this.findAndReplaceSameBlocks(var5);
         }

         var2++;
      }
   }

   private void findAndReplaceNegatedBlocks(Element var1, StrategyBase var2) throws GenerateException {
      int var3 = 0;

      for (Element var5 : var1.getChildren()) {
         if (var5.getName().equals("Item")) {
            String var6 = var5.getAttributeValue("key");
            if (var6.startsWith("Negate") || var6.startsWith("Opposite")) {
               String var7 = this.getRandomBlockId(var5);
               Element var8 = this.generatedElements.get(var7);
               if (var8 == null) {
                  if (Log.isDebugEnabled()) {
                     Log.debug("Element '{}' not found for negation!", var7);
                  }

                  var1.removeContent(var3);
                  continue;
               }

               if (var8.getName().equals("DeleteRule")) {
                  Element var18 = new Element("DeleteRule");
                  var1.setContent(var3, var18);
                  continue;
               }

               Element var9 = var8.clone();
               String var10 = var9.getAttributeValue("key");

               IBlock var11;
               try {
                  var11 = Blocks.getBlockObject(var10, var2, var9);
               } catch (BlockDefinitionException var15) {
                  throw new GenerateException("Cannot create block '" + var10 + "' from given config. Exception", var15);
               } catch (Exception var16) {
                  throw new GenerateException(
                     "Cannot create block '" + var10 + "' from given config. Exception: " + var16.getClass().getSimpleName() + " : " + var16.getMessage(),
                     var16
                  );
               }

               try {
                  IBlock var12 = this.replacements.negateBlock(var11, var2);
                  Element var13 = Blocks.generateBlockTreeXml(var12);
                  this.generatedElements.put("_Negated_" + var7, var13);
                  this.handleOppositeActionParameters(var13, var8, var5);
                  var13.setAttribute("generated", "negated");
                  var13.setAttribute("randomId", var7);
                  if (var9.getAttributeValue("returnType") != null) {
                     var13.setAttribute("returnType", var9.getAttributeValue("returnType"));
                  }

                  this.handleActionParameters(var13, var5, "negated", var7);
                  var1.setContent(var3, var13);
               } catch (BlockDefinitionException | XmlStrategyException var14) {
                  throw new GenerateException(var14);
               }
            }
         }

         List var17 = var5.getChildren();
         if (var17 != null) {
            this.findAndReplaceNegatedBlocks(var5, var2);
         }

         var3++;
      }
   }

   private void handleOppositeActionParameters(Element var1, Element var2, Element var3) {
      String var4 = this.getRandomBlockParamValue(var3, "#UseOwnMagicNumber#");
      if (var4 != null) {
         boolean var5 = Boolean.parseBoolean(var4);
         if (var5) {
            String var6 = this.getRandomBlockParamValue(var3, "#MagicNumber#");
            if (var6 != null) {
               for (Element var8 : var1.getChildren("Param")) {
                  if (var8.getAttributeValue("key").equals("#MagicNumber#")) {
                     var8.setText(var6);
                     var8.setAttribute("variable", "true");
                     var8.setAttribute("variableType", "INT");
                     break;
                  }
               }
            }
         } else {
            String var9 = this.getRandomBlockParamValue(var2, "#MagicNumber#");
            if (var9 != null) {
               for (Element var11 : var1.getChildren("Param")) {
                  if (var11.getAttributeValue("key").equals("#MagicNumber#")) {
                     var11.setText(var9);
                     var11.setAttribute("variable", "true");
                     var11.setAttribute("variableType", "INT");
                     break;
                  }
               }
            }
         }
      }
   }

   private void applyAttributes(Element var1, Element var2) {
      for (Attribute var4 : var1.getAttributes()) {
         var2.setAttribute(var4.getName(), var4.getValue());
      }
   }

   private Element getRandomIdParam(Element var1) {
      for (Element var3 : var1.getChildren()) {
         String var4 = var3.getAttributeValue("controlType");
         if (var4 != null && var4.equals("randomId")) {
            return var3;
         }
      }

      return null;
   }

   private void saveToFile(Element var1, String var2) {
      try {
         XMLUtil.xmlToFile(var1, new File(MainApp.getDataPath() + "tests/tmp/" + var2 + ".xml"));
      } catch (Exception var4) {
         var4.printStackTrace();
      }
   }

   private void fixBlocksAndParams(Element var1) throws GenerateException {
      List var2 = var1.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("key");
         if (var4.getName().equals("Item")) {
            BlockDefinition var6 = this.blocksConfig.getBlock(var5);
            if (var6 != null) {
               try {
                  this.fixCategoryReturnType(var4, var6);
                  String var7 = var4.getAttributeValue("returnType");
                  if (var7 != null && var7.equals("order")) {
                     this.fixActionBlock(var4, var6);
                  }
               } catch (Exception var8) {
                  throw new GenerateException(var8.getMessage(), var8);
               }
            }
         } else if (var4.getName().equals("Param")) {
            String var9 = var4.getText();
            if (var9 != null && var9.endsWith(".00")) {
               var4.setText(var9.replace(".00", ""));
            }
         }

         List var10 = var4.getChildren();
         if (var10 != null) {
            this.fixBlocksAndParams(var4);
         }
      }
   }

   private void fixCategoryReturnType(Element var1, BlockDefinition var2) throws BlockDefinitionException {
      if (var2.categoryType != null) {
         var1.setAttribute("categoryType", var2.categoryType);
      }

      var1.setAttribute("returnType", ReturnTypes.translateReturnType(var2.returnType));
      var1.removeAttribute("help");
   }

   private void fixActionBlock(Element var1, BlockDefinition var2) throws Exception {
      ArrayList var3 = new ArrayList();
      ObjectListIterator var4 = var2.parameters.iterator();

      while (var4.hasNext()) {
         BlockParameter var5 = (BlockParameter)var4.next();
         Element var6 = XMLUtil.getItemParameter(var1, var5.key, false);
         if (var6 != null) {
            var3.add(var6);
         }
      }

      Element var7 = XMLUtil.getItemParameter(var1, "#Identification#", false);
      if (var7 != null) {
         var3.add(var7);
      }

      var1.removeChildren("Param");

      for (Element var9 : var3) {
         var1.addContent(var9);
      }
   }

   public Element generateMutatedRandomBlock(String var1, Element var2, int var3, int var4) throws GenerateException {
      ReplacementConfig var5 = this.replacements.get(var1);
      return var5.generateMutatedRandomBlock(this.rng, var2, var3, var4);
   }

   public void freeGenerator() {
      this.free = true;
   }

   boolean isFree() {
      return this.free;
   }

   void setUsed() {
      this.free = false;
   }

   public void setRandomGenerator(IRandomGenerator var1) {
      this.rng = var1;
   }

   public Element verifyParameters(String var1, Element var2, Element var3) throws Exception {
      if (var2 == null) {
         return var2;
      }

      if (var1 == null) {
         var1 = this.recognizeRandomBlock(var2, var3);
      }

      if (var1 == null) {
         return var2;
      }

      ReplacementConfig var4 = this.replacements.get(var1);
      if (var4 == null) {
         throw new Exception("Replacement config with ID '" + var1 + "' not found!");
      }

      String var5 = var2.getName();
      Element var7;
      if (var5.equals("Param")) {
         var7 = var2.getParentElement();
         if (var7 == null && var3 != null) {
            var7 = this.cloneParentReplaceParamValue(var3, var2);
         }
      } else {
         var7 = var2;
      }

      if (var7 == null) {
         throw new Exception("No parent found!");
      } else {
         String var6 = var7.getAttributeValue("key");
         if (var4.verifyItem(var6, var7)) {
            Log.info("Passed {}", this.printParams(var7));
            return var2;
         } else {
            Log.info("Key '" + var6 + "' verified FALSE");
            return null;
         }
      }
   }

   private Object printParams(Element var1) {
      String var2 = var1.getAttributeValue("key");
      List var3 = var1.getChildren("Param");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         var2 = var2 + " " + var5.getText();
      }

      return var2;
   }

   private Element cloneParentReplaceParamValue(Element var1, Element var2) {
      var1 = var1.clone();
      String var3 = var2.getAttributeValue("key");
      List var4 = var1.getChildren("Param");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("key");
         if (var3.equals(var7)) {
            var6.setText(var2.getText());
            return var1;
         }
      }

      return var1;
   }

   private String recognizeRandomBlock(Element var1, Element var2) {
      String var3 = var1.getAttributeValue("randomId");
      if (var3 != null) {
         return var3;
      }

      if (var2 != null) {
         var3 = var2.getAttributeValue("randomId");
         if (var3 != null) {
            return var3;
         }

         var3 = this.findRandomIdInSiblings(var2);
         if (var3 != null) {
            return var3;
         }
      }

      var1 = var1.getParentElement();
      if (var1 != null) {
         var3 = var1.getAttributeValue("randomId");
         if (var3 != null) {
            return var3;
         }

         var3 = this.findRandomIdInSiblings(var1);
         if (var3 != null) {
            return var3;
         }
      }

      return null;
   }

   private String findRandomIdInSiblings(Element var1) {
      List var2 = var1.getChildren("Param");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("controlType");
         if (var5 != null && var5.equals("randomId")) {
            return var4.getText();
         }
      }

      return null;
   }
}
