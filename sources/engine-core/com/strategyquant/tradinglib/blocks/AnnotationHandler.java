package com.strategyquant.tradinglib.blocks;

import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Variable;
import java.lang.reflect.Field;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AnnotationHandler {
   private static final Logger Log = LoggerFactory.getLogger(AnnotationHandler.class);

   public static String decorateFieldName(String var0) {
      StringBuilder var1 = new StringBuilder();
      var1.append("#");
      var1.append(var0);
      var1.append("#");
      return var1.toString();
   }

   protected static double getParamNumberValue(IBlock var0, Field var1, List<Element> var2, String var3) throws BlockDefinitionException {
      Element var4 = getParameter(var0, var2, var3);
      String var5 = var4.getAttributeValue("variable");
      String var6 = var4.getAttributeValue("customParam");
      if (var5 != null && var5.equals("true") || var6 != null && var6.equals("true")) {
         String var7 = var4.getValue();
         if (var0.getStrategy() != null) {
            Variable var8 = null;
            if (var5 != null && var5.equals("true")) {
               var8 = var0.getStrategy().variables().getById(var7);
            }

            if (var6 != null) {
               String var9 = var4.getText();
               var8 = new Variable(var9, var9, "customParam", "0", true, false);
               var0.getStrategy().variables().add(var8);
            }

            try {
               var8.registerAttachedField(var0, var1);
            } catch (NullPointerException var10) {
               Log.error("AnnotationHandler exception", var10);
            }

            return Double.parseDouble(var8.getValue());
         } else {
            throw new BlockDefinitionException("Strategy is null for block!");
         }
      } else {
         return Double.parseDouble(var4.getValue());
      }
   }

   protected static double getParamChartTFValue(IBlock var0, Field var1, List<Element> var2, String var3) throws BlockDefinitionException {
      Element var4 = getParameter(var0, var2, var3);
      String var5 = var4.getAttributeValue("chartTF");
      if (var5 != null && !var5.equals("")) {
         switch (var5) {
            case "D1":
               return 1.0;
            case "W1":
               return 2.0;
            case "M1":
               return 3.0;
            default:
               return 0.0;
         }
      } else {
         return 0.0;
      }
   }

   protected static Element getParameter(IBlock var0, List<Element> var1, String var2) throws BlockDefinitionException {
      for (Element var4 : var1) {
         if (var4.getName().equals("Param")) {
            String var5 = var4.getAttributeValue("key");
            if (var5 == null) {
               throw new BlockDefinitionException("Block '" + var0.getClass().getSimpleName() + "' - Param doesn't have 'key' attribute!");
            }

            if (var5.equalsIgnoreCase(var2)) {
               return var4;
            }
         } else {
            if (!var4.getName().equals("Block")) {
               throw new BlockDefinitionException(
                  "Block '"
                     + var0.getClass().getSimpleName()
                     + "' - Item parameter should have name <Param> or <Block>, it is <"
                     + var4.getName()
                     + "> instead!"
               );
            }

            String var6 = var4.getAttributeValue("key");
            if (var6 == null) {
               throw new BlockDefinitionException("Block '" + var0.getClass().getSimpleName() + "' - Block doesn't have 'key' attribute!");
            }

            if (var6.equalsIgnoreCase(var2)) {
               throw new BlockDefinitionException("getParameter() shouldn't be used for <Block> parameter!");
            }
         }
      }

      return null;
   }

   protected static void setField(IBlock var0, Field var1, Object var2) {
      try {
         var1.set(var0, var2);
      } catch (IllegalArgumentException var4) {
         Log.error(String.format("Failed to set field '%s' to value '%s'.", var1.getName(), var2 == null ? "null" : var2.toString()));
      } catch (IllegalAccessException var5) {
         Log.error(String.format("Failed to set field '%s' to value '%s'.", var1.getName(), var2 == null ? "null" : var2.toString()));
      }
   }

   public static String getFieldName(String var0, String var1) {
      return !var0.equals("Null") ? var0 : var1;
   }

   protected static Element getOrCreateParamCategory(Element var0, String var1, boolean var2) {
      if (var2) {
         return var0;
      }

      for (Element var4 : var0.getChildren("paramCategory")) {
         if (var4.getAttributeValue("name").equals(var1)) {
            return var4;
         }
      }

      Element var5 = new Element("paramCategory");
      var5.setAttribute("name", var1);
      var5.setAttribute("expanded", "true");
      var0.addContent(var5);
      return var5;
   }
}
