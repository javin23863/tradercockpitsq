package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlocksConfig implements Serializable {
   public static final Logger Log = LoggerFactory.getLogger("BlocksConfig");
   private static BlocksConfig config = null;
   private static Element elConfig = null;
   private HashMap<String, Element> parameterSets = new HashMap<>();
   private HashMap<String, BlockDefinition> blocks = new HashMap<>();
   private HashMap<String, BlockDefinition> formulas = new HashMap<>();

   public static synchronized BlocksConfig getConfig() throws JDOMException, IOException, Exception {
      if (config == null) {
         getConfigElement();
         config = new BlocksConfig(elConfig);
      }

      return config;
   }

   public static synchronized Element getConfigElement() throws Exception {
      if (elConfig == null) {
         elConfig = loadXmlConfig();
      }

      return elConfig;
   }

   private static Element loadXmlConfig() throws Exception {
      File var0 = new File(SQPaths.wizardConfigPath);
      String var1 = SQUtils.fileToString(var0);
      return XMLUtil.stringToXmlElement(var1);
   }

   private BlocksConfig(Element var1) throws Exception {
      this.init();
   }

   private void init() throws Exception {
      this.removeHelpDisplayAttributes(elConfig);
      this.parseParameterSets(elConfig);
      this.parseBlocks(elConfig);
      this.parseFormulas(elConfig);
   }

   private void removeHelpDisplayAttributes(Element var1) {
      List var2 = var1.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         var4.removeAttribute("help");
         this.removeHelpDisplayAttributes(var4);
      }
   }

   private void parseParameterSets(Element var1) {
      for (Element var3 : var1.getChild("ParameterSets").getChildren()) {
         this.parameterSets.put(var3.getAttributeValue("name"), var3);
      }
   }

   private void parseBlocks(Element var1) throws Exception {
      for (Element var3 : var1.getChild("Blocks").getChildren()) {
         for (Element var5 : var3.getChildren()) {
            for (Element var7 : var5.getChildren()) {
               String var8 = var7.getAttributeValue("key");
               String var9 = var7.getAttributeValue("categoryType");
               if (var9 == null || !var9.equals("randomBlock")) {
                  String var11 = var7.getAttributeValue("ParameterSet");
                  Element var10;
                  if (var11 != null) {
                     var10 = this.getParameterSet(var11);
                  } else {
                     var10 = var7;
                  }

                  this.blocks.put(var8, new BlockDefinition(var7, var10, var3.getName()));
               }
            }
         }
      }
   }

   private Element getParameterSet(String var1) throws Exception {
      if (var1.contains(var1)) {
         return this.parameterSets.get(var1);
      } else {
         throw new Exception(L.t("Parameter set '%s' doesn't exist!", new Object[]{var1}));
      }
   }

   public BlockDefinition getBlock(String var1) {
      return var1.startsWith("CBlock_") ? CustomBlocks.getBlock(var1) : this.blocks.get(var1);
   }

   public static ArrayList<RandomPlaceholder> getPlaceholders(String var0) throws Exception {
      ArrayList var1 = new ArrayList();
      Element var2 = XMLUtil.fileToXmlElement(new File(var0));
      recursiveGetPlaceholders(var1, var2);
      return var1;
   }

   public static void recursiveGetPlaceholders(ArrayList<RandomPlaceholder> var0, Element var1) throws Exception {
      Log.debug("Checking tag :" + var1.getName());
      ArrayList var2 = XMLUtil.getNestedElements(var1, "Item");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("generate");
         if (var5 != null) {
            String var6 = var4.getAttributeValue("superType");
            if (var6 == null) {
               throw new Exception(
                  L.t("Item '%s' has randomTemplate=true, but it is missing attribute superType!", new Object[]{var4.getAttributeValue("key")})
               );
            }

            Log.debug("FOUND PLACEHOLDER " + var6);
            switch (var6) {
               case "condition":
                  var0.add(new RandomPlaceholder(getIdentification(var4), 3, 3, var5));
                  break;
               case "comparison":
                  var0.add(new RandomPlaceholder(getIdentification(var4), 2, 3, var5));
                  break;
               case "property":
                  var0.add(new RandomPlaceholder(getIdentification(var4), 1, 3, var5));
                  break;
               case "value":
                  var0.add(new RandomPlaceholder(getIdentification(var4), 4, 6, var5));
                  break;
               case "action":
                  var0.add(new RandomPlaceholder(getIdentification(var4), 5, 4, var5));
            }
         }
      }
   }

   private static String getIdentification(Element var0) throws Exception {
      Element var1 = var0.getChild("Param");
      if (var1 == null) {
         throw new Exception(L.t("Random placeholder doesn't contain identification parameter!", new Object[0]));
      } else if (var1.getAttributeValue("controlType") != null && var1.getAttributeValue("controlType").startsWith("randomId")) {
         return var1.getValue();
      } else {
         throw new Exception(L.t("Random placeholder doesn't contain parameter randomId!", new Object[0]));
      }
   }

   public BlockDefinition getFormula(String var1) {
      return this.formulas.get(var1);
   }

   private void parseFormulas(Element var1) throws Exception {
      for (Element var3 : var1.getChild("Global").getChild("Formulas").getChildren()) {
         for (Element var5 : var3.getChildren()) {
            String var6 = var5.getAttributeValue("key");
            String var8 = var5.getAttributeValue("ParameterSet");
            Element var7;
            if (var8 != null) {
               var7 = this.getParameterSet(var8);
            } else {
               var7 = var5;
            }

            this.formulas.put(var6, new BlockDefinition(var5, var7, "formula"));
         }
      }
   }

   public void reload() {
      try {
         BlocksConfig var1 = getConfig();
         var1.parameterSets = new HashMap<>();
         var1.blocks = new HashMap<>();
         var1.formulas = new HashMap<>();
         elConfig = loadXmlConfig();
         this.init();
      } catch (Exception var2) {
         Log.error("Errow while reloading BlocksConfig." + var2.getMessage(), var2);
      }
   }
}
