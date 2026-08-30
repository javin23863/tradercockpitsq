package com.strategyquant.tradinglib.blocks;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.dataseries.DataSeriesTypes;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.blocks.annotations.DataSeriesChoice;
import com.strategyquant.tradinglib.exit.ExitMethodsList;
import com.strategyquant.tradinglib.formulas.Formulas;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ParameterAnnotationParser extends AnnotationHandler {
   private static final Logger Log = LoggerFactory.getLogger(ParameterAnnotationParser.class);

   public static void wizardParseXml(Parameter var0, IBlock var1, Field var2, Element var3) throws BlockDefinitionException {
      if (!var3.getName().equals("Item") && !var3.getName().equals("Formula")) {
         throw new BlockDefinitionException("@Parameter must be used only on Item or Formula node!");
      }

      String var4 = var2.getType().getName();
      String var5 = decorateFieldName(var2.getName());
      List var6 = var3.getChildren();
      if (!var4.equals("int") && !var4.equals("long") && !var4.equals("double") && !var4.equals("boolean") && !var4.equals("java.lang.String")) {
         if (var2.getType().isAssignableFrom(Variable.class)) {
            wizardParseVariable(var1, var2, var6, var5);
         } else if (var2.getType().isAssignableFrom(DataSeries.class)) {
            wizardParseDataSeries(var1, var2, var6, var5);
         } else if (var2.getType().isAssignableFrom(ChartData.class)) {
            wizardParseChartData(var1, var2, var6, var5);
         } else if (var4.equals("com.strategyquant.tradinglib.IFormula")) {
            wizardParseFormula(var1, var2, var6, var5, var3);
         } else if (var4.equals("com.strategyquant.tradinglib.IBlock")) {
            wizardParseBlock(var1, var2, var6, var5);
         } else if (var4.equals("[Lcom.strategyquant.tradinglib.IBlock;")) {
            wizardParseBlocks(var1, var2, var6, var5);
         } else {
            if (!var2.getType().toString().equals("class [Lcom.strategyquant.tradinglib.ExitMethod;")) {
               throw new BlockDefinitionException("Param with name '" + var5 + "' has an unknown type '" + var4 + "' in block " + var1 + "!");
            }

            wizardParseExitMethods(var1, var2, var6, var5);
         }
      } else {
         wizardParsePrimitiveType(var1, var2, var6, var5, var4);
      }
   }

   private static void wizardParseVariable(IBlock var0, Field var1, List<Element> var2, String var3) throws BlockDefinitionException {
      Element var4 = getParameter(var0, var2, var3);
      String var5 = var4.getAttributeValue("variable");
      if (var5 != null && var5.equals("true")) {
         String var6 = var4.getValue();
         if (var0.getStrategy() != null) {
            Variable var7 = var0.getStrategy().variables().getById(var6);
            setField(var0, var1, var7);
         } else {
            throw new BlockDefinitionException("Strategy cannot be null when using Variable!");
         }
      } else {
         throw new BlockDefinitionException("Field is not a variable!");
      }
   }

   private static void wizardParseExitMethods(IBlock var0, Field var1, List<Element> var2, String var3) throws BlockDefinitionException {
      if (!var1.getType().isAssignableFrom(ExitMethod[].class)) {
         Class var12 = var0.getClass();
         throw new BlockDefinitionException("Field " + var12.getSimpleName() + "." + var1.getName() + " must have ExitMethod[] type!");
      }

      HashMap var4 = new HashMap();

      for (Element var6 : var2) {
         String var7 = var6.getAttributeValue("exitMethod");
         if (var7 != null && var7.equals("true")) {
            String var8 = var6.getAttributeValue("key");
            var8 = var8.replace("#", "");
            String[] var9 = var8.split("\\.");
            if (var9.length != 2) {
               throw new BlockDefinitionException(String.format("Key for exit method has incorrect format '%s'", var8));
            }

            String var10 = var9[0];
            if (!var4.containsKey(var10)) {
               var4.put(var10, new ArrayList());
            }

            ArrayList var11 = (ArrayList)var4.get(var10);
            var11.add(var6);
         }
      }

      if (!var4.isEmpty()) {
         ArrayList var13 = new ArrayList();

         for (Entry var16 : var4.entrySet()) {
            String var19 = (String)var16.getKey();
            ArrayList var20 = (ArrayList)var16.getValue();
            ExitMethod var21 = ExitMethodsList.getExitMethodObject(var19, var0.getStrategy(), var20);
            var13.add(var21);
         }

         ExitMethod[] var15 = new ExitMethod[var13.size()];

         for (int var17 = 0; var17 < var13.size(); var17++) {
            var15[var17] = (ExitMethod)var13.get(var17);
         }

         setField(var0, var1, var15);
      }
   }

   private static void wizardParseBlocks(IBlock var0, Field var1, List<Element> var2, String var3) throws BlockDefinitionException {
      if (!var1.getType().isAssignableFrom(IBlock[].class)) {
         Class var7 = var0.getClass();
         throw new BlockDefinitionException("Field " + var7.getSimpleName() + "." + var1.getName() + " must have Block type!");
      }

      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < var2.size(); var5++) {
         Element var6 = (Element)var2.get(var5);
         if (var6.getName().equals("Block")) {
            var4.add(parseSubBlock(var0.getStrategy(), var6.getChild("Item")));
         } else if (var6.getName().equals("Item")) {
            var4.add(parseSubBlock(var0.getStrategy(), var6));
         }
      }

      IBlock[] var8 = new IBlock[var4.size()];

      for (int var9 = 0; var9 < var4.size(); var9++) {
         var8[var9] = (IBlock)var4.get(var9);
      }

      setField(var0, var1, var8);
   }

   public static IBlock parseSubBlock(StrategyBase var0, Element var1) throws BlockDefinitionException {
      if (var1 == null) {
         throw new BlockDefinitionException("Block doesn't contain item 1");
      } else {
         return Blocks.getBlockObject(var1.getAttributeValue("key"), var0, var1);
      }
   }

   private static void wizardParseBlock(IBlock var0, Field var1, List<Element> var2, String var3) throws BlockDefinitionException {
      if (!var1.getType().isAssignableFrom(IBlock.class)) {
         Class var9 = var0.getClass();
         throw new BlockDefinitionException("Field " + var9.getSimpleName() + "." + var1.getName() + " must have Block type!");
      }

      String var4 = decorateFieldName(var1.getName());

      for (int var5 = 0; var5 < var2.size(); var5++) {
         Element var6 = (Element)var2.get(var5);
         if (isBlock(var0, var6)) {
            String var7 = var6.getAttributeValue("key");
            if (var7 == null) {
               throw new BlockDefinitionException("Block doesn't have 'key' attribute!");
            }

            if (var7.equalsIgnoreCase(var4)) {
               Element var8 = var6.getChild("Item");
               setField(var0, var1, parseSubBlock(var0.getStrategy(), var8));
            }
         }
      }
   }

   private static boolean isBlock(IBlock var0, Element var1) {
      if (var1.getName().equals("Block")) {
         return true;
      }

      if (var1.getName().equals("Param")) {
         String var2 = var1.getAttributeValue("key");
         if (var2 == null) {
            return false;
         }

         Class var3 = var0.getClass().getSuperclass();
         if (var3 == null) {
            return false;
         }

         String var4 = var3.getSimpleName();
         if (var4 == null || !var4.equals("LeftRightComparisonBlockAbstract")) {
            return false;
         }

         if (var2.equals("#Left#") || var2.equals("#Right#")) {
            return true;
         }
      }

      return false;
   }

   private static void wizardParsePrimitiveType(IBlock var0, Field var1, List<Element> var2, String var3, String var4) throws BlockDefinitionException {
      Element var5 = getParameter(var0, var2, var3);
      if (var5 != null) {
         String var6 = null;
         String var7 = var5.getAttributeValue("variable");
         String var8 = var5.getAttributeValue("customParam");
         if (var7 != null && var7.equals("true") || var8 != null && var8.equals("true")) {
            String var9 = var5.getValue();
            if (var0.getStrategy() != null) {
               Variable var10 = null;
               if (var7 != null && var7.equals("true")) {
                  var10 = var0.getStrategy().variables().getById(var9);
               } else if (var8 != null) {
                  String var11 = var5.getText();
                  var10 = new Variable(var11, var11, "customParam", "0", true, false);
                  var0.getStrategy().variables().add(var10);
               }

               if (var10 == null) {
                  String var14 = var5.getAttributeValue("key");
                  if (var14.contains("MagicNumber")) {
                     var10 = var0.getStrategy().variables().get("MagicNumber");
                     if (var10 != null) {
                        var5.setText(var10.getId());
                     }
                  }

                  if (var10 == null) {
                     throw new BlockDefinitionException(String.format("No Variable with ID '%s' defined!", var9));
                  }
               }

               var10.registerAttachedField(var0, var1);
               var6 = var10.getValue();
            }
         } else {
            var6 = var5.getValue();
         }

         if (var6 != null) {
            try {
               parseValue(var4, var0, var1, var6);
            } catch (NumberFormatException var12) {
               if (!var6.contains("--")) {
                  throw var12;
               }

               var6 = var6.replace("--", "-");
               parseValue(var4, var0, var1, var6);
            }
         }
      }
   }

   private static void parseValue(String var0, IBlock var1, Field var2, String var3) throws BlockDefinitionException {
      try {
         if (var0.equals("int")) {
            if (var3.equalsIgnoreCase("Any")) {
               setField(var1, var2, 0);
            } else if (var3.contains(":")) {
               String[] var4 = var3.split(":");
               setField(var1, var2, Integer.parseInt(var4[0]) * 100 + Integer.parseInt(var4[1]));
            } else {
               setField(var1, var2, (int)Double.parseDouble(var3));
            }
         } else if (var0.equals("long")) {
            setField(var1, var2, Long.parseLong(var3));
         } else if (var0.equals("double")) {
            setField(var1, var2, Double.parseDouble(var3));
         } else if (var0.equals("boolean")) {
            setField(var1, var2, Boolean.parseBoolean(var3));
         } else if (var0.equals("java.lang.String")) {
            setField(var1, var2, var3);
         }
      } catch (Exception var5) {
         Log.error(String.format("Failed to set param '%s/%s', value '%s'.", var1.getClass().getSimpleName(), var2.getName(), var3));
         throw new BlockDefinitionException(
            String.format("Failed to set param '%s/%s', value '%s'.", var1.getClass().getSimpleName(), var2.getName(), var3), var5
         );
      }
   }

   private static void wizardParseFormula(IBlock var0, Field var1, List<Element> var2, String var3, Element var4) throws BlockDefinitionException {
      if (!var1.getType().isAssignableFrom(IFormula.class)) {
         Class var14 = var0.getClass();
         throw new BlockDefinitionException("Field " + var14.getSimpleName() + "." + var1.getName() + " must have Formula type!");
      }

      for (int var6 = 0; var6 < var2.size(); var6++) {
         Element var7 = (Element)var2.get(var6);
         boolean var8 = false;
         if (var7.getName().equals("Param")) {
            String var9 = var7.getAttributeValue("key");
            if (var9 == null) {
               throw new BlockDefinitionException("Param doesn't have 'key' attribute!");
            }

            String var10 = decorateFieldName(var1.getName());
            if (var9.equalsIgnoreCase(var10)) {
               var8 = true;
            } else if (var9.contains(".")) {
               String[] var11 = var9.split("\\.");
               if (var11.length != 2) {
                  throw new BlockDefinitionException(String.format("Key for exit method has incorrect format '%s'", var9));
               }

               var9 = var11[0] + "#";
               if (var9.equalsIgnoreCase(var10)) {
                  var8 = true;
               }
            }

            if (var8) {
               Element var17 = var7.getChild("Formula");
               if (var17 == null) {
                  throw new BlockDefinitionException("Formula parameter '" + var1.getName() + "' doesn't have Formula child!");
               }

               String var12 = var17.getAttributeValue("key");
               if (var12 == null) {
                  throw new BlockDefinitionException("CompositeValue doesn't have 'key' attribute!");
               }

               IFormula var5 = Formulas.get(var12);
               var5 = var5.newFormulaInstance(var0.getStrategy(), var17);
               setField(var0, var1, var5);
               return;
            }
         }
      }

      if (!var1.getType().isAssignableFrom(IFormula.class)) {
         Class var15 = var0.getClass();
         throw new BlockDefinitionException("Field " + var15.getSimpleName() + "." + var1.getName() + " must have Formula type!");
      } else {
         throw new BlockDefinitionException("Error parsing formula!");
      }
   }

   private static void wizardParseDataSeries(IBlock var0, Field var1, List<Element> var2, String var3) throws BlockDefinitionException {
      if (var3.equals("#Input#")) {
         var3 = "#Chart#";
      }

      int var4 = (int)getParamNumberValue(var0, var1, var2, var3);
      int var5 = (int)getParamChartTFValue(var0, var1, var2, var3);
      String var6 = "ComputedFrom";
      DataSeriesChoice var7 = var1.getAnnotation(DataSeriesChoice.class);
      if (var7 != null) {
         var6 = var7.key();
      }

      int var8 = (int)getParamNumberValue(var0, var1, var2, decorateFieldName(var6));

      try {
         var0.setCustomData(var1.getName() + "ChartIndex", var4);
         var0.setCustomData(var1.getName() + "ComputedFrom", var8);
         var0.setCustomData(var1.getName() + "ChartTF", var5);
         if (var0.getStrategy() != null && var0.getStrategy().MarketData != null) {
            setField(var0, var1, var0.getStrategy().MarketData.Chart(var4).getSeries(var8, var5));
         }
      } catch (Exception var10) {
         throw new BlockDefinitionException("Exception in getting DataSeries for chart: " + var4 + ", and series: " + DataSeriesTypes.toString(var8));
      }
   }

   private static void wizardParseChartData(IBlock var0, Field var1, List<Element> var2, String var3) throws BlockDefinitionException {
      if (var3.equals("#Input#")) {
         var3 = "#Chart#";
      }

      int var4 = (int)getParamNumberValue(var0, var1, var2, var3);
      int var5 = (int)getParamChartTFValue(var0, var1, var2, var3);

      try {
         var0.setCustomData("StockpickerChartIndex", var4);
         var0.setCustomData(var1.getName() + "ChartIndex", var4);
         var0.setCustomData(var1.getName() + "ChartTF", var5);
         if (var0.getStrategy() != null && var0.getStrategy().MarketData != null) {
            ChartData var6 = var0.getStrategy().MarketData.Chart(var4);
            if (var5 != 0) {
               var6 = var6.cloneOnlyTF(var5);
            }

            setField(var0, var1, var6);
         }
      } catch (Exception var8) {
         var0.setCustomData(var1.getName() + "ChartIndex", var4);
         if (var0.getStrategy() != null && var0.getStrategy().MarketData != null) {
            ChartData var7 = var0.getStrategy().MarketData.Chart(var4);
            if (var5 != 0) {
               var7 = var7.cloneForTF(var5);
            }

            setField(var0, var1, var7);
         }

         throw new BlockDefinitionException("Exception in getting DataSeries for chart: " + var4);
      }
   }
}
