package com.strategyquant.tradinglib.generator;

import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.blocks.random.RandomValueConfig;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.util.StockpickerGenFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockSettingsConverter {
   private static final Logger Log = LoggerFactory.getLogger(BlockSettingsConverter.class);
   private static final String NAME_PRICES = "Prices";
   private static final String NAME_PRICES_DOT = "Prices.";
   private static final String NAME_RANDOM_INDICATOR_SIGNALS = "Indicators";
   private static final String NAME_RANDOM_INDICATOR_SIGNALS_DOT = "Indicators.";
   private final int engine;
   private Element elCharts;
   private Element elOrderTypes;
   private Element elExitTypes;
   private Element wizardConfig;
   private Int2ObjectOpenHashMap<Element> mapAllBlocksInConfig;
   private HashMap<String, ArrayList<Element>> mapRandomGroupBlocks;
   private String andWeight;
   private String orWeight;
   private String exitRuleProbability;
   private String marketSides;
   private boolean entrySymmetry;
   private boolean exitSymmetry;
   private boolean containsExitRule;
   private boolean longEntryImprovement = false;
   private boolean shortEntryImprovement = false;
   private boolean longExitImprovement = false;
   private boolean shortExitImprovement = false;
   private boolean longActionImprovement = false;
   private boolean shortActionImprovement = false;
   private Element strategyTemplate;
   private String strategyType;
   private List<Element> arrayBuildingBlocks;
   private List<Element> arrayBuildingBlocksArtifical;

   public BlockSettingsConverter(Element var1, Element var2) throws GenerateException {
      Element var3 = var1.getChild("Blocks");
      this.strategyTemplate = var2;
      Element var4 = var1.getChild("WhatToBuild");
      if (var4 == null) {
         throw new GenerateException("Builder project config doesn't contain <WhatToBuild>");
      }

      this.engine = StrategyTemplateGenerator.getEngine(var1);
      this.strategyType = var4.getChild("StrategyType").getAttributeValue("type");
      Element var5 = var4.getChild("MarketSides");
      this.marketSides = var5.getAttributeValue("type");
      this.entrySymmetry = false;
      this.exitSymmetry = false;
      if (this.marketSides.equals("both")) {
         this.entrySymmetry = Boolean.parseBoolean(var5.getChildText("EntrySymmetry"));
         this.exitSymmetry = Boolean.parseBoolean(var5.getChildText("ExitSymmetry"));
      }

      this.elCharts = this.convertToCharts(var4.getChild("RulesComplexity"));
      this.initBuildingBlocks(var3.getChild("BuildingBlocks"));
      this.initCustomDataIndy(var3.getChild("CustomData"));
      this.elOrderTypes = var3.getChild("OrderTypes");
      this.elExitTypes = var3.getChild("ExitTypes");
      this.containsExitRule = this.checkContainsExitRule(var3);
      if ((this.andWeight == null || this.andWeight.equals("0")) && (this.orWeight == null || this.orWeight.equals("0"))) {
         this.andWeight = "1";
         this.orWeight = "0";
      }

      if (this.strategyType.equals("improve")) {
         this.loadPartsToImprove(var1);
      }

      this.loadRandomGroups(var2);
      this.getWizardConfig();
      this.applyGlobalSLPTSettings(var1, var3);
   }

   private void loadRandomGroups(Element var1) {
      Element var2 = var1.getChild("Strategy").getChild("RandomGroups");
      if (var2 != null) {
         if (this.mapRandomGroupBlocks == null) {
            this.mapRandomGroupBlocks = new HashMap<>();
         }

         List var3 = var2.getChildren();

         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            List var6 = var5.getChildren();
            ArrayList var7 = new ArrayList(var6.size());

            for (int var8 = 0; var8 < var6.size(); var8++) {
               Element var9 = (Element)var6.get(var8);
               Element var10 = this.convertRandomGroupItemToBlock(var9);
               if (var10 != null) {
                  var7.add(var10);
               }
            }

            String var12 = var5.getAttributeValue("id");
            if (var12 != null && !var12.equals("")) {
               this.mapRandomGroupBlocks.put(var12, var7);
            } else {
               Log.error("Some random group doesn't have ID!");
            }
         }

         int var11 = 0;

         while (var11 < 50 && this.convertRandomBlocksInGroup()) {
            var11++;
         }
      }
   }

   private boolean convertRandomBlocksInGroup() {
      boolean var1 = false;

      for (ArrayList var3 : this.mapRandomGroupBlocks.values()) {
         ArrayList var4 = null;
         Iterator var5 = var3.iterator();

         while (var5.hasNext()) {
            Element var6 = (Element)var5.next();

            try {
               Element var7 = var6.getChild("Generated");
               String var8 = this.getRandomGroupId(var7);
               if (var8 != null) {
                  ArrayList var9 = this.mapRandomGroupBlocks.get(var8);
                  if (var9 != null) {
                     var5.remove();
                     var1 = true;
                     if (var4 == null) {
                        var4 = new ArrayList();
                     }

                     var4.addAll(var9);
                  }
               }
            } catch (GenerateException var10) {
            }
         }

         if (var4 != null) {
            var3.addAll(var4);
         }
      }

      return var1;
   }

   private Element convertRandomGroupItemToBlock(Element var1) {
      Element var2 = new Element("Block");
      String var3 = var1.getAttributeValue("key");
      if (var3 == null) {
         return null;
      }

      var2.setAttribute("key", var3);
      var2.setAttribute("use", "true");
      var2.setAttribute("weight", "1");
      Element var4 = new Element("Generated");
      var4.setAttribute("weight", "1");
      var2.addContent(var4);
      Element var5 = CustomBlocks.getElement(var3);
      Element var6 = var1;
      List var7 = var6.getChildren("Param");
      if (var7.size() > 0) {
         for (int var8 = 0; var8 < var7.size(); var8++) {
            Element var9 = (Element)var7.get(var8);
            Element var10 = this.convertRandomGroupItemParamToBlock(var9, var5);
            var4.addContent(var10);
         }
      }

      List var13 = var6.getChildren("Block");
      if (var13.size() > 0) {
         var7 = var1.getChildren("Block");
         var2.setAttribute("keepParams", "true");

         for (int var14 = 0; var14 < var7.size(); var14++) {
            Element var16 = (Element)var7.get(var14);
            Element var11 = this.convertRandomGroupItemParamToBlock(var16, var5);
            var11.setAttribute("keepParams", "true");
            var4.addContent(var11);
         }
      }

      Element var15 = new Element("Predefined");
      var2.addContent(var15);
      return var2;
   }

   private Element convertRandomGroupItemParamToBlock(Element var1, Element var2) {
      Element var3 = var1.clone();
      String var4 = var3.getAttributeValue("key");
      if (var4.equals("#Chart#")) {
         var3.setAttribute("generation", "random");
         var3.setAttribute("allCharts", "true");
      } else {
         String var5 = var1.getAttributeValue("generate");
         if (var5 != null && var5.equals("random")) {
            var3.setAttribute("generation", "random");
            String var6 = var1.getAttributeValue("randomValue");
            if (var6 != null) {
               var3.setAttribute("randomValue", var6);
            }

            if (var2 != null) {
               try {
                  Element var7 = XMLUtil.getItemParameter(var2, var4, false);
                  if (var7 != null) {
                     String var8 = this.copyAttr(var7, var3, "minValue");
                     String var9 = this.copyAttr(var7, var3, "maxValue");
                     String var10 = this.copyAttr(var7, var3, "step");
                     if (var8.equals("0") && var9.equals("0") && var10.equals("0")) {
                        String var11 = var7.getAttributeValue("genMinValue");
                        if (var11 != null) {
                           var3.setAttribute("minValue", var11);
                        }

                        var11 = var7.getAttributeValue("genMaxValue");
                        if (var11 != null) {
                           var3.setAttribute("maxValue", var11);
                        }

                        var3.setAttribute("step", "1");
                     }
                  }
               } catch (Exception var12) {
               }
            } else {
               String var13 = var3.getAttributeValue("genMinValue");
               if (var13 != null) {
                  var3.setAttribute("minValue", var13);
               }

               var13 = var3.getAttributeValue("genMaxValue");
               if (var13 != null) {
                  var3.setAttribute("maxValue", var13);
               }
            }
         } else {
            var3.setAttribute("generation", "fixed");
         }
      }

      return var3;
   }

   private String copyAttr(Element var1, Element var2, String var3) {
      if (var1 != null && var2 != null && var3 != null) {
         String var4 = var1.getAttributeValue(var3);
         if (var4 != null) {
            var2.setAttribute(var3, var4);
            return var4;
         }
      }

      return "";
   }

   private void initBuildingBlocks(Element var1) {
      List var2 = var1.getChildren();
      this.arrayBuildingBlocks = new ArrayList<>();
      this.arrayBuildingBlocksArtifical = new ArrayList<>();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("key");
         if (var5 != null) {
            if (var5.contains("talib_MAVP")) {
               var4.setAttribute("use", "false");
            }

            if (var5.equals("AND")) {
               this.andWeight = var4.getAttributeValue("weight");
            }

            if (var5.equals("OR")) {
               this.orWeight = var4.getAttributeValue("weight");
            }

            if (!var5.contains(".")) {
               this.arrayBuildingBlocks.add(var4);
            } else {
               this.arrayBuildingBlocksArtifical.add(var4);
            }
         }
      }

      if (this.andWeight == null) {
         this.andWeight = "0";
      }

      if (this.orWeight == null) {
         this.orWeight = "0";
      }
   }

   private void initCustomDataIndy(Element var1) throws GenerateException {
      if (var1 != null) {
         List var2 = var1.getChildren();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            String var5 = var4.getAttributeValue("use");
            if (var5 != null && var5.equals("true")) {
               String var6 = var4.getAttributeValue("name");
               CustomDataInfo var7 = CustomDataManager.getDataInfo(var6);
               if (var7 == null) {
                  Log.info("Cannot find custom data indicator '{}', ignoring!", var6);
               } else {
                  String var8 = var4.getAttributeValue("weight");
                  String var9 = var4.getAttributeValue("required");
                  Element var10 = this.convertCDataIndyToNBlock(var7, var8, var9);
                  if (var10 != null) {
                     this.arrayBuildingBlocks.add(var10);
                  }
               }
            }
         }
      }
   }

   private Element convertCDataIndyToNBlock(CustomDataInfo var1, String var2, String var3) throws GenerateException {
      Element var4 = new Element("Block");
      var4.setAttribute("key", "CDataIndy_" + var1.name);
      var4.setAttribute("id", var1.name);
      var4.setAttribute("use", "true");
      var4.setAttribute("weight", var2);
      var4.setAttribute("dataType", Integer.toString(var1.dataType));
      var4.addContent(createCDataShiftParamElement());
      var4.addContent(createCDataValueParamElement(var1.values, var1));
      return var4;
   }

   public static Element createCDataShiftParamElement() {
      Element var0 = new Element("Param");
      var0.setAttribute("key", "#Shift#");
      var0.setAttribute("type", "int");
      var0.setAttribute("name", "Shift");
      var0.setAttribute("step", "1");
      var0.setAttribute("maxValue", "-1000002");
      var0.setAttribute("minValue", "-1000001");
      var0.setAttribute("generation", "random");
      var0.setAttribute("controlType", "jspinnerVar");
      var0.setAttribute("defaultValue", "1");
      return var0;
   }

   public static Element createCDataValueParamElement(int var0, CustomDataInfo var1) throws GenerateException {
      Element var2 = new Element("Param");
      var2.setAttribute("key", "#Value#");
      var2.setAttribute("type", "int");
      var2.setAttribute("name", "Value");
      var2.setAttribute("controlType", "combo");
      var2.setAttribute("defaultValue", "0");

      String var3;
      try {
         var3 = CustomDataManager.createCDataValues(var0, var1);
      } catch (Exception var5) {
         throw new GenerateException(var5);
      }

      var2.setAttribute("values", var3);
      return var2;
   }

   private void loadPartsToImprove(Element var1) {
      boolean var2 = true;
      boolean var3 = true;
      Element var4 = var1.getChild("WhatToBuild");
      if (var4 != null) {
         Element var5 = var4.getChild("MarketSides");
         if (var5 != null) {
            Element var6 = var5.getChild("EntrySymmetry");
            if (var6 != null && var6.getText().equals("false")) {
               var2 = true;
            }

            Element var7 = var5.getChild("ExitSymmetry");
            if (var7 != null && var7.getText().equals("false")) {
               var3 = true;
            }
         }
      }

      Element var16 = var1.getChild("PartsToImprove");
      if (var16 != null) {
         Element var17 = var16.getChild("EntryRules");
         Element var10 = var17.getChild("LongImprovement");
         if (var10.getAttributeValue("use").equals("true")) {
            this.longEntryImprovement = true;
         }

         var10 = var17.getChild("ShortImprovement");
         if (var10.getAttributeValue("use").equals("true")) {
            this.shortEntryImprovement = true;
         }

         if (var2 && (this.longEntryImprovement || this.shortEntryImprovement)) {
            this.longEntryImprovement = true;
            this.longEntryImprovement = true;
         }

         Element var8 = var16.getChild("OrderTypes");
         var10 = var8.getChild("LongImprovement");
         if (var10.getAttributeValue("use").equals("true")) {
            this.longActionImprovement = true;
         }

         var10 = var8.getChild("ShortImprovement");
         if (var10.getAttributeValue("use").equals("true")) {
            this.shortActionImprovement = true;
         }

         Element var9 = var16.getChild("ExitRules");
         if (var9.getAttributeValue("symmetry").equals("true")) {
            var10 = var9.getChild("LongImprovement");
            if (var10.getAttributeValue("use").equals("true")) {
               this.longExitImprovement = true;
            }
         } else {
            var10 = var9.getChild("ShortImprovement");
            if (var10.getAttributeValue("use").equals("true")) {
               this.shortExitImprovement = true;
            }
         }

         if (var3 && (this.longExitImprovement || this.shortExitImprovement)) {
            this.longExitImprovement = true;
            this.shortExitImprovement = true;
         }
      }
   }

   public Element convert() throws GenerateException {
      return this.strategyType.equals("template") ? this.createTemplateReplacements() : this.createStandardReplacements();
   }

   private Element createTemplateReplacements() throws GenerateException {
      Element var1 = new Element("Replacements");
      var1.setAttribute("type", "advanced");
      HashMap var2 = new HashMap();
      GeneratorBase.checkUniqueRandomIdentifiers(this.strategyTemplate, var2);

      for (Entry var4 : var2.entrySet()) {
         String var5 = (String)var4.getKey();
         Element var6 = (Element)var4.getValue();
         String var7 = var6.getAttributeValue("key");
         String var8 = var6.getAttributeValue("categoryType");
         String var9 = var6.getAttributeValue("superType");
         if (var9 != null) {
            if (var9.equals("condition")) {
               Element var17 = this.createConditionReplacement(var5, var6);
               var1.addContent(var17);
               continue;
            }

            if (var9.equals("action")) {
               Element var16 = this.createEntryReplacement(var5, var6, 0);
               var1.addContent(var16);
               continue;
            }

            if (var9.equals("value")) {
               Element var15 = this.createValueReplacement(var5, var6);
               var1.addContent(var15);
               continue;
            }
         }

         if (var8 == null) {
            throw new GenerateException(String.format("Cannot find identifier type for %s !", var5));
         }

         if (var8.equals("other") && !var7.startsWith("talib_")) {
            try {
               IBlock var14 = Blocks.get(var7);
               Class var11 = var14.getClass();
               if (Blocks.checkContains(var11, "ActionBlock")) {
                  Element var12 = this.createEntryReplacement(var5, var6, 0);
                  var1.addContent(var12);
               } else {
                  Element var18 = this.createConditionReplacement(var5, var6);
                  var1.addContent(var18);
               }
            } catch (BlockDefinitionException var13) {
               throw new GenerateException(var13.getMessage());
            }
         } else {
            Element var10 = this.createConditionReplacement(var5, var6);
            var1.addContent(var10);
         }
      }

      return var1;
   }

   private Element createStandardReplacements() throws GenerateException {
      Element var1 = new Element("Replacements");
      var1.setAttribute("type", "advanced");
      if (this.strategyType.equals("improve")) {
         if (this.longEntryImprovement) {
            Element var2 = this.createConditionReplacement("RandomConditionLong", null);
            var1.addContent(var2);
         }
      } else if (this.marketSides.equals("both") || this.marketSides.equals("long")) {
         Element var4 = this.createConditionReplacement("RandomConditionLong", null);
         var1.addContent(var4);
      }

      if (this.strategyType.equals("improve")) {
         if (this.shortEntryImprovement) {
            Element var5 = this.createConditionReplacement("RandomConditionShort", null);
            var1.addContent(var5);
         }
      } else if (this.marketSides.equals("short") || this.marketSides.equals("both") && !this.entrySymmetry) {
         Element var6 = this.createConditionReplacement("RandomConditionShort", null);
         var1.addContent(var6);
      }

      if (this.containsExitRule || this.longExitImprovement || this.shortExitImprovement) {
         if (this.marketSides.equals("both") || this.marketSides.equals("long") || this.longExitImprovement) {
            Element var7 = this.createExitReplacement("RandomConditionLongExit");
            var1.addContent(var7);
         }

         if (this.marketSides.equals("short") || this.marketSides.equals("both") && !this.exitSymmetry || this.shortExitImprovement) {
            Element var8 = this.createExitReplacement("RandomConditionShortExit");
            var1.addContent(var8);
         }
      }

      if (this.marketSides.equals("both") || this.marketSides.equals("long") || this.longActionImprovement || this.longExitImprovement) {
         Element var9 = this.createEntryReplacement("RandomActionLong", null, 1);
         var1.addContent(var9);
      }

      if (this.marketSides.equals("short")
         || this.marketSides.equals("both") && (!this.entrySymmetry || !this.exitSymmetry)
         || this.shortActionImprovement
         || this.shortExitImprovement) {
         Element var10 = this.createEntryReplacement("RandomActionShort", null, -1);
         var1.addContent(var10);
      }

      int var11 = StockpickerGenFixer.recognizeEngine(this.strategyTemplate);
      if (var11 == 1316847364 || var11 == -1816889229) {
         Element var3 = this.createValueReplacement("RandomPSValue", null);
         var1.addContent(var3);
      }

      return var1;
   }

   private Element createEntryReplacement(String var1, Element var2, int var3) throws GenerateException {
      Element var4 = new Element("Replacement");
      var4.setAttribute("identification", var1);
      var4.setAttribute("type", "action");
      var4.setAttribute("probability", "100");
      var4.addContent(this.elCharts.clone());
      this.addOrderTypeBlocks(var4, var2, var3);
      this.addStopLimitEntryBlocks(var4, "Stop/Limit Price Levels", null);
      this.addStopLimitEntryBlocks(var4, "Stop/Limit Price Ranges", null);
      return var4;
   }

   private boolean addStopLimitEntryBlocks(Element var1, String var2, Element var3) {
      String var4 = var2 + ".";
      if (var3 == null) {
         var3 = new Element("StopLimit" + var2.replace("Stop/Limit Price ", ""));
         var1.addContent(var3);
      }

      boolean var5 = false;

      for (int var6 = 0; var6 < this.arrayBuildingBlocksArtifical.size(); var6++) {
         Element var7 = this.arrayBuildingBlocksArtifical.get(var6);
         if (var7.getAttributeValue("use").equals("true")) {
            String var8 = var7.getAttributeValue("key");
            if (var8.contains(var2)) {
               Element var9 = var7.clone();
               var9.setAttribute("key", var8.replace(var4, ""));
               var3.addContent(var9);
               var5 = true;
            }
         }
      }

      return var5;
   }

   private void addOrderTypeBlocks(Element var1, Element var2, int var3) throws GenerateException {
      Element var4 = new Element("Blocks");
      var1.addContent(var4);
      String var5 = null;
      if (var2 != null) {
         String var6 = var2.getAttributeValue("isParameter");
         if (var6 != null && var6.equals("true")) {
            var5 = var2.getAttributeValue("key");
         }
      }

      int var11 = 0;
      List var7 = this.elOrderTypes.getChildren();

      for (int var8 = 0; var8 < var7.size(); var8++) {
         Element var9 = (Element)var7.get(var8);
         if (var5 == null && var9.getAttributeValue("use").equals("true") || var9.getAttributeValue("key").equals(var5)) {
            Element var10 = var9.clone();
            this.replaceFormulas(var10, var2);
            this.updateNonFormulaExitTypeParams(var10);
            this.tryAddDirectionParam(var10, var3);
            this.tryAddMagicNumberParam(var10);
            if (this.engine == 1316847364 || this.engine == -1816889229) {
               this.setFixedSPFormulas(var10);
            }

            var4.addContent(var10);
            var11++;
         }
      }

      if (var11 == 0) {
         throw new GenerateException(
            String.format("Bad Configuration - You generate block '%s' randomly in your template, but it is not chosen in Building Blocks settings!", var5)
         );
      }
   }

   private void updateNonFormulaExitTypeParams(Element var1) throws GenerateException {
      ArrayList var2 = new ArrayList();
      List var3 = this.elExitTypes.getChildren("Block");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         String var6 = var5.getAttributeValue("type");
         if (var6 == null || !var6.equals("formula")) {
            var2.add(var5.clone());
         }
      }

      Element var22 = var1.getChild("Generated");
      if (var22 != null) {
         var3 = var22.getChildren("Param");

         for (int var23 = 0; var23 < var3.size(); var23++) {
            Element var24 = (Element)var3.get(var23);
            String var7 = var24.getAttributeValue("key");
            String var8 = var7.replace("#", "");

            for (int var9 = 0; var9 < var2.size(); var9++) {
               Element var10 = (Element)var2.get(var9);
               if (var10.getAttributeValue("key").equals(var8)) {
                  String var11 = var10.getAttributeValue("use");
                  if (var11 != null && !var11.equals("false")) {
                     String var12 = this.getSubParamKey(var8);
                     Element var13 = XMLUtil.findFirstWithKey(var10, "Param", var12);
                     if (var13 != null) {
                        String var14 = var13.getAttributeValue("generation");
                        String var15 = var13.getAttributeValue("minValue");
                        String var16 = var13.getAttributeValue("maxValue");
                        String var17 = var13.getAttributeValue("step");
                        String var18 = var13.getAttributeValue("values");
                        String var19 = var13.getAttributeValue("defaultValue");
                        var24.setAttribute("generation", var14);
                        if (var14.equals("random")) {
                           if (var15 != null) {
                              var24.setAttribute("minValue", var15);
                           }

                           if (var16 != null) {
                              var24.setAttribute("maxValue", var16);
                           }

                           if (var17 != null) {
                              var24.setAttribute("step", var17);
                           }

                           if (var18 != null) {
                              var24.setAttribute("values", var18);
                           }

                           if (var8.equals("ExitAfterBars.ExitAfterBars")) {
                              String var20 = var10.getAttributeValue("probability");
                              if (var20 != null) {
                                 var24.setAttribute("probability", var20);
                              }
                           }
                        } else {
                           var24.setAttribute("defaultValue", var19);
                        }
                     } else {
                        Log.error("Cannot get parameters of exit type block with key '" + var8 + "'");
                     }
                     break;
                  }

                  var24.setAttribute("weight", "0");
                  var24.setAttribute("minValue", "0");
                  var24.setAttribute("maxValue", "0");
                  break;
               }
            }
         }
      }
   }

   private String getSubParamKey(String var1) {
      String[] var2 = var1.split("\\.");
      return var2 != null && var2.length == 2 ? "#" + var2[1] + "#" : var1;
   }

   private void tryAddDirectionParam(Element var1, int var2) throws GenerateException {
      Element var3 = var1.getChild("Generated");
      if (var3 != null) {
         Element var4 = this.getItemsParam(var1.getAttributeValue("key"), "#Direction#");
         if (var4 != null) {
            Element var5 = new Element("Param");
            var5.setAttribute("key", "#Direction#");
            var5.setAttribute("generation", "fixed");
            if (var2 != 0) {
               var5.setAttribute("defaultValue", Integer.toString(var2));
            } else {
               var5.setAttribute("defaultValue", var4.getAttributeValue("defaultValue"));
            }

            var3.addContent(var5);
         }
      }
   }

   private void tryAddMagicNumberParam(Element var1) throws GenerateException {
      Element var2 = var1.getChild("Generated");
      if (var2 != null) {
         Element var3 = this.getItemsParam(var1.getAttributeValue("key"), "#MagicNumber#");
         if (var3 != null) {
            Element var4 = new Element("Param");
            var4.setAttribute("key", "#MagicNumber#");
            var4.setAttribute("generation", "fixed");
            var4.setAttribute("defaultValue", "MagicNumber");
            var2.addContent(var4);
         }
      }
   }

   private Element getItemsParam(String var1, String var2) throws GenerateException {
      if (this.mapAllBlocksInConfig.containsKey(var1.hashCode())) {
         Element var3 = (Element)this.mapAllBlocksInConfig.get(var1.hashCode());
         if (var3.getAttributeValue("key").equals(var1)) {
            ArrayList var4 = XMLUtil.getNestedElements(var3, "Param");

            for (int var5 = 0; var5 < var4.size(); var5++) {
               Element var6 = (Element)var4.get(var5);
               if (var6.getAttributeValue("key").equals(var2)) {
                  Element var7 = var6.clone();
                  return var7.detach();
               }
            }
         }
      }

      return null;
   }

   private Element getWizardConfig() throws GenerateException {
      if (this.wizardConfig != null) {
         return this.wizardConfig;
      }

      File var1 = new File(SQPaths.wizardConfigPath);
      if (!var1.exists()) {
         throw new GenerateException("Wizard config file not found.");
      }

      try {
         this.wizardConfig = XMLUtil.fileToXmlElement(var1);
         ArrayList var2 = XMLUtil.getNestedElements(this.wizardConfig.getChild("Blocks"), "Item");
         this.mapAllBlocksInConfig = new Int2ObjectOpenHashMap();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            Element var4 = (Element)var2.get(var3);
            String var5 = var4.getAttributeValue("key");
            if (var5 != null) {
               if (this.mapAllBlocksInConfig.containsKey(var5.hashCode())) {
                  throw new GenerateException("Key '" + var5 + "' already exists in map - duplicate hash value!");
               }

               this.mapAllBlocksInConfig.put(var5.hashCode(), var4);
            }
         }

         return this.wizardConfig;
      } catch (Exception var6) {
         throw new GenerateException(var6);
      }
   }

   private Element getWizardExitRule(String var1) throws GenerateException {
      for (Element var3 : XMLUtil.getNestedElements(this.getWizardConfig(), "ExitRule")) {
         if (var3.getAttributeValue("key").equals(var1)) {
            return var3.clone().clone();
         }
      }

      return null;
   }

   private Element createExitReplacement(String var1) throws GenerateException {
      Element var2 = new Element("Replacement");
      var2.setAttribute("identification", var1);
      var2.setAttribute("type", "condition");
      var2.setAttribute("probability", this.exitRuleProbability);
      var2.setAttribute("deleteRule", "true");
      var2.setAttribute("andWeight", this.andWeight);
      var2.setAttribute("orWeight", this.orWeight);
      var2.addContent(this.elCharts.clone());
      Element var3 = new Element("Blocks");
      var2.addContent(var3);
      if (!this.exitRuleProbability.equals("0")) {
         this.addConditionBlocks(var3);
      }

      return var2;
   }

   private Element createConditionReplacement(String var1, Element var2) throws GenerateException {
      Element var3 = new Element("Replacement");
      var3.setAttribute("identification", var1);
      var3.setAttribute("probability", "100");
      var3.setAttribute("andWeight", this.andWeight);
      var3.setAttribute("orWeight", this.orWeight);
      Element var4 = new Element("Blocks");
      var3.addContent(var4);
      if (var2 != null) {
         String var5 = this.getRandomGroupId(var2);
         if (var5 != null) {
            if (this.mapRandomGroupBlocks != null && !this.mapRandomGroupBlocks.containsKey(var5)) {
               throw new GenerateException("Template contains reference to random block group ID: " + var5 + "', but this group doesn't exist!");
            }

            ArrayList var15 = this.mapRandomGroupBlocks.get(var5);

            for (int var16 = 0; var16 < var15.size(); var16++) {
               Element var18 = ((Element)var15.get(var16)).clone();
               var4.addContent(var18);
            }

            String var17 = var2.getAttributeValue("superType");
            if (var17 != null && var17.equals("value")) {
               var3.setAttribute("type", "value");
            } else {
               var3.setAttribute("type", "condition");
            }

            String var19 = this.getChartIndex(var2);
            Element var9;
            if (var19 == null) {
               var9 = this.randomGroupCloneSelectedChart("0", this.elCharts);
            } else {
               var9 = this.randomGroupCloneSelectedChart(var19, this.elCharts);
            }

            List var10 = var9.getChildren();

            for (int var11 = 0; var11 < var10.size(); var11++) {
               Element var12 = (Element)var10.get(var11);
               var12.setAttribute("minConditions", "1");
               var12.setAttribute("maxConditions", "1");
            }

            var3.addContent(var9);
            return var3;
         }

         String var6 = var2.getAttributeValue("isParameter");
         if (var6 != null && var6.equals("true")) {
            var3.addContent(this.getChartsWithOneCondition(this.elCharts));
            Element var7 = this.getBlock(var2.getAttributeValue("key"));
            this.tryAddDirectionParam(var7, 0);
            this.tryAddMagicNumberParam(var7);
            var4.addContent(var7);
            this.fixRandomParametersInBlock(var7);
            String var8 = var2.getAttributeValue("returnType");
            if (var8.equals("boolean")) {
               var3.setAttribute("type", "condition");
            } else {
               var3.setAttribute("type", "value");
            }

            return var3;
         }
      }

      String var13 = this.getChartIndex(var2);
      Element var14;
      if (var13 == null) {
         var14 = this.elCharts.clone();
      } else {
         var14 = this.randomGroupCloneSelectedChart(var13, this.elCharts);
      }

      var3.addContent(var14);
      var3.setAttribute("type", "condition");
      this.addConditionBlocks(var4);
      return var3;
   }

   private void fixRandomParametersInBlock(Element var1) {
      if (var1.getName().equals("Param")) {
         String var2 = var1.getAttributeValue("generation");
         if (var2 != null && var2.equals("random")) {
            double var3 = this.getDoubleAttribute(var1, "minValue", -1.0);
            double var5 = this.getDoubleAttribute(var1, "maxValue", -1.0);
            double var7 = this.getDoubleAttribute(var1, "genMinValue", -9999999.0);
            double var9 = this.getDoubleAttribute(var1, "genMaxValue", -9999999.0);
            if (var7 != -9999999.0 && var3 != -1.0) {
               var1.setAttribute("minValue", var1.getAttributeValue("genMinValue"));
            }

            if (var9 != -9999999.0 && var5 != -1.0) {
               var1.setAttribute("maxValue", var1.getAttributeValue("genMaxValue"));
            }
         }
      }

      List var11 = var1.getChildren();

      for (int var12 = 0; var12 < var11.size(); var12++) {
         this.fixRandomParametersInBlock((Element)var11.get(var12));
      }
   }

   private double getDoubleAttribute(Element var1, String var2, double var3) {
      String var5 = var1.getAttributeValue(var2);
      if (var5 == null) {
         return var3;
      }

      try {
         return Double.parseDouble(var5);
      } catch (NumberFormatException var7) {
         return var3;
      }
   }

   private Element createValueReplacement(String var1, Element var2) throws GenerateException {
      Element var3 = new Element("Replacement");
      var3.setAttribute("identification", var1);
      var3.setAttribute("probability", "100");
      var3.setAttribute("andWeight", this.andWeight);
      var3.setAttribute("orWeight", this.orWeight);
      var3.setAttribute("type", "value");
      Element var4 = new Element("Blocks");
      var3.addContent(var4);
      if (var2 != null) {
         String var5 = this.getRandomGroupId(var2);
         if (var5 != null) {
            if (!this.mapRandomGroupBlocks.containsKey(var5)) {
               throw new GenerateException("Template contains reference to random block group ID: " + var5 + "', but this group doesn't exist!");
            }

            ArrayList var14 = this.mapRandomGroupBlocks.get(var5);

            for (int var16 = 0; var16 < var14.size(); var16++) {
               Element var18 = ((Element)var14.get(var16)).clone();
               var4.addContent(var18);
            }

            String var17 = this.getChartIndex(var2);
            Element var19;
            if (var17 == null) {
               var19 = this.elCharts.clone();
            } else {
               var19 = this.randomGroupCloneSelectedChart(var17, this.elCharts);
            }

            List var9 = var19.getChildren();

            for (int var10 = 0; var10 < var9.size(); var10++) {
               Element var11 = (Element)var9.get(var10);
               var11.setAttribute("minConditions", "1");
               var11.setAttribute("maxConditions", "1");
            }

            var3.addContent(var19);
            return var3;
         }

         String var6 = var2.getAttributeValue("isParameter");
         if (var6 != null && var6.equals("true")) {
            var3.addContent(this.getChartsWithOneCondition(this.elCharts));
            Element var15 = this.getBlock(var2.getAttributeValue("key"));
            this.tryAddDirectionParam(var15, 0);
            this.tryAddMagicNumberParam(var15);
            var4.addContent(var15);
            String var8 = var2.getAttributeValue("returnType");
            if (var8.equals("boolean")) {
               var3.setAttribute("type", "condition");
            } else {
               var3.setAttribute("type", "value");
            }

            return var3;
         }
      }

      String var12 = this.getChartIndex(var2);
      Element var13;
      if (var12 == null) {
         var13 = this.elCharts.clone();
      } else {
         var13 = this.randomGroupCloneSelectedChart(var12, this.elCharts);
      }

      var3.addContent(var13);
      if (var1.equals("RandomPSValue")) {
         this.addStopLimitEntryBlocks(var3, "Stop/Limit Price Levels", var4);
         this.addStopLimitEntryBlocks(var3, "Stop/Limit Price Ranges", var4);
         this.addNormalBlocks(var4);
      } else {
         boolean var7 = this.addStopLimitEntryBlocks(var3, "Stop/Limit Price Levels", var4);
      }

      return var3;
   }

   private void addNormalBlocks(Element var1) {
      HashSet var2 = new HashSet();
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         String var6 = var5.getAttributeValue("key");
         var2.add(var6);
      }

      for (int var10 = 0; var10 < this.arrayBuildingBlocksArtifical.size(); var10++) {
         Element var11 = this.arrayBuildingBlocksArtifical.get(var10);
         if (var11.getAttributeValue("use").equals("true")) {
            String var12 = var11.getAttributeValue("key");
            if (var12.contains("Prices")) {
               String var7 = var12.replace("Prices.", "");
               if (!var2.contains(var7)) {
                  Element var8 = var11.clone();
                  var8.setAttribute("key", var7);
                  var1.addContent(var8);
               }
            }

            if (var12.contains("Indicators")) {
               String var13 = var12.replace("Indicators.", "");
               if (!var2.contains(var13)) {
                  String var14 = var11.getAttributeValue("category");
                  if (var14 != null && (var14.equals("indicators") || var14.equals("prices"))) {
                     Element var9 = var11.clone();
                     var9.setAttribute("key", var13);
                     var1.addContent(var9);
                  }
               }
            }
         }
      }
   }

   private Element randomGroupCloneSelectedChart(String var1, Element var2) {
      Element var3 = new Element("Charts");
      List var4 = var2.getChildren();
      int var5 = Integer.parseInt(var1);
      if (var4.size() <= var5) {
         var5 = 0;
         Log.error("Random group uses chart index " + var1 + ", but there are not enough charts. Continuing with main chart.");
      }

      Element var6 = ((Element)var4.get(var5)).clone();
      var6.setAttribute("chartIndex", var1);
      var3.addContent(var6);
      return var3;
   }

   private String getRandomGroupId(Element var1) throws GenerateException {
      try {
         Element var2 = XMLUtil.getItemParameter(var1, "#Group#", false);
         if (var2 == null) {
            return null;
         }

         String var3 = var2.getText();
         return var3 != null && !var3.equals("") ? var3 : null;
      } catch (Exception var4) {
         throw new GenerateException("Exception", var4);
      }
   }

   private String getChartIndex(Element var1) throws GenerateException {
      if (var1 == null) {
         return null;
      }

      try {
         Element var2 = XMLUtil.getItemParameter(var1, "#Chart#", false);
         if (var2 == null) {
            return null;
         }

         String var3 = var2.getText();
         return var3 != null && !var3.equals("") && !var3.equals("Any") ? var3 : null;
      } catch (Exception var4) {
         throw new GenerateException("Exception", var4);
      }
   }

   private void addConditionBlocks(Element var1) throws GenerateException {
      boolean var2 = false;

      for (int var3 = 0; var3 < this.arrayBuildingBlocks.size(); var3++) {
         Element var4 = this.arrayBuildingBlocks.get(var3);
         if (var4.getAttributeValue("use").equals("true")) {
            String var5 = var4.getAttributeValue("key");
            if (!var5.equals("AND") && !var5.equals("OR")) {
               if (var5.equals("BarHour") || var5.equals("BarMinute")) {
                  var2 = true;
               }

               Element var6 = var4.clone();
               this.tryAddDirectionParam(var6, 0);
               this.tryAddMagicNumberParam(var6);
               var1.addContent(var6);
            }
         }
      }

      boolean var13 = false;
      boolean var15 = false;
      IntOpenHashSet var16 = new IntOpenHashSet();

      for (int var17 = 0; var17 < this.arrayBuildingBlocksArtifical.size(); var17++) {
         Element var7 = this.arrayBuildingBlocksArtifical.get(var17);
         if (var7.getAttributeValue("use").equals("true")) {
            String var8 = var7.getAttributeValue("key");
            if (var8.contains("Prices")) {
               String var9 = var8.replace("Prices.", "");
               if (!var16.contains(var9.hashCode())) {
                  Element var10 = var7.clone();
                  var10.setAttribute("key", var9);
                  var1.addContent(var10);
                  var13 = true;
                  var16.add(var9.hashCode());
               }
            }

            if (var8.contains("Indicators")) {
               String var18 = var8.replace("Indicators.", "");
               if (!var16.contains(var18.hashCode())) {
                  Element var19 = var7.clone();
                  var19.setAttribute("key", var18);
                  var1.addContent(var19);

                  try {
                     int var11 = Blocks.getReturnType(Blocks.get(var18));
                     if (var11 == 1 || var11 == 6) {
                        var15 = true;
                     }
                  } catch (Exception var12) {
                  }

                  var16.add(var18.hashCode());
               }
            }
         }
      }

      if (var2 || var15) {
         this.addNumberBlock(var1);
      }
   }

   private void addNumberBlock(Element var1) {
      Element var2 = new Element("Block");
      var2.setAttribute("key", "Number");
      var2.setAttribute("weight", "1");
      var2.setAttribute("use", "true");
      var2.setAttribute("category", "operators");
      Element var3 = new Element("Generated");
      var2.addContent(var3);
      Element var4 = new Element("Param");
      var4.setAttribute("key", "#Number#");
      var4.setAttribute("name", "Number");
      var4.setAttribute("type", "double");
      var4.setAttribute("generation", "random");
      var4.setAttribute("weight", "1");
      var4.setAttribute("minValue", "0");
      var4.setAttribute("maxValue", "59");
      var4.setAttribute("step", "1");
      var3.addContent(var4);
      var1.addContent(var2);
   }

   private Element getChartsWithOneCondition(Element var1) {
      Element var2 = new Element("Charts");

      for (Element var4 : var1.getChildren()) {
         Element var5 = var4.clone();
         var5.setAttribute("minConditions", "1");
         var5.setAttribute("maxConditions", "1");
         var5.setAttribute("minExitConditions", "1");
         var5.setAttribute("maxExitConditions", "1");
         var2.addContent(var5);
      }

      return var2;
   }

   private Element getBlock(String var1) throws GenerateException {
      Element var2 = null;

      for (int var3 = 0; var3 < this.arrayBuildingBlocks.size(); var3++) {
         var2 = this.arrayBuildingBlocks.get(var3);
         String var4 = var2.getAttributeValue("key");
         if (var4.equals(var1)) {
            return var2.clone();
         }
      }

      var2 = null;
      if (this.mapAllBlocksInConfig.containsKey(var1.hashCode())) {
         Element var10 = (Element)this.mapAllBlocksInConfig.get(var1.hashCode());
         var2 = new Element("Item");

         for (Attribute var5 : var10.getAttributes()) {
            var2.setAttribute(var5.getName(), var5.getValue());
         }

         var2.setAttribute("weight", "1");
         var2.setAttribute("use", "true");
         Element var12 = new Element("Generated");
         var12.setAttribute("weight", "1");
         var2.addContent(var12);
         ArrayList var13 = XMLUtil.getNestedElements(var10, "Param");

         for (int var6 = 0; var6 < var13.size(); var6++) {
            Element var7 = (Element)var13.get(var6);
            var7 = var7.clone();
            var7.setAttribute("generation", "random");
            var7.setAttribute("weight", "1");
            var12.addContent(var7.clone());
         }
      }

      if (var2 == null) {
         throw new GenerateException(String.format("Bad configuration - Cannot find block '%s' in config!", var1));
      } else {
         return var2;
      }
   }

   private void setFixedSPFormulas(Element var1) throws GenerateException {
      Element var2 = var1.getChild("Generated");
      if (var2 != null) {
         for (Element var4 : var2.getChildren("Param")) {
            String var5 = var4.getAttributeValue("key");
            if (var5.contains("MoveSL2BE") || var5.contains("TrailingStop")) {
               var4.setAttribute("generation", "fixed");
            }
         }
      }
   }

   private void replaceFormulas(Element var1, Element var2) throws GenerateException {
      String var3 = var1.getAttributeValue("key");
      Element var4 = var1.getChild("Formulas");
      if (var4 != null) {
         for (Element var6 : var4.getChildren("Formula")) {
            String var7 = var6.getAttributeValue("key");
            if (var7.equalsIgnoreCase("#ProfitTarget.ProfitTarget#")) {
               boolean var8 = true;
            }

            Element var12 = this.getFormulaReplacement(var3, var7);
            if (var12 != null) {
               var6.removeContent();
               String var13 = var12.getAttributeValue("probability");
               if (var13 != null) {
                  var6.setAttribute("probability", var13);
               }

               for (Element var11 : var12.getChildren()) {
                  var6.addContent(var11.clone().detach());
               }

               this.fixFormulaRandomValues(var6, var2);
            } else {
               String var9 = this.getDependence(var3, var7);
               if (var9 != null) {
                  var6.setAttribute("dependentOn", var9);
               }

               var6.setAttribute("probability", "0");
            }
         }
      }
   }

   private void fixFormulaRandomValues(Element var1, Element var2) {
      if (var2 != null) {
         String var3 = var1.getAttributeValue("key");
         Element var4 = null;

         try {
            var4 = XMLUtil.getItemParameter(var2, var3, false);
         } catch (Exception var16) {
            throw new RuntimeException(var16);
         }

         if (var4 != null) {
            String var5 = var4.getAttributeValue("generate");
            if (var5 != null && var5.equalsIgnoreCase("random")) {
               String var6 = var4.getAttributeValue("randomValue");
               if (var6 != null && !var6.equalsIgnoreCase("default") && !var6.equalsIgnoreCase("")) {
                  boolean var7 = false;
                  String var8 = var4.getAttributeValue("type");
                  if (var8 != null && var8.equalsIgnoreCase("double")) {
                     var7 = true;
                  }

                  RandomValueConfig var9 = RandomValueConfig.parseRandomValue(var6, var7);
                  if (var9 != null) {
                     for (Element var11 : var1.getChildren()) {
                        String var12 = var11.getAttributeValue("key");
                        if (var12 != null && var12.contains(".FixedValue")) {
                           Element var13 = var11.getChild("Generated");
                           if (var13 != null) {
                              Element var14 = var13.getChild("Param");
                              if (var14 != null) {
                                 String var15 = var14.getAttributeValue("generation");
                                 if (var15 == null || !var15.equalsIgnoreCase("random")) {
                                    return;
                                 }

                                 var9.setMinMaxStepAttributes(var14);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private Element getFormulaReplacement(String var1, String var2) throws GenerateException {
      var2 = var2.replace("#", "");

      for (Element var4 : this.elExitTypes.getChildren("Block")) {
         if (var4.getAttributeValue("key").equals(var2)) {
            Element var5 = var4.clone().detach();
            var5.setName("Formula");
            if (var4.getAttributeValue("use").equals("true")) {
               var5.setAttribute("probability", var4.getAttributeValue("probability"));
               List var6 = var5.getChildren("Value");
               Element var7 = this.getWizardExitRule(var2);
               if (var7 != null) {
                  for (Element var9 : var6) {
                     String var10 = var9.getAttributeValue("key");
                     if (var9.getChild("Generated") == null) {
                        for (Element var12 : var7.getChildren("Item")) {
                           if (var12.getAttributeValue("key").equals(var10)) {
                              Element var13 = this.transformFormulaElem(var12);
                              if (var13 != null) {
                                 var9.addContent(var13);
                              }
                           }
                        }
                     }
                  }
               }
            } else {
               var5.setAttribute("probability", "0");
            }

            Element var16 = this.getNoneValueElem(var1, var2);
            if (var16 != null) {
               var5.addContent(var16.clone().detach());
            }

            return var5;
         }
      }

      Element var15 = this.getWizardFormulaElem(var2);
      return var15 != null ? var15 : null;
   }

   private Element getNoneValueElem(String var1, String var2) throws GenerateException {
      String var3 = "#" + var2 + "#";
      Element var4 = this.getItemsParam(var1, var3);
      if (var4 != null) {
         String var5 = var4.getAttributeValue("controlType");
         Element var6 = this.getWizardFormulaElem(var5);
         if (var6 != null) {
            return var6.getChild("ValueNone");
         }
      }

      return null;
   }

   private Element getWizardFormulaElem(String var1) throws GenerateException {
      for (Element var3 : XMLUtil.getNestedElements(this.getWizardConfig(), "Formula")) {
         if (var3.getAttributeValue("key").equals(var1)) {
            Element var4 = var3.clone().detach();
            var4.removeContent();

            for (Element var6 : var3.getChildren("Item")) {
               Element var7 = var6.clone().detach();
               String var8 = var7.getAttributeValue("ignoreInBuilder");
               String var9 = var7.getAttributeValue("noneValue");
               if (var9 != null && var9.equals("true")) {
                  var7.setName("ValueNone");
                  var7.removeAttribute("name");
                  var7.removeAttribute("noneValue");
                  var4.addContent(var7);
               } else if (var8 == null || !var8.equals("true")) {
                  var7.setName("Value");
                  var7.setAttribute("weight", "1");
                  var7.setAttribute("use", "true");
                  Element var10 = this.transformFormulaElem(var7);
                  if (var10 != null) {
                     var7.addContent(var10);
                  }

                  var7.removeContent(var7.getChild("paramCategory"));
                  var4.addContent(var7);
               }
            }

            return var4;
         }
      }

      return null;
   }

   private Element transformFormulaElem(Element var1) {
      ArrayList var2 = XMLUtil.getNestedElements(var1, "Param");
      if (var2.size() == 0) {
         return null;
      }

      Element var3 = new Element("Generated");
      var3.setAttribute("weight", "1");

      for (Element var5 : var2) {
         Element var6 = var5.clone().detach();
         String var7 = var6.getAttributeValue("genMinValue");
         String var8 = var6.getAttributeValue("genMaxValue");
         if (var7 != null) {
            var6.removeAttribute("genMinValue");
            var6.setAttribute("minValue", var7);
         }

         if (var8 != null) {
            var6.removeAttribute("genMaxValue");
            var6.setAttribute("maxValue", var8);
         }

         var6.removeAttribute("postfix");
         var6.removeAttribute("exitMethod");
         var6.removeAttribute("controlType");
         String var9 = var1.getAttributeValue("key").equals("SQ.Formulas.Size.DefineOwnSize") ? "fixed" : "random";
         var6.setAttribute("generation", var9);
         var3.addContent(var6);
      }

      return var3;
   }

   private String getDependence(String var1, String var2) throws GenerateException {
      Element var3 = this.getItemsParam(var1, var2);
      return var3 != null ? var3.getAttributeValue("dependentOn") : null;
   }

   private Element convertToCharts(Element var1) {
      Element var2 = new Element("Charts");
      boolean var3 = false;
      String var4 = var1.getAttributeValue("useDifferentSettings");
      if (var4 != null) {
         var3 = var4.equals("true");
      }

      List var5 = var1.getChildren("Chart");
      if (var5.size() > 0) {
         Element var6 = (Element)var5.get(0);
         String var7 = var6.getAttributeValue("minConditions");
         String var8 = var6.getAttributeValue("maxConditions");
         String var9 = var6.getAttributeValue("minExitConditions");
         String var10 = var6.getAttributeValue("maxExitConditions");
         String var11 = XMLUtil.tryGetAttr(var6, "minExitTypes", "1");
         String var12 = XMLUtil.tryGetAttr(var6, "maxExitTypes", "5");
         String var13 = XMLUtil.tryGetAttr(var6, "minPeriod", "2");
         String var14 = XMLUtil.tryGetAttr(var6, "maxPeriod", "2");
         String var15 = XMLUtil.tryGetAttr(var6, "minShift", "1");
         String var16 = XMLUtil.tryGetAttr(var6, "maxShift", "1");
         int var17 = Integer.parseInt(var15);
         int var18 = Integer.parseInt(var16);
         if (Engines.isTradestationEngine(this.engine)) {
            if (var18 > 0) {
               var15 = "0";
            }
         } else if (var18 > 1) {
            var15 = "1";
         }

         for (int var19 = 0; var19 < var5.size(); var19++) {
            Element var20 = (Element)var5.get(var19);
            Element var21 = new Element("Chart");
            if (!var3) {
               var21.setAttribute("minConditions", var7);
               var21.setAttribute("maxConditions", var8);
               if (var9 != null) {
                  var21.setAttribute("minExitConditions", var9);
               } else {
                  var21.setAttribute("minExitConditions", var9);
               }

               if (var10 != null) {
                  var21.setAttribute("maxExitConditions", var10);
               } else {
                  var21.setAttribute("maxExitConditions", var8);
               }

               var21.setAttribute("minExitTypes", var11);
               var21.setAttribute("maxExitTypes", var12);
               var21.setAttribute("minPeriod", var13);
               var21.setAttribute("maxPeriod", var14);
               var21.setAttribute("minShift", var15);
               var21.setAttribute("maxShift", var16);
            } else {
               var21.setAttribute("minConditions", var20.getAttributeValue("minConditions"));
               var21.setAttribute("maxConditions", var20.getAttributeValue("maxConditions"));
               if (var20.getAttributeValue("minExitConditions") != null) {
                  var21.setAttribute("minExitConditions", var20.getAttributeValue("minExitConditions"));
               } else {
                  var21.setAttribute("minExitConditions", var20.getAttributeValue("minConditions"));
               }

               if (var20.getAttributeValue("maxExitConditions") != null) {
                  var21.setAttribute("maxExitConditions", var20.getAttributeValue("maxExitConditions"));
               } else {
                  var21.setAttribute("maxExitConditions", var20.getAttributeValue("maxConditions"));
               }

               var21.setAttribute("minExitTypes", var11);
               var21.setAttribute("maxExitTypes", var12);
               var21.setAttribute("minPeriod", XMLUtil.tryGetAttr(var20, "minPeriod", "2"));
               var21.setAttribute("maxPeriod", XMLUtil.tryGetAttr(var20, "maxPeriod", "2"));
               if (Engines.isTradestationEngine(this.engine)) {
                  var21.setAttribute("minShift", var15);
               } else {
                  var21.setAttribute("minShift", XMLUtil.tryGetAttr(var20, "minShift", "1"));
               }

               var21.setAttribute("maxShift", var20.getAttributeValue("maxShift", "1"));
            }

            var2.addContent(var21);
         }
      }

      return var2;
   }

   private boolean checkContainsExitRule(Element var1) {
      Element var2 = var1.getChild("ExitTypes");

      for (Element var4 : var2.getChildren()) {
         if (var4.getAttributeValue("key").equals("_ExitRule_")) {
            this.exitRuleProbability = var4.getAttributeValue("probability");
            String var5 = var4.getAttributeValue("use");
            if (var5 != null && var5.equals("false")) {
               this.exitRuleProbability = "0";
            }

            return true;
         }
      }

      if (this.exitRuleProbability == null) {
         this.exitRuleProbability = "0";
      }

      return false;
   }

   private void applyGlobalSLPTSettings(Element var1, Element var2) {
      Element var3 = var1.getChild("WhatToBuild").getChild("SLPTOptions");
      boolean var4 = XMLUtil.getBoolean(var3, "SLATR", false);
      boolean var5 = XMLUtil.getBoolean(var3, "SLFixedPips", false);
      boolean var6 = XMLUtil.getBoolean(var3, "SLPercent", false);
      boolean var7 = XMLUtil.getBoolean(var3, "SeparatedSettings", false);
      boolean var8 = var4;
      boolean var9 = var5;
      boolean var10 = var6;
      if (var7) {
         var8 = XMLUtil.getBoolean(var3, "PTATR", false);
         var9 = XMLUtil.getBoolean(var3, "PTFixedPips", false);
         var10 = XMLUtil.getBoolean(var3, "PTPercent", false);
      }

      List var11 = var2.getChild("ExitTypes").getChildren();
      this.updateExitType(var11, "StopLoss.StopLoss", "SQ.Formulas.SLPT.ATRBasedValue", var4);
      this.updateExitType(var11, "StopLoss.StopLoss", "SQ.Formulas.SLPT.FixedValue", var5);
      this.updateExitType(var11, "StopLoss.StopLoss", "SQ.Formulas.SLPT.PctValue", var6);
      this.updateExitType(var11, "ProfitTarget.ProfitTarget", "SQ.Formulas.SLPT.ATRBasedValue", var8);
      this.updateExitType(var11, "ProfitTarget.ProfitTarget", "SQ.Formulas.SLPT.FixedValue", var9);
      this.updateExitType(var11, "ProfitTarget.ProfitTarget", "SQ.Formulas.SLPT.PctValue", var10);
   }

   private void updateExitType(List<Element> var1, String var2, String var3, boolean var4) {
      for (int var5 = 0; var5 < var1.size(); var5++) {
         Element var6 = (Element)var1.get(var5);
         if (var6.getAttributeValue("key").equals(var2)) {
            Element var7 = XMLUtil.findFirstWithKey(var6, "Value", var3);
            if (var7 != null) {
               var7.setAttribute("use", var4 ? "true" : "false");
            }

            return;
         }
      }
   }
}
