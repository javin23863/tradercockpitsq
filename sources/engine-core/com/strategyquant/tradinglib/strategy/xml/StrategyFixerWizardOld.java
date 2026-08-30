package com.strategyquant.tradinglib.strategy.xml;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.BlockDefinitionException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyFixerWizardOld {
   public static final Logger Log = LoggerFactory.getLogger("StrategyFixerWizardOld");
   public static final String currentVersion = "3.9.131";
   private static HashMap<String, String> blockCategoryTypes;
   private static Map<String, List<Attribute>> itemAttributes;

   public static String fixStrategy(String var0) throws Exception {
      return fixStrategy(var0, SQPaths.wizardConfigPath, false);
   }

   public static String fixStrategy(String var0, String var1, boolean var2) throws Exception {
      Element var3 = XMLUtil.stringToElement(var0);
      if (!var2 && checkVersion(var3)) {
         return var0;
      }

      loadCategoryTypesAndAttributes(var1);
      var0 = SQUtils.removeUTF8BOM(var0);
      var0 = SQUtils.removeXmlns(var0);
      var0 = fixCategoryTypes(var3);
      var0 = fixMissingAttributes(var3);
      var0 = fixMoneyManagement(var3);
      var0 = SQUtils.fixRenamedActionParams(var0);
      var0 = SQUtils.fixHeikenAshi(var0);
      return var0.replaceAll("help=\".*?\"", "");
   }

   public static String fixMissingAttributes(Element var0) throws Exception {
      ArrayList var1 = XMLUtil.getNestedElements(var0, "Item");

      for (int var2 = 0; var2 < var1.size(); var2++) {
         Element var3 = (Element)var1.get(var2);
         String var4 = var3.getAttributeValue("key");
         if (var4 != null) {
            List var5 = itemAttributes.get(var4);
            if (var5 != null) {
               addMissingAttributes(var3, var5);
            }
         }
      }

      return XMLUtil.xmlToString(var0);
   }

   private static void addMissingAttributes(Element var0, List<Attribute> var1) {
      List var2 = var0.getAttributes();
      Set var3 = var2.stream().map(Attribute::getName).collect(Collectors.toSet());
      Map var4 = var1.stream().collect(Collectors.toMap(Attribute::getName, Function.identity()));

      for (String var6 : var4.keySet()) {
         if (!var3.contains(var6)) {
            var0.getAttributes().add(new Attribute(var6, ((Attribute)var4.get(var6)).getValue()));
         }
      }
   }

   public static boolean checkVersion(Element var0) {
      try {
         String var1 = var0.getAttributeValue("Version");
         String[] var2 = var1.split("\\.");
         String[] var3 = "3.9.131".split("\\.");

         for (int var4 = 0; var4 < var3.length; var4++) {
            if (var4 < var2.length && Integer.parseInt(var2[var4]) < Integer.parseInt(var3[var4])) {
               return false;
            }
         }

         return var2.length == var3.length;
      } catch (Exception var5) {
         return false;
      }
   }

   public static String fixMoneyManagement(Element var0) throws JDOMException, IOException, BlockDefinitionException {
      ArrayList var1 = XMLUtil.getNestedElements(var0, "MoneyManagement");

      for (int var2 = 0; var2 < var1.size(); var2++) {
         Element var3 = (Element)var1.get(var2);
         if (var3.getChild("Method") == null) {
            Element var4 = new Element("Method");
            var4.setAttribute("type", var3.getAttributeValue("type"));
            var4.setAttribute("use", "true");
            Element var5 = new Element("Params");
            Element var6 = var3.getChild("params");
            String var7 = "Param";
            if (var6 == null) {
               var6 = var3.getChild("Parameters");
               var7 = "Parameter";
            }

            if (var6 != null) {
               List var8 = var6.getChildren(var7);

               for (int var9 = 0; var9 < var8.size(); var9++) {
                  Element var10 = (Element)var8.get(var9);
                  var5.addContent(var10.clone().setName("Param"));
               }

               var3.removeContent(var6);
            }

            var4.addContent(var5);
            var3.addContent(var4);
         }
      }

      return XMLUtil.xmlToString(var0);
   }

   private static void fixBlockAttributes(Element var0) throws BlockDefinitionException {
      List var1 = var0.getChildren();
      if (var1 != null) {
         for (int var2 = 0; var2 < var1.size(); var2++) {
            Element var3 = (Element)var1.get(var2);
            if (var3.getName().equals("Item")) {
               String var4 = var3.getAttributeValue("key");
               if (var4 != null) {
                  String var5 = blockCategoryTypes.get(var4);
                  if (var5 != null) {
                     var3.setAttribute("categoryType", var5);
                  }
               }
            }

            if (var3.getChildren() != null) {
               fixBlockAttributes(var3);
            }
         }
      }
   }

   public static String fixCategoryTypes(Element var0) throws JDOMException, IOException, BlockDefinitionException {
      fixBlockAttributes(var0);
      var0.setAttribute("Version", "3.9.131");
      return XMLUtil.xmlToString(var0);
   }

   private static void loadCategoryTypesAndAttributes(String var0) throws Exception {
      if (blockCategoryTypes == null) {
         blockCategoryTypes = new HashMap<>();
         itemAttributes = new HashMap<>();
         File var1 = new File(var0);
         if (!var1.exists()) {
            throw new Exception("Wizard config file not found.");
         }

         Element var2 = XMLUtil.fileToXmlElement(var1);
         ArrayList var3 = XMLUtil.getNestedElements(var2, "Item");

         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            String var6 = var5.getAttributeValue("key");
            String var7 = var5.getAttributeValue("categoryType");
            if (var6 != null && var7 != null) {
               blockCategoryTypes.put(var6, var7);
            }

            itemAttributes.put(var6, var5.getAttributes());
         }
      }
   }
}
