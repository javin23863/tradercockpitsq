package com.strategyquant.tradinglib.blocks;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.Required;
import com.strategyquant.tradinglib.SLPTValue;
import com.strategyquant.tradinglib.Variable;
import com.strategyquant.tradinglib.blocks.annotations.DataSeriesChoice;
import com.strategyquant.tradinglib.exit.ExitMethodsList;
import java.lang.reflect.Field;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterAnnotationGenerator extends AnnotationHandler {
   public static final Logger Log = LoggerFactory.getLogger("ParameterAnnotationGenerator");

   public static void wizardGenerateXml(Parameter var0, Field var1, Element var2, IBlock var3, Class<?> var4, boolean var5, int var6, String var7, Element var8) throws BlockDefinitionException {
      String var9 = var1.getType().getName();
      if (var9.equals("int")
         || var9.equals("long")
         || var9.equals("double")
         || var9.equals("boolean")
         || var9.equals("java.lang.String")
         || var9.equals("java.lang.Object")
         || var9.equals("com.strategyquant.tradinglib.Variable")) {
         wizardGeneratePrimitiveTypeXml(var0, var1, var2, var3, var4, var9, var5, var6, var8);
      } else if (var9.equals("com.strategyquant.tradinglib.IFormula")) {
         wizardGenerateFormulaXml(var0, var1, var2, var3, var4, var9, var5, var6, var7, null);
      } else if (var9.equals("com.strategyquant.tradinglib.IBlock")) {
         wizardGenerateBlock(var0, var1, var2, var3, var4, var9, var5);
      } else if (var9.equals("[Lcom.strategyquant.tradinglib.IBlock;")) {
         wizardGenerateBlocks(var0, var1, var2, var3, var4, var9, var5);
      } else if (var1.getType().isAssignableFrom(ChartData.class)) {
         wizardGenerateChartDataXml(var0, var1, var2, var3, var4, var5);
      } else if (var1.getType().isAssignableFrom(DataSeries.class)) {
         wizardGenerateChartDataXml(var0, var1, var2, var3, var4, var5);
         wizardGenerateComputeFromXml(var0, var1, var2, var3, var4, var5);
      } else if (!var1.getType().isAssignableFrom(TimeDataSeries.class)) {
         if (!var1.getType().toString().equals("class [Lcom.strategyquant.tradinglib.ExitMethod;")) {
            throw new BlockDefinitionException("Unsupported parameter type " + var1.getType() + " in block '" + var3.getClass().getSimpleName() + "'!");
         }

         try {
            wizardGenerateExitMethodParams(var1, var2, (ExitMethod[])var1.get(var3), var4, var5);
         } catch (IllegalArgumentException | IllegalAccessException var11) {
            Log.error("Parameter is not an ExitMethods array!", var11);
            throw new BlockDefinitionException("Parameter is not an ExitMethods array!");
         }
      }
   }

   private static void wizardGenerateBlocks(Parameter var0, Field var1, Element var2, IBlock var3, Class<?> var4, String var5, boolean var6) throws BlockDefinitionException {
      if (var6) {
         IBlock[] var7;
         try {
            var7 = (IBlock[])var1.get(var3);
         } catch (IllegalArgumentException | IllegalAccessException var13) {
            throw new BlockDefinitionException("Error getting blocks", var13);
         }

         for (IBlock var11 : var7) {
            Element var12 = new Element("Block");
            var12.addContent(Blocks.generateBlockTreeXml(var11));
            var2.addContent(var12);
         }
      }
   }

   private static void wizardGenerateBlock(Parameter var0, Field var1, Element var2, IBlock var3, Class<?> var4, String var5, boolean var6) throws BlockDefinitionException {
      if (var6) {
         IBlock var7;
         try {
            var7 = (IBlock)var1.get(var3);
         } catch (IllegalArgumentException | IllegalAccessException var9) {
            throw new BlockDefinitionException("Error getting block", var9);
         }

         Element var8 = new Element("Block");
         var8.setAttribute("key", decorateFieldName(var1.getName()));
         var8.addContent(Blocks.generateBlockTreeXml(var7));
         var2.addContent(var8);
      } else {
         Element var10 = getOrCreateParamCategory(var2, var0.category(), var6);
         Element var11 = new Element("Param");
         var11.setAttribute("key", decorateFieldName(var1.getName()));
         var11.setAttribute("name", getFieldName(var0.name(), SQUtils.insertUppercaseSpaces(var1.getName())));
         var11.setAttribute("type", "value");
         if (isPriceFormula(var3)) {
            var11.setAttribute("valueType", "price");
         }

         var11.setAttribute("controlType", "value");
         if (!var0.postfix().equals("Null")) {
            var11.setAttribute("postfix", var0.postfix());
         }

         if (!var0.standsFor().equals("Null")) {
            var11.setAttribute("standsFor", var0.standsFor());
         }

         var10.addContent(var11);
      }
   }

   static void wizardGenerateExitMethodParams(Field var0, Element var1, ExitMethod[] var2, Class<?> var3, boolean var4) throws BlockDefinitionException {
      Element var5 = getOrCreateParamCategory(var1, "Exit methods", var4);

      for (ExitMethod var7 : ExitMethodsList.get().getAvailableClasses()) {
         var7.generateXmlParams(var5, var0, var1, var2, var4);
      }
   }

   private static boolean isPriceFormula(IBlock var0) {
      Formula var1 = var0.getClass().getAnnotation(Formula.class);
      return var1 != null && var1.priceLevel();
   }

   public static void wizardGenerateFormulaXml(
      Parameter var0, Field var1, Element var2, IBlock var3, Class<?> var4, String var5, boolean var6, int var7, String var8, Element var9
   ) throws BlockDefinitionException {
      Element var10 = null;
      Element var11 = var9;
      if (var9 == null) {
         var10 = getOrCreateParamCategory(var2, var0.category(), var6);
         var11 = new Element("Param");
         var11.setAttribute("key", decorateFieldName(var1.getName()));
         var11.setAttribute("name", getFieldName(var0.name(), SQUtils.insertUppercaseSpaces(var1.getName())));
      }

      var11.setAttribute("type", "double");
      var11.setAttribute("isFormula", "true");
      if (!var0.defaultValue().equals("Null")) {
         var11.setAttribute("defaultValue", var0.defaultValue());
      }

      if (!var0.showIfDefault()) {
         var11.setAttribute("displayOnDefault", "false");
      }

      Editor var12 = var1.getAnnotation(Editor.class);
      if (var12 == null) {
         throw new BlockDefinitionException("Formula type field must have the editor defined!");
      }

      if (var12.type() == 200) {
         var11.setAttribute("controlType", var12.formulaName());
      } else {
         var11.setAttribute("controlType", Editors.convertToWizardConstant(var12.type()));
      }

      String var13 = getHelpValue(var1);
      if (!var13.equals("Null")) {
         var11.setAttribute("help", var13);
      }

      Required var14 = var1.getAnnotation(Required.class);
      if (var14 != null) {
         var11.setAttribute("required", "true");
      }

      if (var7 != 0) {
         var11.setAttribute("exitMethod", "true");
         if (var8 != null) {
            var11.setAttribute("dependentOn", var8);
         }

         if (var7 == 2) {
            var11.setAttribute("exitMethodType", "SL");
         } else if (var7 == 3) {
            var11.setAttribute("exitMethodType", "PT");
         }
      }

      addForEngineAttribute(var11, var1);
      if (var9 == null) {
         var10.addContent(var11);
      }

      if (var6) {
         var11.addContent(generateFormulaForParam(var0, var1, var2, var3, var4, var5));
      }
   }

   private static Element generateFormulaForParam(Parameter var0, Field var1, Element var2, IBlock var3, Class<?> var4, String var5) throws BlockDefinitionException {
      Element var6 = new Element("Formula");

      try {
         Object var7 = var1.get(var3);
         if (var7 == null) {
            throw new BlockDefinitionException(
               String.format("Parameter '%s' in exit method/block '%s' is null!", var1.getName(), var3.getClass().getSimpleName())
            );
         }

         var6.setAttribute("key", var7.getClass().getCanonicalName());
         Element var8 = Blocks.generateBlockTreeXml((IBlock)var7);
         if (var8 != null) {
            List var9 = var8.getChildren();
            int var10 = var9.size();

            for (int var11 = 0; var11 < var10; var11++) {
               var6.addContent(((Element)var9.get(0)).detach());
            }
         }

         return var6;
      } catch (IllegalArgumentException | IllegalAccessException var12) {
         throw new BlockDefinitionException("Exception generating XML for formula!", var12);
      }
   }

   private static String getHelpValue(Field var0) {
      Help var1 = var0.getAnnotation(Help.class);
      return var1 != null ? var1.value() : "Null";
   }

   private static void wizardGenerateComputeFromXml(Parameter var0, Field var1, Element var2, IBlock var3, Class<?> var4, boolean var5) {
      String var6 = "ComputedFrom";
      String var7 = "Computed From";
      int var8 = 0;
      String var9 = "Default";
      String var10 = null;
      DataSeriesChoice var11 = var1.getAnnotation(DataSeriesChoice.class);
      if (var11 != null) {
         var6 = var11.key();
         var7 = var11.name();
         var8 = var0.defaultSeries();
         var9 = var11.category();
         var10 = var11.help();
      }

      Element var12 = getOrCreateParamCategory(var2, var9, var5);
      Element var13 = new Element("Param");
      var13.setAttribute("key", decorateFieldName(var6));
      var13.setAttribute("name", var7);
      var13.setAttribute("type", "int");
      var13.setAttribute("controlType", "combo");
      var13.setAttribute("values", "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6");
      var13.setAttribute("defaultValue", String.valueOf(var8));
      if (var10 != null) {
         var13.setAttribute("help", var10);
      }

      if (var5) {
         int var14 = var3.getCustomData(var1.getName() + "ComputedFrom");
         var13.addContent(Integer.toString(var14));
      }

      var12.addContent(var13);
   }

   private static void wizardGenerateChartDataXml(Parameter var0, Field var1, Element var2, IBlock var3, Class<?> var4, boolean var5) {
      Element var6 = getOrCreateParamCategory(var2, var0.category(), var5);
      Element var7 = new Element("Param");
      var7.setAttribute("key", decorateFieldName("Chart"));
      var7.setAttribute("name", !var0.name().equals("Null") ? var0.name() : "Chart");
      var7.setAttribute("type", "data");
      var7.setAttribute("controlType", "dataVar");
      var7.setAttribute("defaultValue", String.valueOf(var0.defaultChartIndex()));
      String var8 = getHelpValue(var1);
      if (!var8.equals("Null")) {
         var7.setAttribute("help", var8);
      }

      if (var5) {
         int var9 = var3.getCustomData(var1.getName() + "ChartIndex");
         var7.addContent(Integer.toString(var9));
         if (var3.getStrategy() != null) {
            Variable var10 = var3.getStrategy().variables().getByField(var3, var1.getName());
            if (var10 != null) {
               var7.setAttribute("variable", "true");
               var7.setAttribute("variableType", var10.translateType(var10.getInternalType()));
               var7.setText(var10.getId());
            }
         }
      }

      var6.addContent(var7);
   }

   private static void wizardGeneratePrimitiveTypeXml(
      Parameter var0, Field var1, Element var2, IBlock var3, Class<?> var4, String var5, boolean var6, int var7, Element var8
   ) throws BlockDefinitionException {
      Element var9 = null;
      Element var10 = var8;
      if (var8 == null) {
         var9 = getOrCreateParamCategory(var2, var0.category(), var6);
         var10 = new Element("Param");
         var10.setAttribute("key", decorateFieldName(var1.getName()));
         var10.setAttribute("name", getFieldName(var0.name(), SQUtils.insertUppercaseSpaces(var1.getName())));
      }

      if (var1.getName().equals("Shift")) {
         NoShift var11 = var4.getAnnotation(NoShift.class);
         if (var11 != null) {
            return;
         }
      }

      if (var5.equals("java.lang.String")) {
         var10.setAttribute("type", "string");
      } else if (!var5.equals("java.lang.Object") && !var5.equals("com.strategyquant.tradinglib.Variable")) {
         var10.setAttribute("type", var5);
      } else {
         var10.setAttribute("type", "none");
      }

      String var19 = getDefaultValue(var1, var0.defaultValue());
      if (var19.endsWith(".0")) {
         var19 = var19.substring(0, var19.length() - 2);
      }

      if (!var5.equals("java.lang.Object") && !var5.equals("com.strategyquant.tradinglib.Variable") && !var19.equals("Null")) {
         var10.setAttribute("defaultValue", var19);
      }

      if (!var0.showIfDefault()) {
         var10.setAttribute("displayOnDefault", "false");
      }

      wizardGenerateEditorAttributes(var10, var0, var1, var5);
      String var12 = getHelpValue(var1);
      if (!var12.equals("Null")) {
         var10.setAttribute("help", var12);
      }

      if (!var0.postfix().equals("Null")) {
         var10.setAttribute("postfix", var0.postfix());
      }

      Required var13 = var1.getAnnotation(Required.class);
      if (var13 != null) {
         var10.setAttribute("required", "true");
      }

      if (var7 > 0) {
         var10.setAttribute("exitMethod", "true");
      }

      SLPTValue var14 = getSLPTAnnotation(var1);
      if (var14 != null) {
         if (var7 == 2) {
            if (var14.value() == -1000) {
               var10.setAttribute("genMinValue", Integer.toString(-1000005));
               var10.setAttribute("genMaxValue", Integer.toString(-1000006));
            } else if (var14.value() == -2000) {
               var10.setAttribute("genMinValue", Integer.toString(-1000007));
               var10.setAttribute("genMaxValue", Integer.toString(-1000008));
            } else if (var14.value() == -3000) {
               var10.setAttribute("genMinValue", Integer.toString(-1000009));
               var10.setAttribute("genMaxValue", Integer.toString(-1000010));
            } else if (var14.value() == -4000) {
               var10.setAttribute("genMinValue", Integer.toString(-1000019));
               var10.setAttribute("genMaxValue", Integer.toString(-1000020));
            }
         }

         if (var7 == 3) {
            if (var14.value() == -1000) {
               var10.setAttribute("genMinValue", Integer.toString(-1000011));
               var10.setAttribute("genMaxValue", Integer.toString(-1000012));
            } else if (var14.value() == -2000) {
               var10.setAttribute("genMinValue", Integer.toString(-1000013));
               var10.setAttribute("genMaxValue", Integer.toString(-1000014));
            } else if (var14.value() == -3000) {
               var10.setAttribute("genMinValue", Integer.toString(-1000015));
               var10.setAttribute("genMaxValue", Integer.toString(-1000016));
            } else if (var14.value() == -4000) {
               var10.setAttribute("genMinValue", Integer.toString(-1000017));
               var10.setAttribute("genMaxValue", Integer.toString(-1000018));
            }
         }
      }

      if (var0.builderMinValue() != -9999999.0) {
         var10.setAttribute("builderMinValue", SQUtils.doubleToString(var0.builderMinValue()));
      }

      if (var0.builderMaxValue() != -9999999.0) {
         var10.setAttribute("builderMaxValue", SQUtils.doubleToString(var0.builderMaxValue()));
      }

      if (var0.builderStep() != -9999999.0) {
         var10.setAttribute("builderStep", SQUtils.doubleToString(var0.builderStep()));
      } else {
         var10.setAttribute("builderStep", SQUtils.doubleToString(var0.step()));
      }

      addForEngineAttribute(var10, var1);
      if (!var0.standsFor().equals("Null")) {
         var10.setAttribute("standsFor", var0.standsFor());
      }

      if (var6) {
         var10.removeAttribute("help");
         boolean var15 = false;
         if (var3.getStrategy() != null) {
            Variable var16 = var3.getStrategy().variables().getByField(var3, var1.getName());
            if (var16 != null) {
               var10.setAttribute("variable", "true");
               var10.setAttribute("variableType", var16.translateType(var16.getInternalType()));
               var10.addContent(var16.getId());
               var15 = true;
            }
         }

         if (!var15) {
            var19 = null;

            try {
               switch (var5) {
                  case "int":
                     var19 = Integer.toString(var1.getInt(var3));
                     break;
                  case "long":
                     var19 = Long.toString(var1.getLong(var3));
                     break;
                  case "double":
                     var19 = SQUtils.doubleToString(var1.getDouble(var3));
                     break;
                  case "boolean":
                     var19 = Boolean.toString(var1.getBoolean(var3));
                     break;
                  case "java.lang.String":
                     var19 = (String)var1.get(var3);
                     break;
                  case "java.lang.Object":
                     throw new BlockDefinitionException("Cannot set value of type Object!");
               }
            } catch (IllegalArgumentException | IllegalAccessException var18) {
               throw new BlockDefinitionException("Exception", var18);
            }

            if (var19 != null) {
               var10.addContent(var19);
            }
         }
      }

      if (var8 == null) {
         var9.addContent(var10);
      }
   }

   private static void addForEngineAttribute(Element var0, Field var1) {
      ForEngine var2 = var1.getAnnotation(ForEngine.class);
      if (var2 != null && !var2.value().equals("Null")) {
         String var3 = var2.value();
         var0.setAttribute("forEngine", var3);
      }
   }

   private static SLPTValue getSLPTAnnotation(Field var0) {
      return var0.getAnnotation(SLPTValue.class);
   }

   private static String getDefaultValue(Field var0, String var1) {
      if (!var1.equals("Null")) {
         return var1;
      } else if (var0.getName().equals("Shift")) {
         return "1";
      } else {
         return var0.getType().getName().equals("java.lang.String") ? "" : "10";
      }
   }

   private static void wizardGenerateEditorAttributes(Element var0, Parameter var1, Field var2, String var3) throws BlockDefinitionException {
      String var4 = var2.getName();
      boolean var6 = true;
      String var7 = null;
      if (var3.equals("int") && var4.contains("Shift")) {
         var0.setAttribute("controlType", "jspinnerVar");
         var0.setAttribute("minValue", "0");
         var0.setAttribute("maxValue", "1000");
         var0.setAttribute("genMinValue", SQUtils.doubleToString(-1000001.0));
         var0.setAttribute("genMaxValue", SQUtils.doubleToString(-1000002.0));
         var0.setAttribute("paramType", "shift");
         var0.setAttribute("step", "1");
         if (var0.getAttribute("defaultValue") == null) {
            var0.setAttribute("defaultValue", "1");
         }
      } else {
         if (var3.equals("int") && (var4.contains("Period") || var1.isPeriod())) {
            var0.setAttribute("genMinValue", SQUtils.doubleToString(-1000003.0));
            var0.setAttribute("genMaxValue", SQUtils.doubleToString(-1000004.0));
            var0.setAttribute("paramType", "period");
         }

         if (var3.equals("java.lang.String") && var4.equalsIgnoreCase("Symbol")) {
            if (var1.allowAny()) {
               var0.setAttribute("controlType", "symbolVarWithAny");
               var0.setAttribute("defaultValue", "Current");
            } else {
               var0.setAttribute("controlType", "symbolVar");
               var0.setAttribute("defaultValue", "Current");
            }

            var0.setAttribute("paramType", "symbol");
         } else {
            Editor var8 = var2.getAnnotation(Editor.class);
            int var5;
            if (var8 != null && var8.type() != 0) {
               if (var3.equals("java.lang.String")) {
                  if (var8.type() == 20) {
                     var5 = 30;
                  } else {
                     var5 = var8.type();
                  }
               } else if (var8.type() == 30) {
                  var5 = 20;
               } else {
                  var5 = var8.type();
               }

               var6 = var8.allowVariables();
               var7 = var8.values();
            } else if (var3.equals("java.lang.String")) {
               var5 = 30;
               var6 = false;
            } else if (var3.equals("boolean")) {
               var5 = 100;
               var6 = false;
            } else {
               var5 = 20;
               var6 = true;
            }

            var0.setAttribute("controlType", Editors.convertToWizardConstant(var5));
            if (!var3.equals("java.lang.String") && (var5 == 10 || var5 == 20)) {
               var0.setAttribute("minValue", SQUtils.doubleToString(var1.minValue()));
               var0.setAttribute("maxValue", SQUtils.doubleToString(var1.maxValue()));
               var0.setAttribute("step", SQUtils.doubleToString(var1.step()));
            }

            if (var7 != null && !var7.equals("Null")) {
               var0.setAttribute("values", var7);
            }
         }
      }
   }
}
