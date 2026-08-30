package com.strategyquant.tradinglib.blocks;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Output;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import org.jdom2.Element;

public class OutputAnnotationHandler extends AnnotationHandler {
   public static void wizardParseXml(Output var0, IBlock var1, Field var2, Element var3) throws BlockDefinitionException {
      if (!var3.getName().equals("Item")) {
         throw new BlockDefinitionException("@Output must be used on Item node!");
      }

      if (!var2.getType().isAssignableFrom(DataSeries.class)) {
         throw new BlockDefinitionException("@Output parameter must have DataSeries type!");
      }

      Class var4 = var1.getClass();
      int var5 = countOutputs(var4);
      if (var5 > 1) {
         int var6 = (int)getParamNumberValue(var1, var2, var3.getChildren(), "#Line#");

         for (Field var10 : var4.getFields()) {
            if (var10.getName().equals("OutputIndex")) {
               setField(var1, var10, var6);
            }
         }
      }
   }

   public static void wizardGenerateXml(Class<?> var0, Element var1, IBlock var2, Class<?> var3, boolean var4) throws BlockDefinitionException {
      int var5 = 0;
      StringBuilder var6 = new StringBuilder();

      for (Field var10 : var0.getFields()) {
         for (Annotation var14 : var10.getAnnotations()) {
            if (var14 instanceof Output) {
               if (var6.length() != 0) {
                  var6.append(",");
               }

               var6.append(getFieldName(((Output)var14).name(), var10.getName()));
               var6.append("=");
               var6.append(var5);
               var5++;
               break;
            }
         }
      }

      if (var5 > 1) {
         Element var16 = getOrCreateParamCategory(var1, "Line", var4);
         Element var17 = new Element("Param");
         var17.setAttribute("key", "#Line#");
         var17.setAttribute("name", "Line");
         var17.setAttribute("type", "int");
         var17.setAttribute("controlType", Editors.convertToWizardConstant(40));
         if (!var6.toString().equals("Null")) {
            var17.setAttribute("values", var6.toString());
         }

         var17.setAttribute("defaultValue", "0");
         if (var4) {
            Field var18 = null;

            for (Field var25 : var0.getFields()) {
               if (var25.getName().equals("OutputIndex")) {
                  var18 = var25;
               }
            }

            if (var18 == null) {
               throw new BlockDefinitionException("There is no OutputIndex field in this block object!");
            }

            var17.removeAttribute("help");
            String var20 = null;

            try {
               String var22 = var18.getType().getName();
               switch (var22) {
                  case "int":
                     var20 = Integer.toString(var18.getInt(var2));
                     break;
                  case "long":
                     var20 = Long.toString(var18.getLong(var2));
                     break;
                  case "double":
                     var20 = SQUtils.doubleToString(var18.getDouble(var2));
                     break;
                  case "boolean":
                     var20 = Boolean.toString(var18.getBoolean(var2));
                     break;
                  case "java.lang.String":
                     var20 = (String)var18.get(var2);
                     break;
                  case "java.lang.Object":
                     throw new BlockDefinitionException("Cannot set value of type Object!");
               }
            } catch (IllegalArgumentException | IllegalAccessException var15) {
               throw new BlockDefinitionException("Exception", var15);
            }

            if (var20 != null) {
               var17.addContent(var20);
            }
         }

         var16.addContent(var17);
      }
   }

   private static int countOutputs(Class<?> var0) {
      int var1 = 0;

      for (Field var5 : var0.getFields()) {
         for (Annotation var9 : var5.getAnnotations()) {
            if (var9 instanceof Output) {
               var1++;
            }
         }
      }

      return var1;
   }
}
