package com.strategyquant.tradinglib.project;

import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectCustomResources {
   private static final Logger Log = LoggerFactory.getLogger(ProjectCustomResources.class);
   private static final String ResourcesElementName = "Resources";
   private static final String CustomSnippetsElementName = "CustomSnippets";
   private static final String CustomIndicatorsElementName = "CustomIndicators";
   private static final String CustomBlocksElementName = "CustomBlocks";
   private static final String RandomGroupsElementName = "RandomGroups";

   public static void addProjectResources(String var0, Element var1) throws Exception {
      try {
         if (var0 == null) {
            var0 = XMLUtil.elementToString(var1);
         }

         Element var2 = XMLUtil.getOrCreateChild(var1, "Resources");
         ArrayList var3 = new ArrayList();

         try {
            ArrayList var4 = XMLUtil.getNestedElements(var1, "CustomData");

            for (int var5 = 0; var5 < var4.size(); var5++) {
               Element var6 = (Element)var4.get(var5);
               List var7 = var6.getChildren("Data");

               for (int var8 = 0; var8 < var7.size(); var8++) {
                  Element var9 = (Element)var7.get(var8);
                  if (XMLUtil.getBooleanAttr(var9, "use", false)) {
                     String var10 = var9.getAttributeValue("name");
                     if (!var3.contains(var10)) {
                        var3.add(var10);
                     }
                  }
               }
            }
         } catch (Exception var11) {
            Log.error("No custom data added to custom resources", var11);
         }

         Element var14 = XMLUtil.getOrCreateChild(var2, "CustomIndicators");
         var14.removeContent();

         for (int var15 = 0; var15 < var3.size(); var15++) {
            Element var16 = new Element("CustomIndicator");
            var16.setText((String)var3.get(var15));
            var14.addContent(var16);
         }
      } catch (Exception var12) {
         Log.error("Cannot add custom resources into XML config", var12);
         throw var12;
      }
   }

   private static void loadCustomBlocks(ArrayList<String> var0, String var1, String var2, String var3) {
      for (int var4 = var1.indexOf(var2, 0); var4 > 0; var4 = var1.indexOf(var2, var4 + 1)) {
         try {
            int var5 = var1.indexOf(var3, var4);
            int var6 = var1.indexOf("<Contents>", var4);
            if (var6 > 0 && var6 < var5) {
               int var7 = var1.indexOf("</Contents>", var4);
               var5 = var1.indexOf(var3, var7);
            }

            if (var5 > 0) {
               String var12 = var1.substring(var4, var5 + var3.length());
               Element var8 = XMLUtil.stringToElement(var12);
               String var9 = var8.getAttributeValue("key");
               boolean var10 = XMLUtil.getBooleanAttr(var8, "use", true);
               if (!var0.contains(var9) && var10) {
                  var0.add(var9);
               }
            }
         } catch (Exception var11) {
            Log.error("Checking custom block resource failed", var11);
         }
      }
   }

   public static Element checkProjectResources(ArrayList<Element> var0) throws Exception {
      Element var1 = new Element("Resources");
      Element var2 = XMLUtil.getOrCreateChild(var1, "CustomIndicators");
      Element var3 = XMLUtil.getOrCreateChild(var1, "CustomSnippets");
      Element var4 = XMLUtil.getOrCreateChild(var1, "CustomBlocks");

      for (int var5 = 0; var5 < var0.size(); var5++) {
         Element var6 = (Element)var0.get(var5);
         Element var7 = XMLUtil.getOrCreateChild(var6, "Resources");

         try {
            Element var8 = XMLUtil.getOrCreateChild(var7, "CustomIndicators");
            List var9 = var8.getChildren("CustomIndicator");

            for (int var10 = var9.size() - 1; var10 >= 0; var10--) {
               Element var11 = (Element)var9.get(var10);
               String var12 = var11.getText();
               if (CustomDataManager.checkDataExists(var12)) {
                  var8.removeContent(var11);
               } else {
                  var11.setAttribute("status", "0");
                  var2.addContent(var11.clone());
               }
            }

            ArrayList var22 = new ArrayList();
            ArrayList var23 = XMLUtil.getNestedElements(var6, "Block");

            for (int var24 = 0; var24 < var23.size(); var24++) {
               Element var13 = (Element)var23.get(var24);
               boolean var14 = XMLUtil.getBooleanAttr(var13, "use", false);
               if (var14) {
                  String[] var15 = var13.getAttributeValue("key").split("\\.");
                  String var16 = var15[var15.length - 1];
                  if (!keyIsIgnored(var16)
                     && var16 != null
                     && !var16.startsWith("CBlock")
                     && !var16.startsWith("talib_")
                     && !Blocks.exists(var16)
                     && !var22.contains(var16)) {
                     var22.add(var16);
                     Element var17 = new Element("CustomSnippet");
                     var17.setText(var16);
                     var3.addContent(var17.clone());
                  }
               }
            }

            Element var25 = var7.getChild("CustomBlocks");
            if (var25 != null) {
               List var26 = var25.getChildren("Item");

               for (int var27 = 0; var27 < var26.size(); var27++) {
                  Element var28 = (Element)var26.get(var27);
                  byte var29 = -1;
                  String var30 = null;
                  String var18 = var28.getAttributeValue("key");
                  Element var19 = CustomBlocks.findByKey(var18);
                  if (var19 == null) {
                     Log.debug(String.format("CB '%s' / '%s' is missing", var18, XMLUtil.getAttr(var28, "name", "NA")));
                     var29 = 0;
                  } else {
                     boolean var20 = ResourcesCustomBlocks.areIdentical(var28, var19);
                     if (var20) {
                        if (!XMLUtil.getAttr(var28, "name").equals(XMLUtil.getAttr(var19, "name"))) {
                           var29 = 3;
                           var30 = XMLUtil.getAttr(var19, "name");
                        }
                     } else {
                        var29 = 1;
                     }
                  }

                  if (var29 != -1) {
                     Element var31 = var28.clone();
                     var31.setAttribute("status", var29 + "");
                     if (var30 != null) {
                        var31.setAttribute("differentName", var30);
                     }

                     var4.addContent(var31);
                  }
               }
            }
         } catch (Exception var21) {
            Log.error("Cannot check custom resources from XML config", var21);
            throw new Exception("Cannot check XML config custom resources - " + var21.getMessage());
         }
      }

      return var1;
   }

   private static boolean keyIsIgnored(String var0) {
      switch (var0) {
         case "ExitAfterBars":
         case "MoveSL2BE":
         case "SL2BEAddPips":
         case "StopLoss":
         case "ProfitTarget":
         case "TrailingStop":
         case "TrailingActivation":
         case "_ExitRule_":
         case "_ExitEOD_":
            return true;
         default:
            return false;
      }
   }

   public static Element checkStrategyResources(Element var0) throws Exception {
      Element var1 = new Element("Resources");
      Element var2 = XMLUtil.getOrCreateChild(var1, "CustomBlocks");
      Element var3 = XMLUtil.getOrCreateChild(var1, "RandomGroups");
      byte var4 = -1;
      String var5 = null;
      Element var6 = var0.getChild("CustomBlocks");
      if (var6 != null) {
         List var7 = var6.getChildren("Item");

         for (int var8 = 0; var8 < var7.size(); var8++) {
            var4 = -1;
            var5 = null;
            Element var9 = (Element)var7.get(var8);
            String var10 = var9.getAttributeValue("key");
            Element var11 = CustomBlocks.findByKey(var10);
            if (var11 == null) {
               Log.debug(String.format("CB '%s' / '%s' is missing", var10, XMLUtil.getAttr(var9, "name", "NA")));
               var4 = 0;
            } else {
               boolean var12 = ResourcesCustomBlocks.areIdentical(var9, var11);
               if (var12) {
                  if (!XMLUtil.getAttr(var9, "name").equals(XMLUtil.getAttr(var11, "name"))) {
                     var4 = 3;
                     var5 = XMLUtil.getAttr(var11, "name");
                  }
               } else {
                  var4 = 1;
               }
            }

            if (var4 != -1) {
               Element var24 = var9.clone();
               var24.setAttribute("status", var4 + "");
               if (var5 != null) {
                  var24.setAttribute("differentName", var5);
               }

               var2.addContent(var24);
            }
         }
      }

      Element var19 = var0.getChild("RandomGroups");
      if (var19 != null) {
         List var20 = RandomGroups.listGroups();
         List var21 = var19.getChildren("Group");

         for (int var22 = 0; var22 < var21.size(); var22++) {
            var4 = -1;
            var5 = null;
            Element var23 = (Element)var21.get(var22);
            String var25 = XMLUtil.getAttr(var23, "id");
            Element var13 = RandomGroups.findByID(var25, var20);
            if (var13 == null) {
               Log.debug(String.format("RG '%s' / '%s' is missing", var25, XMLUtil.getAttr(var23, "name", "NA")));
               var4 = 0;
            } else {
               boolean var14 = ResourcesRandomGroups.areIdentical(var23, var13);
               if (var14) {
                  if (!XMLUtil.getAttr(var23, "name").equals(XMLUtil.getAttr(var13, "name"))) {
                     var4 = 3;
                     var5 = XMLUtil.getAttr(var13, "name");
                  }
               } else {
                  var4 = 1;
               }
            }

            if (var4 != -1) {
               Element var26 = var23.clone();
               var26.setAttribute("status", var4 + "");
               if (var5 != null) {
                  var26.setAttribute("differentName", var5);
               }

               var3.addContent(var26);
            }
         }
      }

      return var1;
   }

   public static Element resolveStrategyResources(Element var0, Element var1) throws Exception {
      if (var0 == null) {
         return var1;
      }

      boolean var2 = false;
      Element var3 = null;
      Element var4 = var0.getChild("CustomBlocks");
      if (var4 != null) {
         var3 = CustomBlocks.get();
         List var5 = var4.getChildren("Item");

         for (int var6 = 0; var6 < var5.size(); var6++) {
            Element var7 = (Element)var5.get(var6);
            String var8 = XMLUtil.getAttr(var7, "action");
            String var9 = XMLUtil.getAttr(var7, "name");
            String var10 = XMLUtil.getAttr(var7, "key");
            String var11 = XMLUtil.getAttr(var7, "differentName", null);

            try {
               if (var11 != null) {
                  if (var1 != null) {
                     useCustomBlockFromSQ(var10, var1);
                  }
               } else if (!var8.equals("ignore")) {
                  if (var8.equals("add") || var8.equals("useFromConfig")) {
                     List var12 = var3.getChildren("Item");

                     for (int var13 = 0; var13 < var12.size(); var13++) {
                        Element var14 = (Element)var12.get(var13);
                        String var15 = var14.getAttributeValue("key");
                        if (var15 != null && var15.equals(var10)) {
                           var3.removeContent(var14);
                           break;
                        }
                     }

                     Element var38 = var7.clone();

                     try {
                        String var40 = var9;
                        int var42 = 1;

                        while (CustomBlocks.findByName(var40, var3) != null) {
                           var40 = var9 + " (" + ++var42 + ")";
                        }

                        if (!var40.equals(var9)) {
                           var38.setAttribute("name", var40);
                        }

                        List var16 = var38.getChildren("Param");
                        Element var17 = var38.getChild("Contents");
                        List var18 = var17.getChildren("Param");
                        ArrayList var19 = XMLUtil.getNestedElements(var38.getChild("Contents"), "Param");

                        for (int var20 = var18.size() - 1; var20 >= 0; var20--) {
                           Element var21 = (Element)var18.get(var20);
                           if (var21.getAttributeValue("name").equals("Chart")) {
                              var17.removeContent(var21);
                           }
                        }

                        for (int var52 = 0; var52 < var19.size(); var52++) {
                           Element var53 = (Element)var19.get(var52);
                           if (var53.getAttributeValue("customParam", "false").equals("true")) {
                              String var22 = var53.getText();
                              if (!var22.startsWith("#")) {
                                 var22 = var53.getAttributeValue("value", "null");
                                 if (var22.startsWith("#")) {
                                    var53.setText(var22);
                                 }
                              }

                              boolean var23 = false;

                              for (int var24 = 0; var24 < var16.size(); var24++) {
                                 Element var25 = (Element)var16.get(var24);
                                 if (var25.getAttributeValue("key", "").equals(var22)) {
                                    var23 = true;
                                    break;
                                 }
                              }

                              if (!var23) {
                                 var53.setText(var53.getAttributeValue("defaultValue", "0"));
                                 var53.removeAttribute("customParam");
                              }
                           }
                        }
                     } catch (Exception var28) {
                        Log.error(String.format("Error fixing missing CB with key %s.", var10), var28);
                     }

                     var3.addContent(var38);
                     var2 = true;
                  } else if (var8.equals("useFromSQ") && var1 != null) {
                     useCustomBlockFromSQ(var10, var1);
                  }
               }
            } catch (Exception var29) {
               Log.error("Resolving custom block '" + var9 + "' failed", var29);
            }
         }
      }

      boolean var30 = false;
      Element var31 = null;
      Element var32 = var0.getChild("RandomGroups");
      if (var32 != null) {
         var31 = RandomGroups.getXml();
         List var33 = var31.getChildren("Group");
         List var34 = var32.getChildren("Group");

         for (int var35 = 0; var35 < var34.size(); var35++) {
            Element var36 = (Element)var34.get(var35);
            String var37 = XMLUtil.getAttr(var36, "action");
            String var39 = XMLUtil.getAttr(var36, "name");
            String var41 = XMLUtil.getAttr(var36, "id");
            String var43 = XMLUtil.getAttr(var36, "differentName", null);

            try {
               if (var43 == null && !var37.equals("ignore")) {
                  if (var37.equals("add") || var37.equals("useFromConfig")) {
                     for (int var45 = 0; var45 < var33.size(); var45++) {
                        Element var48 = (Element)var33.get(var45);
                        String var50 = var48.getAttributeValue("id");
                        if (var50 != null && var50.equals(var41)) {
                           var31.removeContent(var48);
                           break;
                        }
                     }

                     Element var46 = var36.clone();

                     try {
                        String var49 = var39;
                        int var51 = 1;

                        while (RandomGroups.findByName(var49, var31) != null) {
                           var49 = var39 + " (" + ++var51 + ")";
                        }

                        if (!var49.equals(var39)) {
                           var46.setAttribute("name", var49);
                        }
                     } catch (Exception var26) {
                        Log.error(String.format("Error fixing missing RG with id %s.", var41), var26);
                     }

                     var31.addContent(var46);
                     var30 = true;
                  } else if (var37.equals("useFromSQ") && var1 != null) {
                     Element var44 = RandomGroups.findByID(var41, var33);
                     Element var47 = XMLUtil.findFirst(var1, "RandomGroups");
                     XMLUtil.replaceElementsWithAttribute(var47, "id", var41, var44.clone(), null);
                  }
               }
            } catch (Exception var27) {
               Log.error("Resolving random group '" + var39 + "' failed", var27);
            }
         }
      }

      if (var30) {
         RandomGroups.save(var31);
         SQWebSocketManager.addToDataQueue(
            new DataToSend("randomGroups", new JSONObject().put("blockGroupsXML", XMLUtil.elementToString(RandomGroups.getXml()))), "SQUANT", "AlgoWizard"
         );
      }

      if (var2) {
         CustomBlocks.save(var3);
         SQWebSocketManager.addToDataQueue(
            new DataToSend("customBlocks", new JSONObject().put("customBlocksXML", XMLUtil.elementToString(CustomBlocks.get()))), "SQUANT", "AlgoWizard"
         );
      }

      return var1;
   }

   private static void useCustomBlockFromSQ(String var0, Element var1) throws Exception {
      Element var2 = CustomBlocks.findByKey(var0);
      if (var2 == null) {
         throw new Exception(String.format("Custom block with key '%s' doesn't exist.", var0));
      }

      Element var3 = XMLUtil.findFirst(var1, "CustomBlocks");
      if (var3 != null) {
         XMLUtil.replaceElementsWithAttribute(var3, "key", var0, var2, null);
      }

      Element var4 = var2.clone();
      var4.removeContent(var4.getChild("Contents"));
      List var5 = var4.getChildren("Param");

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Element var7 = (Element)var5.get(var6);
         var7.setText(var7.getAttributeValue("defaultValue", "0"));
      }

      XMLUtil.replaceElementsWithAttribute(var1, "key", var0, var4, "CustomBlocks");
   }
}
