package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.blocks.random.ReplacementsConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneratorBase {
   public static final Logger Log = LoggerFactory.getLogger("GeneratorBase");
   protected Element elTemplate;
   protected IRandomGenerator rng;
   protected ReplacementsConfig replacements;
   protected HashMap<String, Element> generatedElements;

   protected void addTemplateVariablesToBuildSettings(SettingsMap var1) {
      for (Element var3 : this.elTemplate.getChild("Strategy").getChild("Variables").getChildren()) {
         var1.set(var3.getChildText("name"), var3.getChildText("id"));
      }
   }

   protected void getIdentifiers(Element var1, ArrayList<String> var2, HashMap<String, Element> var3, boolean var4) throws GenerateException {
      Object var5 = null;
      Object var6 = null;
      String var7 = null;
      List var8 = var1.getChildren();

      for (int var9 = 0; var9 < var8.size(); var9++) {
         Element var10 = (Element)var8.get(var9);
         if (var10.getName().equals("Item") || var10.getName().equals("Param")) {
            String var11 = var10.getAttributeValue("generate");
            if (var11 != null) {
               if (var4 && var11.equals("random")) {
                  var2.add(var10.getName().equals("Item") ? findIdentifierInParams(var10, true) : findIdentifierInParams(var1, true));
               } else if (!var4 && (var11.equals("same") || var11.equals("opposite"))) {
                  if (var10.getName().equals("Item")) {
                     var2.add(findIdentifierInParams(var10, true));
                  } else {
                     var5 = var10.getAttributeValue("identification");
                     if (var5 == null) {
                        var5 = var10.getAttributeValue("identificator");
                     }

                     var6 = var1.getAttributeValue("key");
                     if (var6 == null) {
                        var6 = "Unknown";
                     }

                     var7 = var10.getAttributeValue("key");
                     if (var7 == null) {
                        var7 = "Unknown";
                     }

                     if (var5 == null) {
                        throw new GenerateException(String.format("Bad configuration - Identification not found in item '%s->%s'", var6, var7));
                     }

                     if (!var3.containsKey(var5)) {
                        throw new GenerateException(
                           String.format("Bad configuration - No random block found for identifier '%s' referenced by '%s->%s'", var5, var6, var7)
                        );
                     }

                     Element var12 = (Element)var3.get(var5);
                     Element var13 = this.getRandomBlockParamElement(var12, var7);
                     if (var13 == null) {
                        throw new GenerateException(
                           String.format(
                              "Bad configuration - No parameter with key '%s' found in random block with identifier '%s' referenced by '%s->%s'",
                              var7,
                              var5,
                              var6,
                              var7
                           )
                        );
                     }

                     var2.add(var5);
                  }
               }
            }
         }

         List var17 = var10.getChildren();
         if (var17 != null) {
            this.getIdentifiers(var10, var2, var3, var4);
         }
      }
   }

   public static void checkUniqueRandomIdentifiers(Element var0, HashMap<String, Element> var1) throws GenerateException {
      String var2 = var0.getName();
      if (!var2.equals("RandomGroups")) {
         List var3 = var0.getChildren();

         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            List var6 = var5.getChildren();
            if (var5.getName().equals("Item") || var5.getName().equals("Param")) {
               String var7 = var5.getAttributeValue("generate");
               if (var7 != null && var7.equals("random")) {
                  Element var8 = var5.getName().equals("Item") ? var5 : var0;
                  String var9 = findIdentifierInParams(var8, true);
                  if (!var1.containsKey(var9)) {
                     var8.setAttribute("isParameter", var5.getName().equals("Item") ? "false" : "true");
                     var1.put(var9, var8);
                  } else if (var1.get(var9) != var8) {
                     throw new GenerateException(String.format("Bad configuration - Identification '%s' used in more than one random blocks!", var9));
                  }
               }
            }

            var6 = var5.getChildren();
            if (var6 != null) {
               checkUniqueRandomIdentifiers(var5, var1);
            }
         }
      }
   }

   protected static String findIdentifierInParams(Element var0, boolean var1) throws GenerateException {
      if (!var0.getName().equals("Item")) {
         throw new GenerateException(String.format("Bad configuration - element is not Item, it is '%s'", var0.getName()));
      }

      List var2 = var0.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("key");
         if (var5 != null && var5.equals("#Identification#")) {
            return var4.getValue();
         }
      }

      String var6 = var0.getAttributeValue("key");
      if (var6 == null) {
         var6 = "Unknown";
      }

      if (var1) {
         throw new GenerateException(String.format("Bad configuration - Identification not found in item '%s'", var6));
      } else {
         return null;
      }
   }

   protected void findAndOrBlocks(Element var1, RandomAndOrList var2) throws GenerateException {
      List var3 = var1.getChildren();

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         List var6 = var5.getChildren();
         if (var5.getName().equals("Item")) {
            String var7 = var5.getAttributeValue("key");
            if (var7.equals("RND")) {
               RandomAndOr var8 = new RandomAndOr(var5);
               var2.add(var8);
            } else if (var7.startsWith("Random")) {
               String var10 = this.getRandomBlockId(var5);
               var2.setRAO(1, var10);
            } else if (var7.startsWith("Same") || var7.startsWith("Negate")) {
               String var11 = this.getRandomBlockId(var5);
               var2.setRAO(2, var11);
            }
         }

         var6 = var5.getChildren();
         if (var6 != null) {
            this.findAndOrBlocks(var5, var2);
         }
      }
   }

   protected String getRandomBlockId(Element var1) throws GenerateException {
      List var2 = var1.getChildren("Param");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         if (var4.getAttributeValue("key").equals("#Identification#")) {
            return var4.getValue();
         }
      }

      throw new GenerateException("No parameter found for Random block!");
   }

   protected String getRandomBlockParamValue(Element var1, String var2) {
      List var3 = var1.getChildren("Param");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (var5.getAttributeValue("key").equals(var2)) {
            return var5.getValue();
         }
      }

      return null;
   }

   protected Element getRandomBlockParamElement(Element var1, String var2) {
      List var3 = var1.getChildren("Param");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         if (var5.getAttributeValue("key").equals(var2)) {
            return var5;
         }
      }

      return null;
   }
}
