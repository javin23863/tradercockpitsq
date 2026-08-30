package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.BlockSuperTypes;
import com.strategyquant.tradinglib.generator.CheckersList;
import com.strategyquant.tradinglib.generator.GenerateException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplacementConfig {
   public static final Logger Log = LoggerFactory.getLogger("ReplacementConfig");
   public Int2ObjectOpenHashMap<ReplacementBlocks> blocksByType = new Int2ObjectOpenHashMap();
   public ObjectArrayList<ReplacementBlock> allBlocks = new ObjectArrayList();
   public ObjectArrayList<ReplacementBlock> priceBlocks = new ObjectArrayList();
   public ObjectArrayList<ReplacementBlock> priceRangeBlocks = new ObjectArrayList();
   public int replacementSuperType;
   private IntArrayList availableBooleanTypes = new IntArrayList();
   private int andWeight;
   private int orWeight;
   private ReplacementChartsConfig chartsConfig = null;
   public String randomId;
   SettingsMap buildSettings;
   private double probability;
   public boolean deleteRule;
   private CheckersList checkersList;
   private String cachedRV;
   private RandomValueConfig cachedRVConfig;

   public ReplacementConfig(ReplacementsConfig var1, BlocksConfig var2, Element var3, SettingsMap var4, CheckersList var5) throws GenerateException {
      this.buildSettings = var4;
      this.checkersList = var5;
      this.replacementSuperType = BlockSuperTypes.translate(var3.getAttributeValue("type"));
      if (this.replacementSuperType < 0) {
         throw new GenerateException("Block supertype '" + var3.getAttributeValue("type") + "' is not recognized!");
      }

      this.randomId = var3.getAttributeValue("identification");
      int var6 = this.loadIntAttribute(var3, "probability", 100);
      this.probability = var6 / 100.0;
      Element var7 = var3.getChild("Charts");
      if (var7 == null) {
         if (this.replacementSuperType == 3 || this.replacementSuperType == 4 || this.replacementSuperType == 1) {
            throw new GenerateException("Block with type ='" + var3.getAttributeValue("type") + "' doesn't have <Charts> settings!");
         }
      } else {
         this.chartsConfig = new ReplacementChartsConfig(var7, this.probability);
      }

      this.andWeight = this.loadIntAttribute(var3, "andWeight", 1);
      this.orWeight = this.loadIntAttribute(var3, "orWeight", 1);
      this.deleteRule = this.loadBoolAttribute(var3, "deleteRule", false);
      List var8 = var3.getChild("Blocks").getChildren();

      for (int var9 = 0; var9 < var8.size(); var9++) {
         Element var10 = (Element)var8.get(var9);
         String var11 = var10.getAttributeValue("key");
         ReplacementBlock var12 = new ReplacementBlock(var1, var11, var2, var10, this);
         if (var12 == null || var12.blockDef == null) {
            throw new GenerateException("Block '" + var11 + "' was not found!");
         }

         this.allBlocks.add(var12);
         if ((var12.blockDef.superType == 2 || var12.blockDef.superType == 3 || var12.blockDef.superType == 1)
            && !this.availableBooleanTypes.contains(var12.blockDef.superType)) {
            this.availableBooleanTypes.add(var12.blockDef.superType);
         }
      }

      if (this.replacementSuperType == 5) {
         var8 = var3.getChild("StopLimitLevels").getChildren();

         for (int var15 = 0; var15 < var8.size(); var15++) {
            Element var17 = (Element)var8.get(var15);
            String var19 = var17.getAttributeValue("key");
            ReplacementBlock var21 = new ReplacementBlock(var1, var19, var2, var17, this);
            if (var21 == null || var21.blockDef == null) {
               throw new GenerateException("Block '" + var19 + "' was not found!");
            }

            if (var21.blockDef.superType != 4 || var21.blockDef.returnType != 2) {
               throw new GenerateException("Block '" + var19 + "' is not price level!");
            }

            this.priceBlocks.add(var21);
         }

         var8 = var3.getChild("StopLimitRanges").getChildren();

         for (int var16 = 0; var16 < var8.size(); var16++) {
            Element var18 = (Element)var8.get(var16);
            String var20 = var18.getAttributeValue("key");
            ReplacementBlock var22 = new ReplacementBlock(var1, var20, var2, var18, this);
            if (var22 == null || var22.blockDef == null) {
               throw new GenerateException("Block '" + var20 + "' was not found!");
            }

            if (var22.blockDef.superType != 4 || var22.blockDef.returnType != 7) {
               throw new GenerateException("Block '" + var20 + "' is not price range!");
            }

            this.priceRangeBlocks.add(var22);
         }
      }

      if (this.probability != 0.0 && this.replacementSuperType == 3 && this.availableBooleanTypes.size() == 0) {
         throw new GenerateException(
            "You haven't selected any building blocks that can be used to construct conditions for random placeholder'" + this.randomId + "!"
         );
      }
   }

   private boolean loadBoolAttribute(Element var1, String var2, boolean var3) {
      String var4 = var1.getAttributeValue(var2);
      return var4 != null ? Boolean.parseBoolean(var4) : var3;
   }

   private int loadIntAttribute(Element var1, String var2, int var3) {
      String var4 = var1.getAttributeValue(var2);
      return var4 != null ? Integer.parseInt(var4) : var3;
   }

   public Element generateRandomBlock(IRandomGenerator var1, int var2, int var3, ItemRandomValueRanges var4) throws GenerateException {
      return this.replacementSuperType == 3
         ? this.generateRandomConditionBlock(var1, 0, var2, var3, var4)
         : this.generateRandomSimpleBlock(var1, 0, var2, var3, var4);
   }

   private Element generateRandomConditionBlock(IRandomGenerator var1, int var2, int var3, int var4, ItemRandomValueRanges var5) throws GenerateException {
      if (this.chartsConfig == null) {
         throw new GenerateException("chartsConfig cannot be null for condition!");
      }

      int var6 = 0;

      for (ReplacementChartConfig var8 : this.chartsConfig) {
         int var9 = var4 == 2 ? var8.minExitConditions : var8.minConditions;
         int var10 = var4 == 2 ? var8.maxExitConditions : var8.maxConditions;
         if (var9 == var10) {
            var8.setConditionsToGenerate(var4, var9);
         } else {
            var8.setConditionsToGenerate(var4, this.getConditionsToGenerate(var1, var9, var10));
         }

         var6 += var8.getConditionsToGenerate(var4);
      }

      if (var6 == 0) {
         return var4 != 2 ? this.createAlwaysTrue() : this.createAlwaysFalse();
      }

      if (var6 == 1) {
         return this.generateBooleanBlock(var1, this.getFirstChartWithConditionToGenerate(var4), var2, var3, var4, var5);
      }

      Element var11 = this.generateAndOrBlock(this.getAndOrNameByWeight(var1));
      ReplacementChartConfig var12 = this.getFirstChartWithConditionToGenerate(var4);
      var11.addContent(this.generateBooleanBlock(var1, var12, var2, var3, var4, var5));
      var12.decreaseConditionsToGenerate(var4);

      while (this.countConditionsToGenerate(var4) != 1) {
         var12 = this.getFirstChartWithConditionToGenerate(var4);
         var11.addContent(this.generateBooleanBlock(var1, var12, var2, var3, var4, var5));
         var12.decreaseConditionsToGenerate(var4);
         Element var15 = this.generateAndOrBlock(this.getAndOrNameByWeight(var1));
         var15.addContent(var11);
         var11 = var15;
      }

      var12 = this.getFirstChartWithConditionToGenerate(var4);
      var11.addContent(this.generateBooleanBlock(var1, var12, var2, var3, var4, var5));
      return var11;
   }

   private Element createAlwaysFalse() {
      Element var1 = new Element("Item");
      var1.setAttribute("key", "AlwaysFalse");
      var1.setAttribute("name", "Always False");
      var1.setAttribute("display", "Always False");
      var1.setAttribute("mI", "BarAndTime");
      var1.setAttribute("returnType", "boolean");
      var1.setAttribute("categoryType", "simpleRules");
      return var1;
   }

   private Element createAlwaysTrue() {
      Element var1 = new Element("Item");
      var1.setAttribute("key", "AlwaysTrue");
      var1.setAttribute("name", "Always True");
      var1.setAttribute("display", "Always True");
      var1.setAttribute("mI", "BarAndTime");
      var1.setAttribute("returnType", "boolean");
      var1.setAttribute("categoryType", "simpleRules");
      return var1;
   }

   private int getConditionsToGenerate(IRandomGenerator var1, int var2, int var3) {
      var2 = Math.min(var2, 30);
      var3 = Math.min(var3, 30);
      int var4 = var2;
      int var5 = var3 - var2 + 1;
      if (var5 == 1) {
         return var4 + var1.nextInt(1);
      }

      int var6 = 0;

      for (int var7 = 0; var7 < var5; var7++) {
         var6 = (int)(var6 + Math.pow(2.0, var7));
      }

      int var12 = var1.nextInt(var6 + 1);
      var6 = 0;

      for (int var8 = 0; var8 < var5; var8++) {
         var6 = (int)(var6 + Math.pow(2.0, var8));
         if (var12 <= var6) {
            return var4 + (var5 - var8 - 1);
         }
      }

      return var4 + var1.nextInt(var3 - var2 + 1);
   }

   private int countConditionsToGenerate(int var1) {
      int var2 = 0;

      for (ReplacementChartConfig var4 : this.chartsConfig) {
         var2 += var4.getConditionsToGenerate(var1);
      }

      return var2;
   }

   private ReplacementChartConfig getFirstChartWithConditionToGenerate(int var1) throws GenerateException {
      for (ReplacementChartConfig var3 : this.chartsConfig) {
         if (var3.getConditionsToGenerate(var1) > 0) {
            return var3;
         }
      }

      return null;
   }

   private Element generateRandomSimpleBlock(IRandomGenerator var1, int var2, int var3, int var4, ItemRandomValueRanges var5) throws GenerateException {
      ReplacementChartConfig var6 = this.getChartConfig(var1);
      return this.generateRandomBlock(this.replacementSuperType, var1, var6, false, var2, var3, var4, var5);
   }

   private ReplacementChartConfig getChartConfig(IRandomGenerator var1) {
      if (this.chartsConfig == null) {
         return null;
      } else if (this.chartsConfig.size() == 1) {
         return this.chartsConfig.get(0);
      } else {
         return this.replacementSuperType == 5 ? this.chartsConfig.get(0) : this.chartsConfig.get(var1.nextInt(this.chartsConfig.size()));
      }
   }

   private String getAndOrNameByWeight(IRandomGenerator var1) {
      int var2 = var1.nextInt(this.andWeight + this.orWeight);
      return var2 < this.andWeight ? "AND" : "OR";
   }

   private Element generateAndOrBlock(String var1) {
      Element var2 = new Element("Item");
      var2.setAttribute("key", var1);
      return var2;
   }

   private Element generateBooleanBlock(IRandomGenerator var1, ReplacementChartConfig var2, int var3, int var4, int var5, ItemRandomValueRanges var6) throws GenerateException {
      if (this.availableBooleanTypes.size() == 1) {
         return this.generateRandomBlock(this.availableBooleanTypes.getInt(0), var1, var2, false, var3, var4, var5, var6);
      }

      for (int var7 = 0; var7 <= 100; var7++) {
         int var8 = var1.nextInt(this.availableBooleanTypes.size());
         Element var9 = this.generateRandomBlock(this.availableBooleanTypes.getInt(var8), var1, var2, false, var3, var4, false, var5, var6);
         if (var9 != null) {
            return var9;
         }
      }

      throw new GenerateException("Cannot generate correct block with this configuration, all possibilities refused by strategy verification");
   }

   private Element generateRandomBlock(
      int var1, IRandomGenerator var2, ReplacementChartConfig var3, boolean var4, int var5, int var6, int var7, ItemRandomValueRanges var8
   ) throws GenerateException {
      return this.generateRandomBlock(var1, var2, var3, var4, var5, var6, true, var7, var8);
   }

   private Element generateRandomBlock(
      int var1, IRandomGenerator var2, ReplacementChartConfig var3, boolean var4, int var5, int var6, boolean var7, int var8, ItemRandomValueRanges var9
   ) throws GenerateException {
      if (var5 > 10) {
         throw new GenerateException("Current depth is bigger than maximal depth!");
      }

      ReplacementBlocks var10 = this.getReplacementBlocksByType(var1, -1, var4, var6, var3.chartIndex);

      for (int var11 = 0; var11 <= 1000; var11++) {
         ReplacementBlock var12 = var10.chooseRandomly(var2, var5);
         Element var14 = var12.getXml();
         String var15 = var14.getAttributeValue("keepParams");
         Element var13;
         if (var15 != null && var15.equals("true")) {
            var13 = var14.clone();
            this.fixSubchartsInSubblocks(var13, var1, var3);
         } else {
            var13 = this.cloneBlockElementWithoutChildren(var14);
            var13.setName("Item");
            this.generateBlockParameters(var12, var13, var2, var1, var3, var5 + 1, var6, var8, var9);
         }

         if (this.checkersList.check(var13)) {
            var13.setAttribute("retries", Integer.toString(var11));
            return var13;
         }
      }

      if (var7) {
         throw new GenerateException("Cannot generate correct block with this configuration, all possibilities refused by strategy verification.");
      } else {
         return null;
      }
   }

   private void fixSubchartsInSubblocks(Element var1, int var2, ReplacementChartConfig var3) {
      if (var2 == 2) {
         if (var3.chartIndex != 0) {
            ArrayList var4 = XMLUtil.getNestedElements(var1, "Param", "key", "#Chart#");

            for (int var5 = 0; var5 < var4.size(); var5++) {
               Element var6 = (Element)var4.get(var5);
               String var7 = var6.getText();
               if (var7 != null && var7.equals("0")) {
                  var6.setText(Integer.toString(var3.chartIndex));
               }
            }
         }
      }
   }

   private void removeGenerateAttributes(Element var1) {
      var1.removeAttribute("generate");
      List var2 = var1.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         this.removeGenerateAttributes((Element)var2.get(var3));
      }
   }

   public Element generateSpecialBlock(IRandomGenerator var1, int var2, int var3, int var4, int var5, ItemRandomValueRanges var6) throws GenerateException {
      ReplacementChartConfig var7 = this.getChartConfig(var1);
      ReplacementBlock var8 = this.chooseSpecialRandomly(var1, var2);
      Element var9 = this.cloneBlockElementWithoutChildren(var8.getXml());
      var9.setName("Item");
      this.generateBlockParameters(var8, var9, var1, 4, var7, var3, var4, var5, var6);
      return var9;
   }

   private ReplacementBlock chooseSpecialRandomly(IRandomGenerator var1, int var2) throws GenerateException {
      ObjectArrayList var3;
      if (var2 == 2) {
         if (this.priceBlocks.size() == 0) {
            throw new GenerateException("There is no Price block defined!");
         }

         var3 = this.priceBlocks;
      } else {
         if (this.priceRangeBlocks.size() == 0) {
            throw new GenerateException("There is no Price Range block defined!");
         }

         var3 = this.priceRangeBlocks;
      }

      if (var3.size() == 1) {
         return (ReplacementBlock)var3.get(0);
      }

      int var4 = var1.nextInt(var3.size());
      return (ReplacementBlock)var3.get(var4);
   }

   private void generateBlockParameters(
      ReplacementBlock var1,
      Element var2,
      IRandomGenerator var3,
      int var4,
      ReplacementChartConfig var5,
      int var6,
      int var7,
      int var8,
      ItemRandomValueRanges var9
   ) throws GenerateException {
      if (var1.parameters != null && var1.parameters.size() != 0) {
         ReplacementParameters var10 = var1.parameters.chooseRandomly(var3, var5, var9);
         ArrayList var11 = null;

         for (ReplacementParameter var13 : var10.parameters) {
            if (var13.generation != 3) {
               switch (var13.blockParam.type) {
                  case "value":
                     if (this.replacementSuperType != 3) {
                        break;
                     }

                     if (var13.blockParam.key.equals("#Indicator#") && (var4 == 1 || var4 == 4)
                        || var13.blockParam.key.startsWith("#Indicator") && (var4 == 1 || var4 == 4 || var4 == 2)) {
                        boolean var16 = this.hasReplacementBlocksByType(4, -1, false, var7, var5.chartIndex);
                        if (!var16) {
                           break;
                        }
                     }

                     var11 = this.addValueParam(var11, var13, var2, var3, var4, var5, var6, var7, var8, var9);
                     break;
                  case "int":
                     var13.addIntParam(var2, var3, var5, -1, var9);
                     break;
                  case "double":
                     var13.addDoubleParam(var2, var3, var9);
                     break;
                  case "string":
                     var13.addStringParam(var2, var3);
                     break;
                  case "boolean":
                     var13.addBooleanParam(var2, var3);
                     break;
                  case "data":
                     var13.addDataParam(var2, var3, var5);
                     break;
                  default:
                     Log.error("Adding parameter of unknown type: " + var13.blockParam.type);
               }
            }
         }

         if (var10.containsFormulas) {
            double[] var17 = this.generateProfitTargetParam(var10, var2, var3, var5, var7, var8);

            for (ReplacementParameter var20 : var10.parameters) {
               if (var20.generation == 3 && var20.formulaDependentOn == null && !var20.blockParam.key.equals("#ProfitTarget.ProfitTarget#")) {
                  var20.addFormulaParam(var2, var3, var10.parameters, var5, var7, var17, var8);
               }
            }

            for (ReplacementParameter var21 : var10.parameters) {
               if (var21.generation == 3 && var21.formulaDependentOn != null) {
                  var21.addFormulaParam(var2, var3, var10.parameters, var5, var7, var17, var8);
               }
            }

            var10.fixParametersOrder(var2);
         }
      }
   }

   private double[] generateProfitTargetParam(ReplacementParameters var1, Element var2, IRandomGenerator var3, ReplacementChartConfig var4, int var5, int var6) throws GenerateException {
      if (!var1.containsProfitTarget) {
         return null;
      }

      for (ReplacementParameter var8 : var1.parameters) {
         if (var8.blockParam.key.equals("#ProfitTarget.ProfitTarget#")) {
            var8.addFormulaParam(var2, var3, var1.parameters, var4, var5, null, var6);
            List var9 = var2.getChildren();

            for (int var10 = 0; var10 < var9.size(); var10++) {
               Element var11 = (Element)var9.get(var10);
               if (var11.getAttributeValue("key").equals("#ProfitTarget.ProfitTarget#")) {
                  Element var12 = var11.getChild("Formula");
                  if (var12 == null) {
                     return null;
                  }

                  if (var12.getAttributeValue("key").equals("SQ.Formulas.SLPT.ATRBasedValue")) {
                     List var21 = var12.getChildren();
                     double var22 = -1.0;
                     double var23 = -1.0;

                     for (int var18 = 0; var18 < var21.size(); var18++) {
                        Element var19 = (Element)var21.get(var18);
                        if (var19.getAttributeValue("key").equals("#Value#")) {
                           String var20 = var19.getText();
                           var22 = Double.parseDouble(var20);
                        }

                        if (var19.getAttributeValue("key").equals("#AtrPeriod#")) {
                           String var24 = var19.getText();
                           var23 = Integer.parseInt(var24);
                        }
                     }

                     return new double[]{-1.0, var22, var23};
                  }

                  if (var12.getAttributeValue("key").equals("SQ.Formulas.SLPT.FixedValue")) {
                     List var13 = var12.getChildren();

                     for (int var14 = 0; var14 < var13.size(); var14++) {
                        Element var15 = (Element)var13.get(var14);
                        if (var15.getAttributeValue("key").equals("#Value#")) {
                           String var16 = var15.getText();
                           return new double[]{1.0, Double.parseDouble(var16), 0.0};
                        }
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private ArrayList<ReplacementBlock> addValueParam(
      ArrayList<ReplacementBlock> var1,
      ReplacementParameter var2,
      Element var3,
      IRandomGenerator var4,
      int var5,
      ReplacementChartConfig var6,
      int var7,
      int var8,
      int var9,
      ItemRandomValueRanges var10
   ) throws GenerateException {
      if (var5 == 1 || var5 == 4) {
         Element var11 = this.generateRandomBlock(4, var4, var6, false, var7, var8, var9, var10);
         Element var12 = new Element("Block");
         var12.setAttribute("key", var2.blockParam.key);
         var12.addContent(var11);
         var12.setAttribute("generated", "random");
         var12.setAttribute("randomId", this.randomId);
         var3.addContent(var12);
         return var1;
      } else if (var5 == 2) {
         return this.addValueParamInComparison(var1, var3, var2, var4, 4, var6, var7, var8, var9, var10);
      } else if (var5 == 3) {
         throw new GenerateException("It shouldn't get here");
      } else {
         throw new GenerateException("Generating VALUE param for unknown supertype: " + var5);
      }
   }

   private ArrayList<ReplacementBlock> addValueParamInComparison(
      ArrayList<ReplacementBlock> var1,
      Element var2,
      ReplacementParameter var3,
      IRandomGenerator var4,
      int var5,
      ReplacementChartConfig var6,
      int var7,
      int var8,
      int var9,
      ItemRandomValueRanges var10
   ) throws GenerateException {
      boolean var11 = var1 == null;
      Element var12;
      ReplacementBlock var13;
      if (var11) {
         ReplacementBlocks var14 = this.getReplacementBlocksByType(var5, -1, var11, var8, var6.chartIndex);
         var13 = var14.chooseRandomly(var4, var7);
         var12 = this.cloneBlockElementWithoutChildren(var13.getXml());
         var2.setName("Item");
         this.generateBlockParameters(var13, var12, var4, var5, var6, var7 + 1, var8, var9, var10);
      } else {
         ReplacementBlock var18 = (ReplacementBlock)var1.get(0);
         ReplacementBlocks var15 = this.getReplacementBlocksByType(var5, var18.blockDef.returnType, var11, var8, var6.chartIndex);
         var13 = var15.chooseRandomly(var4, var7);
         var12 = this.cloneBlockElementWithoutChildren(var13.getXml());
         var2.setName("Item");
         if (var18.blockDef.isIndicator && var13.blockDef.key.equals("Number")) {
            ReplacementParameters var16 = var13.parameters.chooseRandomly(var4, var6, var10);
            ReplacementParameter var17 = var16.parameters.get(0);
            if (var17.generation == 2) {
               this.generatorIndicatorComparisonValue(var18, var13, var12, var17, var4);
            } else {
               var17.addDoubleParam(var12, var4, var10);
            }
         } else {
            this.generateBlockParameters(var13, var12, var4, var5, var6, var7 + 1, var8, var9, var10);
         }
      }

      Element var19 = new Element("Block");
      var19.setAttribute("key", var3.blockParam.key);
      var19.addContent(var12);
      var2.addContent(var19);
      if (var1 == null) {
         var1 = new ArrayList();
      }

      var1.add(var13);
      return var1;
   }

   private void generatorIndicatorComparisonValue(ReplacementBlock var1, ReplacementBlock var2, Element var3, ReplacementParameter var4, IRandomGenerator var5) {
      double var6 = var1.indicatorMin;
      double var8 = var1.indicatorMax;
      double var10 = var1.indicatorStep;
      double var12;
      if (var10 != 0.0) {
         int var14 = (int)((var8 - var6) / var10) + 1;
         if (var14 < 0) {
            if (var6 < 0.0) {
               var6 = -10000.0;
            } else {
               var6 = 0.0;
            }

            if (var8 > 0.0) {
               var8 = 10000.0;
            } else {
               var8 = 0.0;
            }

            if (var10 < 1.0) {
               var10 = 1.0;
            }

            var14 = (int)((var8 - var6) / var10) + 1;
         }

         int var15 = var5.nextInt(var14);
         var12 = var6 + var15 * var10;
      } else {
         var12 = var6 + (var8 - var6) * var5.nextDouble();
      }

      Element var17 = new Element("Param");
      var17.setAttribute("key", var4.blockParam.key);
      var17.setAttribute("controlType", var4.blockParam.controlType);
      var17.setAttribute("type", var4.blockParam.type);
      var17.addContent(SQUtils.d2String(var12, var4.blockParam.decimals));
      var3.addContent(var17);
   }

   private ReplacementBlocks getReplacementBlocksByType(int var1, int var2, boolean var3, int var4, int var5) throws GenerateException {
      int var7 = SQUtils.betterHashNumbers(new int[]{var1, var2, var3 ? 1000 : 2000, var4, var5});
      ReplacementBlocks var6;
      if (!this.blocksByType.containsKey(var7)) {
         var6 = this.createReplacementBlocks(var1, var2, var3, var4, var5);
         if (var6.size() == 0) {
            throw new GenerateException(String.format("No available %s blocks found for chart #%d at %s", BlockSuperTypes.translate(var1), var5, this.randomId));
         }

         this.blocksByType.put(var7, var6);
      } else {
         var6 = (ReplacementBlocks)this.blocksByType.get(var7);
      }

      return var6;
   }

   private boolean hasReplacementBlocksByType(int var1, int var2, boolean var3, int var4, int var5) throws GenerateException {
      int var7 = SQUtils.betterHashNumbers(new int[]{var1, var2, var3 ? 1000 : 2000, var4, var5});
      ReplacementBlocks var6;
      if (!this.blocksByType.containsKey(var7)) {
         var6 = this.createReplacementBlocks(var1, var2, var3, var4, var5);
         if (var6.size() == 0) {
            return false;
         }
      } else {
         var6 = (ReplacementBlocks)this.blocksByType.get(var7);
      }

      return var6.size() > 0;
   }

   private ReplacementBlocks createReplacementBlocks(int var1, int var2, boolean var3, int var4, int var5) throws GenerateException {
      ReplacementBlocks var6 = new ReplacementBlocks();

      for (int var7 = 0; var7 < this.allBlocks.size(); var7++) {
         ReplacementBlock var8 = (ReplacementBlock)this.allBlocks.get(var7);
         if (var1 == var8.blockDef.superType
            && (var2 <= 0 || var8.blockDef.returnType == var2)
            && (var8.key.startsWith("CDataIndy") || var8.key.startsWith("CBlock") || this.checkChartGeneration(var8, var5))) {
            if (var1 == 5) {
               var6.add(var8);
            } else if (var1 == 2) {
               var6.add(var8);
            } else if (var1 == 4) {
               if (var3) {
                  if (!var8.blockDef.notFirstValue) {
                     var6.add(var8);
                  }
               } else {
                  var6.add(var8);
               }
            } else if (var1 == 1) {
               var6.add(var8);
            } else {
               if (var1 != 3) {
                  throw new GenerateException("Undefined superType: " + var1);
               }

               var6.add(var8);
            }
         }
      }

      return var6;
   }

   private boolean checkChartGeneration(ReplacementBlock var1, int var2) {
      ReplacementParameter var3 = var1.parameters.getChartParam();
      if (var3 == null) {
         return true;
      }

      if (var3.generation == 1) {
         String var4 = var3.blockParam.getParamAttribute("defaultValue");
         if (var4 == null || var4.equals("")) {
            return true;
         }

         if (var4.equals(Integer.toString(var2))) {
            return true;
         }

         Log.info("Chart param for block {}  doesn't equal this chart index); {} != {}", new Object[]{var1.blockDef.key, var4, var2});
         return false;
      } else {
         return true;
      }
   }

   private Element cloneBlockElementWithoutChildren(Element var1) {
      Element var2 = new Element(var1.getName());

      for (Attribute var4 : var1.getAttributes()) {
         var2.setAttribute(var4.clone());
      }

      var2.setAttribute("generated", "random");
      var2.setAttribute("randomId", this.randomId);
      return var2;
   }

   public boolean shouldGenerate(IRandomGenerator var1) {
      return var1.probability(this.probability);
   }

   public boolean hasStopLimitOrder() {
      ObjectListIterator var1 = this.allBlocks.iterator();

      while (var1.hasNext()) {
         ReplacementBlock var2 = (ReplacementBlock)var1.next();
         ObjectListIterator var3 = var2.blockDef.parameters.iterator();

         while (var3.hasNext()) {
            BlockParameter var4 = (BlockParameter)var3.next();
            if (var4.key.equals("#Price#")) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean hasPriceAndRangeBlocks() {
      return this.priceBlocks.size() > 0 && this.priceRangeBlocks.size() > 0;
   }

   public Element generateMutatedRandomBlock(IRandomGenerator var1, Element var2, int var3, int var4) throws GenerateException {
      for (int var5 = 0; var5 <= 100; var5++) {
         ReplacementBlock var6 = this.getBlockByKey(var2.getAttributeValue("key"));
         ReplacementChartConfig var7 = this.chartsConfig.get(0);
         this.generateBlockParameters(var6, var2, var1, var6.blockDef.superType, var7, 0, var3, var4, null);
         if (this.checkersList.check(var2)) {
            var2.setAttribute("retries", Integer.toString(var5));
            return var2;
         }
      }

      return null;
   }

   private ReplacementBlock getBlockByKey(String var1) throws GenerateException {
      for (int var2 = 0; var2 < this.allBlocks.size(); var2++) {
         if (((ReplacementBlock)this.allBlocks.get(var2)).key.equals(var1)) {
            return (ReplacementBlock)this.allBlocks.get(var2);
         }
      }

      for (int var3 = 0; var3 < this.priceBlocks.size(); var3++) {
         if (((ReplacementBlock)this.priceBlocks.get(var3)).key.equals(var1)) {
            return (ReplacementBlock)this.priceBlocks.get(var3);
         }
      }

      for (int var4 = 0; var4 < this.priceRangeBlocks.size(); var4++) {
         if (((ReplacementBlock)this.priceRangeBlocks.get(var4)).key.equals(var1)) {
            return (ReplacementBlock)this.priceRangeBlocks.get(var4);
         }
      }

      throw new GenerateException(String.format("Cannot find block with key %s", var1));
   }

   double generateValueFromRandomValue(String var1, ItemRandomValueRanges var2, IRandomGenerator var3, boolean var4) {
      if (var2 == null) {
         return var4 ? Double.MAX_VALUE : 2.147483647E9;
      } else {
         String var5 = var2.getForParam(var1);
         if (var5 == null) {
            return var4 ? Double.MAX_VALUE : 2.147483647E9;
         } else {
            RandomValueConfig var6 = this.getRandomValueConfig(var5, var4);
            if (!var6.isValid()) {
               return var4 ? Double.MAX_VALUE : 2.147483647E9;
            } else {
               return var6.generateRandomValue(var3, var4);
            }
         }
      }
   }

   private RandomValueConfig getRandomValueConfig(String var1, boolean var2) {
      if (this.cachedRV == null || !this.cachedRV.equals(var1)) {
         this.cachedRV = var1;
         this.cachedRVConfig = this.createRandomValueConfig(var1, var2);
      }

      return this.cachedRVConfig;
   }

   private RandomValueConfig createRandomValueConfig(String var1, boolean var2) {
      RandomValueConfig var3 = RandomValueConfig.parseRandomValue(var1, var2);
      if (var3 != null) {
         return var3;
      }

      Log.error("Error parsing randomValue '" + var1 + "' in strategy template.");
      return new RandomValueConfig(false);
   }

   public int getDecimals(String var1, ItemRandomValueRanges var2, int var3) {
      if (var2 == null) {
         return var3;
      }

      String var4 = var2.getForParam(var1);
      if (var4 == null) {
         return var3;
      }

      RandomValueConfig var5 = this.getRandomValueConfig(var4, true);
      return !var5.isValid() ? var3 : var5.getDecimals(var3);
   }

   public int getDecimals(double var1, int var3) {
      BigDecimal var4 = BigDecimal.valueOf(var1);
      int var5 = var4.scale();
      int var6 = var3;
      if (var5 > var6) {
         var6 = var5;
      }

      if (var6 > 6) {
         var6 = 6;
      }

      return var6;
   }

   public ReplacementBlock findBlockByKey(String var1) {
      for (int var2 = 0; var2 < this.allBlocks.size(); var2++) {
         ReplacementBlock var3 = (ReplacementBlock)this.allBlocks.get(var2);
         if (var3.blockDef.key.equals(var1)) {
            return var3;
         }
      }

      return null;
   }

   public boolean checkPriceRangeBlockExists() {
      return this.priceRangeBlocks.size() > 0;
   }

   public boolean verifyItem(String var1, Element var2) throws GenerateException {
      ReplacementBlock var3 = this.getBlockByKey(var1);
      return var3.verifyItem(var2);
   }
}
