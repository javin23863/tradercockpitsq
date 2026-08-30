package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.ElementComparatorByAttribute;
import com.strategyquant.lib.XMLUtil;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcesCustomBlocks {
   private static final Logger Log = LoggerFactory.getLogger(ResourcesCustomBlocks.class);

   public static boolean areIdentical(Element var0, Element var1) throws Exception {
      try {
         Log.debug(
            String.format(
               "Comparing two CB with keys '%s'/'%s', names '%s'/'%s'...",
               var0.getAttributeValue("key"),
               var1.getAttributeValue("key"),
               XMLUtil.getAttr(var0, "name", "NA"),
               XMLUtil.getAttr(var1, "name", "NA")
            )
         );
         if (XMLUtil.getAttr(var0, "key").equals(XMLUtil.getAttr(var1, "key"))
            && XMLUtil.getAttr(var0, "oppositeBlockKey").equals(XMLUtil.getAttr(var1, "oppositeBlockKey"))
            && XMLUtil.getAttr(var0, "returnType").equals(XMLUtil.getAttr(var1, "returnType"))
            && XMLUtil.getAttr(var0, "type").equals(XMLUtil.getAttr(var1, "type"))
            && XMLUtil.getAttr(var0, "strategyType", "Standard").equals(XMLUtil.getAttr(var1, "strategyType", "Standard"))) {
            String var2 = XMLUtil.getAttr(var0, "key");
            Log.debug("CB attributes match, checking details ...");
            List var3 = var0.getChildren("Param");
            List var4 = var1.getChildren("Param");
            if (var3.size() != var4.size()) {
               Log.debug(String.format("CB %s has different Param count %d<>%d.", var2, var3.size(), var4.size()));
               return false;
            }

            for (int var5 = 0; var5 < var3.size(); var5++) {
               Element var6 = (Element)var3.get(var5);
               Element var7 = (Element)var4.get(var5);
               if (!XMLUtil.getAttr(var6, "key").equals(XMLUtil.getAttr(var7, "key"))) {
                  Log.debug(String.format("CB %s Param key mismatch '%s'<> '%s'.", var2, XMLUtil.getAttr(var6, "key"), XMLUtil.getAttr(var7, "key")));
                  return false;
               }

               String var8 = XMLUtil.getAttr(var6, "key");
               if (XMLUtil.getAttr(var6, "key").equals("#Chart1#") && XMLUtil.getAttr(var6, "defaultValue", "missing").equals("missing")) {
                  XMLUtil.trySetAttr(var6, "defaultValue", "0");
               }

               if (XMLUtil.getAttr(var7, "key").equals("#Chart1#") && XMLUtil.getAttr(var7, "defaultValue", "missing").equals("missing")) {
                  XMLUtil.trySetAttr(var7, "defaultValue", "0");
               }

               try {
                  if (!XMLUtil.getAttr(var6, "name").equals(XMLUtil.getAttr(var7, "name"))
                     || !XMLUtil.getAttr(var6, "type").equals(XMLUtil.getAttr(var7, "type"))
                     || !XMLUtil.getAttr(var6, "controlType").equals(XMLUtil.getAttr(var7, "controlType"))
                     || !XMLUtil.getAttr(var6, "defaultValue").equals(XMLUtil.getAttr(var7, "defaultValue"))
                     || !XMLUtil.getAttr(var6, "minValue", "NA").equals(XMLUtil.getAttr(var7, "minValue", "NA"))
                     || !XMLUtil.getAttr(var6, "maxValue", "NA").equals(XMLUtil.getAttr(var7, "maxValue", "NA"))
                     || !XMLUtil.getAttr(var6, "step", "NA").equals(XMLUtil.getAttr(var7, "step", "NA"))) {
                     Log.debug(
                        String.format(
                           "CB Param with key '%s/%s' has different attributes. Params:\n%s\n%s",
                           var2,
                           var8,
                           XMLUtil.elementLineToString(var6),
                           XMLUtil.elementLineToString(var7)
                        )
                     );
                     return false;
                  }
               } catch (Exception var18) {
                  Log.debug(String.format("CB Error while comparing Param with key '%s/%s'. Err: %s", var2, var8, var18.getMessage()), var18);
                  return false;
               }
            }

            Element var20 = var0.getChild("Contents");
            Element var21 = var1.getChild("Contents");
            List var22 = var20.getChildren("Item");
            List var23 = var21.getChildren("Item");
            if (var22.size() != var23.size()) {
               Log.debug(String.format("CB has different Item count in Contents %d<>%d.", var22.size(), var23.size()));
               return false;
            }

            ElementComparatorByAttribute var9 = new ElementComparatorByAttribute("key");
            var22.sort(var9);
            var23.sort(var9);

            for (int var10 = 0; var10 < var22.size(); var10++) {
               Element var11 = (Element)var22.get(var10);
               Element var12 = (Element)var23.get(var10);
               if (!itemIdentical(var11, var12)) {
                  return false;
               }

               ArrayList var13 = XMLUtil.getNestedElements(var11, "Item");
               ArrayList var14 = XMLUtil.getNestedElements(var12, "Item");
               if (var13.size() != var14.size()) {
                  Log.debug(
                     String.format("Item with key '%s' has different nested Item count %d<>%d.", XMLUtil.getAttr(var11, "key"), var13.size(), var14.size())
                  );
                  return false;
               }

               for (int var15 = 0; var15 < var13.size(); var15++) {
                  Element var16 = (Element)var13.get(var15);
                  Element var17 = (Element)var14.get(var15);
                  if (!itemIdentical(var16, var17)) {
                     return false;
                  }
               }
            }

            Log.debug("CB are itentical");
            return true;
         } else {
            Log.debug(String.format("CB has different attributes. Blocks:\n%s\n%s", XMLUtil.elementLineToString(var0), XMLUtil.elementLineToString(var1)));
            return false;
         }
      } catch (Exception var19) {
         Log.error("Error while comparing two CB..., Err. " + var19.getMessage(), var19);
         throw new Exception("Error while comparing two CB..., Err. " + var19.getMessage(), var19);
      }
   }

   private static boolean itemIdentical(Element var0, Element var1) throws Exception {
      if (XMLUtil.getAttr(var0, "key").equals(XMLUtil.getAttr(var1, "key"))
         && XMLUtil.getAttr(var0, "name", "NA").equals(XMLUtil.getAttr(var1, "name", "NA"))
         && XMLUtil.getAttr(var0, "returnType", "NA").equals(XMLUtil.getAttr(var1, "returnType", "NA"))
         && XMLUtil.getAttr(var0, "categoryType", "NA").equals(XMLUtil.getAttr(var1, "categoryType", "NA"))) {
         List var2 = var0.getChildren("Param");
         List var3 = var1.getChildren("Param");
         if (var2.size() != var3.size()) {
            Log.debug(
               String.format("String.format(Item with key '%s' has different Param count %d<>%d.", XMLUtil.getAttr(var0, "key"), var2.size(), var3.size())
            );
            return false;
         }

         for (int var4 = 0; var4 < var2.size(); var4++) {
            Element var5 = (Element)var2.get(var4);
            Element var6 = (Element)var3.get(var4);
            if (!XMLUtil.getAttr(var5, "key").equals(XMLUtil.getAttr(var6, "key"))
               || !XMLUtil.getAttr(var5, "name").equals(XMLUtil.getAttr(var6, "name"))
               || !XMLUtil.getAttr(var5, "type").equals(XMLUtil.getAttr(var6, "type"))
               || !XMLUtil.getAttr(var5, "controlType").equals(XMLUtil.getAttr(var6, "controlType"))
               || !XMLUtil.getAttr(var5, "defaultValue").equals(XMLUtil.getAttr(var6, "defaultValue"))
               || !XMLUtil.getAttr(var5, "minValue", "NA").equals(XMLUtil.getAttr(var6, "minValue", "NA"))
               || !XMLUtil.getAttr(var5, "maxValue", "NA").equals(XMLUtil.getAttr(var6, "maxValue", "NA"))
               || !XMLUtil.getAttr(var5, "step", "NA").equals(XMLUtil.getAttr(var6, "step", "NA"))
               || !XMLUtil.getAttr(var5, "builderStep", "NA").equals(XMLUtil.getAttr(var6, "builderStep", "NA"))
               || !var5.getText().equals(var6.getText())) {
               Log.debug(
                  String.format(
                     "Item/Param with key '%s/%s' has different attributes. Params:\n%s\n%s",
                     XMLUtil.getAttr(var0, "key"),
                     XMLUtil.getAttr(var5, "key"),
                     XMLUtil.elementLineToString(var5),
                     XMLUtil.elementLineToString(var6)
                  )
               );
               return false;
            }
         }

         return true;
      } else {
         Log.debug(
            String.format(
               "Item with key '%s' has different attributes. Items:\n%s\n%s",
               XMLUtil.getAttr(var0, "key"),
               XMLUtil.elementLineToString(var0),
               XMLUtil.elementLineToString(var1)
            )
         );
         return false;
      }
   }
}
