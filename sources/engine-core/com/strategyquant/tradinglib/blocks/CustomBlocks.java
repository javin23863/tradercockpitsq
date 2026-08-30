package com.strategyquant.tradinglib.blocks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.blocks.random.BlockDefinition;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomBlocks {
   private static final Logger Log = LoggerFactory.getLogger(CustomBlocks.class);
   private static Element config;
   private static final DataToSend refreshMessage = new DataToSend("customBlocksChanged", new JSONObject().put("customBlocksChanged", true));
   private static HashMap<String, BlockDefinition> cbDefinitionsMap = new HashMap<>();
   private static HashMap<String, Element> cbElementsMap = new HashMap<>();

   public static void load() {
      try {
         File var0 = new File(SQPaths.algoWizardCustomBlocksPath);
         if (var0.exists()) {
            try {
               Element var1 = XMLUtil.fileToXmlElement(var0);
               if (setConfig(var1)) {
                  XMLUtil.xmlToFile(var1, var0);
               }
            } catch (Exception var2) {
               Log.error("Loading custom blocks failed", var2);
               setConfig(new Element("CustomBlocks"));
            }
         } else {
            setConfig(new Element("CustomBlocks"));
         }
      } catch (Exception var3) {
         Log.error("Error while loading CustomBlocks - " + var3.getMessage(), var3);
      }
   }

   public static void save(Element var0) throws Exception {
      setConfig(var0);
      String var1 = XMLUtil.elementToString(var0);
      SQUtils.createAndHandleBackupFiles(SQPaths.algoWizardCustomBlocksPath, var1, 50);
      SQUtils.stringToFile(SQPaths.algoWizardCustomBlocksPath, var1);
      SQWebSocketManager.addToDataQueue(refreshMessage, "BUILDER", "TASKMANAGER");
   }

   public static Element get() {
      return config;
   }

   public static JSONArray getAsBuildingBlocks() {
      JSONArray var0 = new JSONArray();
      if (config != null) {
         List var1 = config.getChildren("Item");

         for (int var2 = 0; var2 < var1.size(); var2++) {
            try {
               Element var3 = (Element)var1.get(var2);
               Element var4 = var3.getChild("Contents");
               JSONObject var5 = new JSONObject();
               var5.put("name", var3.getAttributeValue("name"));
               var5.put("key", var3.getAttributeValue("key"));
               var5.put("weight", 1);
               var5.put("originalWeight", 1);
               var5.put("empty", var4 == null || var4.getChildren().size() == 0);
               String var6 = var3.getAttributeValue("type");
               String var7 = var3.getAttributeValue("category");
               var5.put("returnType", getReturnType(var6));
               var5.put("strategyType", var3.getAttributeValue("strategyType", "Standard"));
               var5.put("groupKey", getGroupName(var6, null));
               var5.put("group", getGroupName(var6, var7));
               JSONArray var8 = new JSONArray();
               List var9 = var3.getChildren("Param");

               for (int var10 = 0; var10 < var9.size(); var10++) {
                  Element var11 = (Element)var9.get(var10);
                  String var12 = var11.getAttributeValue("key");
                  String var13 = var11.getAttributeValue("name");
                  String var14 = var11.getAttributeValue("type");
                  String var15 = var11.getAttributeValue("paramType");
                  String var16 = var11.getAttributeValue("defaultValue");
                  String var17 = var11.getAttributeValue("minValue");
                  String var18 = var11.getAttributeValue("maxValue");
                  String var19 = var11.getAttributeValue("genMinValue");
                  String var20 = var11.getAttributeValue("genMaxValue");
                  String var21 = var11.getAttributeValue("step");
                  if (var12.equals("#Chart#")) {
                     var8.put(
                        new JSONObject()
                           .put("generation", "random")
                           .put("defaultValue", "0")
                           .put("name", var13)
                           .put("weight", 1)
                           .put("originalWeight", 1)
                           .put("type", "data")
                           .put("paramType", var15)
                           .put("allCharts", true)
                           .put("key", "#Chart#")
                     );
                  } else if (var12.equals("#ComputedFrom#")) {
                     var8.put(
                        new JSONObject()
                           .put("generation", "random")
                           .put("defaultValue", getComputedFromDefaultValue(var16))
                           .put("values", "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6,Volume=7")
                           .put("name", var13)
                           .put("weight", 1)
                           .put("originalWeight", 1)
                           .put("type", "int")
                           .put("paramType", var15)
                           .put("key", "#ComputedFrom#")
                     );
                  } else {
                     var14 = var14.equals("bool") ? "boolean" : var14;
                     JSONObject var22 = new JSONObject();
                     var22.put("generation", "random");
                     var22.put("key", var12);
                     var22.put("name", var13);
                     var22.put("type", var14);
                     var22.put("paramType", var15);
                     if (var14.equals("int") || var14.equals("double")) {
                        var22.put("originalMaxValue", var18);
                        var22.put("genMaxValue", var20);
                        var22.put("maxValue", var18);
                        var22.put("originalMinValue", var17);
                        var22.put("genMinValue", var19);
                        var22.put("minValue", var17);
                        var22.put("step", var21);
                     } else if (var14.equals("data")) {
                        var22.put("paramType", "data");
                        var22.put("allCharts", true);
                     }

                     if (var15 != null && "0".equals(var21) && "0".equals(var17) && "0".equals(var18)) {
                        if (var15.equals("period")) {
                           var22.put("minValue", -1000003);
                           var22.put("maxValue", -1000004);
                           var22.put("step", 1);
                        } else if (var15.equals("shift")) {
                           var22.put("minValue", -1000001);
                           var22.put("maxValue", -1000002);
                           var22.put("step", 1);
                        }
                     }

                     var22.put("defaultValue", var16);
                     var22.put("weight", 1);
                     var22.put("originalWeight", 1);
                     var8.put(var22);
                  }
               }

               var5.put("params", var8);
               var0.put(var5);
            } catch (Exception var23) {
               Log.error("Converting custom blocks to building blocks failed", var23);
            }
         }
      }

      return var0;
   }

   private static String getReturnType(String var0) {
      if (var0 == null) {
         return "number";
      }

      switch (var0) {
         case "Condition":
            return "boolean";
         case "Price level":
            return "price";
         default:
            return "number";
      }
   }

   private static String getComputedFromDefaultValue(String var0) {
      if (var0 == null) {
         return "0";
      }

      switch (var0) {
         case "Open":
            return "1";
         case "High":
            return "2";
         case "Low":
            return "3";
         case "Median":
            return "4";
         case "Typical":
            return "5";
         case "Weighted":
            return "6";
         default:
            return "0";
      }
   }

   private static String getGroupName(String var0, String var1) {
      if (var0 == null) {
         return "Custom values";
      }

      switch (var0) {
         case "Condition":
            if (var1 != null && !var1.equals("No category")) {
               return var1;
            }

            return "Custom conditions";
         case "Price level":
            return "Custom price levels";
         default:
            return "Custom values";
      }
   }

   public static BlockDefinition getBlock(String var0) {
      synchronized (cbDefinitionsMap) {
         return cbDefinitionsMap.get(var0);
      }
   }

   private static boolean setConfig(Element var0) {
      boolean var1 = fixConfig(var0);
      config = var0;
      synchronized (cbDefinitionsMap) {
         cbDefinitionsMap.clear();
         cbElementsMap.clear();
         if (config != null) {
            List var3 = config.getChildren("Item");

            for (int var4 = 0; var4 < var3.size(); var4++) {
               try {
                  Element var5 = (Element)var3.get(var4);
                  Element var6 = new Element("TempCategory");
                  var6.addContent(var5.clone());
                  String var7 = var5.getAttributeValue("type");
                  fixReturnType(var5, var7);
                  BlockDefinition var8 = new BlockDefinition(var5, var6, var7);
                  cbDefinitionsMap.put(var8.key, var8);
                  cbElementsMap.put(var8.key, var5.clone());
               } catch (Exception var10) {
                  Log.error("Converting custom blocks to building blocks failed", var10);
               }
            }
         }

         return var1;
      }
   }

   private static boolean fixConfig(Element var0) {
      if (var0 == null) {
         return false;
      }

      boolean var1 = false;

      try {
         ArrayList var2 = new ArrayList();
         List var3 = var0.getChildren("Item");

         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = (Element)var3.get(var4);
            String var6 = XMLUtil.getAttr(var5, "key");
            if (!var2.contains(var6)) {
               var2.add(var6);
            }
         }

         for (int var10 = 0; var10 < var3.size(); var10++) {
            Element var11 = (Element)var3.get(var10);
            String var12 = XMLUtil.getAttr(var11, "key");
            String var7 = XMLUtil.getAttr(var11, "name");
            String var8 = XMLUtil.getAttr(var11, "oppositeBlockKey", null);
            if (var8 != null && !var8.equals("") && !var8.equals("CBlock_null") && !var8.equals("null") && !var8.equals("CBlock_") && !var2.contains(var8)) {
               var11.setAttribute("oppositeBlockKey", "CBlock_null");
               var1 = true;
               Log.info(
                  String.format("Fixed CustomBlock with key/name %s/%s - Opposite block with key %s doesn't exist, changed to 'CBlock_null'", var12, var7, var8)
               );
            }
         }
      } catch (Exception var9) {
         Log.error("Error while fixing CustomBlocks xml - " + var9.getMessage(), var9);
      }

      return var1;
   }

   private static void fixReturnType(Element var0, String var1) throws Exception {
      switch (var1) {
         case "Condition":
            var0.setAttribute("returnType", "boolean");
            return;
         case "Price level":
            var0.setAttribute("returnType", "price");
            return;
         case "Action":
            var0.setAttribute("returnType", "order");
            return;
         case "Value":
            var0.setAttribute("returnType", "number");
            return;
         default:
            throw new Exception(L.t("Custom block has unknown block type: %s", new Object[]{var1}));
      }
   }

   public static Element getElement(String var0) {
      synchronized (cbDefinitionsMap) {
         return cbElementsMap.get(var0);
      }
   }

   public static HashMap<String, Element> getAllElements() {
      synchronized (cbDefinitionsMap) {
         return cbElementsMap;
      }
   }

   public static void addCBlockSourceCodes(Element var0) {
      Element var1 = var0.getChild("Strategy").getChild("CustomBlocks");
      if (var1 != null) {
         try {
            _addCBlockSourceCodesToItems(var0, var1);
         } catch (Exception var3) {
            var3.printStackTrace();
         }
      }
   }

   private static void _addCBlockSourceCodesToItems(Element var0, Element var1) {
      if (!var0.getName().equals("RandomGroups")) {
         if (var0.getName().equals("Item")) {
            String var2 = var0.getAttributeValue("key");
            if (var2 != null && var2.startsWith("CBlock")) {
               String var3 = getCBlockId(var2);
               if (var3 != null) {
                  _addCBlockSourceCodesToItem(var0, var2, var1);
               }
            }
         }

         List var5 = var0.getChildren();
         if (var5 != null && var5.size() > 0) {
            for (int var6 = 0; var6 < var5.size(); var6++) {
               Element var4 = (Element)var5.get(var6);
               _addCBlockSourceCodesToItems(var4, var1);
            }
         }
      }
   }

   private static void _addCBlockSourceCodesToItem(Element var0, String var1, Element var2) {
      Element var3 = null;
      List var4 = var2.getChildren();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("key");
         if (var7 != null && var7.equals(var1)) {
            var3 = var6;
            break;
         }
      }

      if (var3 == null) {
         Log.error("No Custom Blocks definition found for block '" + var1 + "'");
      }

      Element var9 = var3.getChild("Contents").clone();
      if (!hasContentsElement(var0)) {
         var0.addContent(var9);
      }

      var4 = var0.getChildren("Param");

      for (int var10 = 0; var10 < var4.size(); var10++) {
         Element var11 = (Element)var4.get(var10);
         _fixCBlockParametersForSourceCodes(var11, var9);
      }
   }

   private static boolean hasContentsElement(Element var0) {
      Element var1 = var0.getChild("Contents");
      return var1 != null && var1.getChildren().size() != 0;
   }

   private static void _fixCBlockParametersForSourceCodes(Element var0, Element var1) {
      String var2 = var0.getAttributeValue("key");
      if (var1.getName().equals("Param")) {
         String var3 = var1.getText();
         if (var3 != null && var3.equals(var2)) {
            var1.setText(var0.getText());
            String var4 = var0.getAttributeValue("variable");
            if (var4 != null) {
               var1.setAttribute("variable", var4);
            }
         }
      }

      List var6 = var1.getChildren();
      if (var6 != null && var6.size() > 0) {
         for (int var7 = 0; var7 < var6.size(); var7++) {
            Element var5 = (Element)var6.get(var7);
            _fixCBlockParametersForSourceCodes(var0, var5);
         }
      }
   }

   public static String getCBlockId(String var0) {
      return var0.length() <= 7 ? null : var0.substring(7);
   }

   public static Element findByKey(String var0) {
      HashMap var1 = getAllElements();

      for (String var3 : var1.keySet()) {
         Element var4 = (Element)var1.get(var3);
         if (var4.getAttributeValue("key").equals(var0)) {
            return var4;
         }
      }

      return null;
   }

   public static Element findByName(String var0, Element var1) {
      if (var1 == null) {
         return null;
      }

      for (Element var4 : var1.getChildren("Item")) {
         if (var4.getAttributeValue("name").equals(var0)) {
            return var4;
         }
      }

      return null;
   }

   public static JSONArray listBackupFiles() {
      return SQUtils.listBackupFiles(SQPaths.algoWizardCustomBlocksPath);
   }
}
