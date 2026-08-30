package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.ElementComparatorByAttribute;
import com.strategyquant.lib.XMLUtil;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourcesRandomGroups {
   private static final Logger Log = LoggerFactory.getLogger(ResourcesRandomGroups.class);

   public static boolean areIdentical(Element var0, Element var1) throws Exception {
      try {
         Log.debug(
            String.format(
               "Comparing two RG with ids '%s'/'%s', names '%s'/'%s'...",
               XMLUtil.getAttr(var0, "id"),
               XMLUtil.getAttr(var1, "id"),
               XMLUtil.getAttr(var0, "name", "NA"),
               XMLUtil.getAttr(var1, "name", "NA")
            )
         );
         if (XMLUtil.getAttr(var0, "id").equals(XMLUtil.getAttr(var1, "id")) && XMLUtil.getAttr(var0, "type").equals(XMLUtil.getAttr(var1, "type"))) {
            Log.debug("RG attributes match, checking details ...");
            List var2 = var0.getChildren("Item");
            List var3 = var1.getChildren("Item");
            if (var2.size() != var3.size()) {
               Log.debug("RG has different Item count.");
               return false;
            }

            ElementComparatorByAttribute var4 = new ElementComparatorByAttribute("key");
            var2.sort(var4);
            var3.sort(var4);

            for (int var5 = 0; var5 < var2.size(); var5++) {
               Element var6 = (Element)var2.get(var5);
               Element var7 = (Element)var3.get(var5);
               if (!itemIdentical(var6, var7)) {
                  return false;
               }

               ArrayList var8 = XMLUtil.getNestedElements(var6, "Item");
               ArrayList var9 = XMLUtil.getNestedElements(var7, "Item");
               if (var8.size() != var9.size()) {
                  Log.debug(String.format("Item with key '%s' has different nested Item count %d<>%d.", XMLUtil.getAttr(var6, "key"), var8.size(), var9.size()));
                  return false;
               }

               for (int var10 = 0; var10 < var8.size(); var10++) {
                  Element var11 = (Element)var8.get(var10);
                  Element var12 = (Element)var9.get(var10);
                  if (!itemIdentical(var11, var12)) {
                     return false;
                  }
               }
            }

            Log.debug("RG are itentical");
            return true;
         } else {
            Log.debug(String.format("RG has different attributes. Groups:\n%s\n%s", XMLUtil.elementLineToString(var0), XMLUtil.elementLineToString(var1)));
            return false;
         }
      } catch (Exception var13) {
         Log.error("Error while comparing two RG..., Err. " + var13.getMessage(), var13);
         throw new Exception("Error while comparing two RG..., Err. " + var13.getMessage(), var13);
      }
   }

   private static boolean itemIdentical(Element var0, Element var1) throws Exception {
      if (XMLUtil.getAttr(var0, "key").equals(XMLUtil.getAttr(var1, "key"))
         && XMLUtil.getAttr(var0, "name", "NA").equals(XMLUtil.getAttr(var1, "name", "NA"))
         && XMLUtil.getAttr(var0, "returnType", "NA").equals(XMLUtil.getAttr(var1, "returnType", "NA"))
         && XMLUtil.getAttr(var0, "categoryType", "NA").equals(XMLUtil.getAttr(var1, "categoryType", "NA"))) {
         String var2 = XMLUtil.getAttr(var0, "key");
         List var3 = var0.getChildren("Param");
         List var4 = var1.getChildren("Param");
         if (var3.size() != var4.size()) {
            Log.debug(String.format("RG Item with key '%s' has different Param count %d<>%d.", var2, var3.size(), var4.size()));
            return false;
         }

         for (int var5 = 0; var5 < var3.size(); var5++) {
            Element var6 = (Element)var3.get(var5);
            Element var7 = (Element)var4.get(var5);
            if (!XMLUtil.getAttr(var6, "key").equals(XMLUtil.getAttr(var7, "key"))) {
               Log.debug(String.format("RG %s Param key mismatch '%s'<> '%s'.", var2, XMLUtil.getAttr(var6, "key"), XMLUtil.getAttr(var7, "key")));
               return false;
            }

            String var8 = XMLUtil.getAttr(var6, "key");

            try {
               if (!XMLUtil.getAttr(var6, "name").equals(XMLUtil.getAttr(var7, "name"))
                  || !XMLUtil.getAttr(var6, "type").equals(XMLUtil.getAttr(var7, "type"))
                  || !XMLUtil.getAttr(var6, "controlType").equals(XMLUtil.getAttr(var7, "controlType"))
                  || !XMLUtil.getAttr(var6, "defaultValue").equals(XMLUtil.getAttr(var7, "defaultValue"))
                  || !XMLUtil.getAttr(var6, "minValue", "NA").equals(XMLUtil.getAttr(var7, "minValue", "NA"))
                  || !XMLUtil.getAttr(var6, "maxValue", "NA").equals(XMLUtil.getAttr(var7, "maxValue", "NA"))
                  || !XMLUtil.getAttr(var6, "step", "NA").equals(XMLUtil.getAttr(var7, "step", "NA"))
                  || !XMLUtil.getAttr(var6, "generate", "NA").equals(XMLUtil.getAttr(var7, "generate", "NA"))
                  || !XMLUtil.getAttr(var6, "randomValue", "NA").equals(XMLUtil.getAttr(var7, "randomValue", "NA"))
                  || !XMLUtil.getAttr(var6, "builderStep", "NA").equals(XMLUtil.getAttr(var7, "builderStep", "NA"))
                  || !var6.getText().equals(var7.getText())) {
                  Log.debug(
                     String.format(
                        "RG Item/Param with key '%s/%s' has different attributes. Params:\n%s\n%s",
                        var2,
                        XMLUtil.getAttr(var6, "key"),
                        XMLUtil.elementLineToString(var6),
                        XMLUtil.elementLineToString(var7)
                     )
                  );
                  return false;
               }
            } catch (Exception var10) {
               Log.error(String.format("RG Error comparing Param with key '%s/%s'..., Err: %s", var2, var8, var10.getMessage()), var10);
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
