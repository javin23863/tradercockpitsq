package com.strategyquant.plugin.Servlet.impl.RenameTool;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ResultsGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameToolGenerator {
   private static final Logger Log = LoggerFactory.getLogger(RenameToolGenerator.class);
   private static String NO_VALUE = "NN";
   private static int strategyNumber = 0;

   public static void processStrategy(ResultsGroup var0) throws Exception {
      String var1 = var0.getName();
      String var2 = "";
      Element var3 = var0.getStrategyXml();
      Element var4 = XMLUtil.stringToElement(var0.getLastSettings());
      String var5 = NO_VALUE;
      String var6 = NO_VALUE;
      String var7 = NO_VALUE;

      try {
         Element var8 = var4.getChild("Data");
         Element var9 = var8.getChild("Setups");
         Element var10 = var9.getChild("Setup");
         List var11 = var10.getChildren("Chart");

         for (int var12 = 0; var12 < var11.size(); var12++) {
            Element var13 = (Element)var11.get(var12);
            String var14 = var13.getAttributeValue("symbol");
            String var15 = var13.getAttributeValue("timeframe");
            if (var12 == 0) {
               var5 = var14;
               var6 = var15 != null ? var15 : NO_VALUE;
            } else if (var12 == 1) {
               var7 = var15 != null ? var15 : NO_VALUE;
               break;
            }
         }
      } catch (Exception var33) {
         Log.error("Cannot read market info", var33);
      }

      String var43 = getValue(RenameToolSettings.MN_Markets, var5);
      String var44 = var6 + (var7 != null && !var7.equals(NO_VALUE) ? "," + var7 : "");
      String var45 = getValue(RenameToolSettings.MN_TimeFrames, var44);
      String var46 = getNumber(var43) + getNumber(var45) + getStrategyId(var1);
      Element var47 = XMLUtil.getChildElem(var3, "Strategy");
      ArrayList var48 = XMLUtil.getNestedElements(var47, "Rule");
      String var49 = "SQ3";
      String var50 = "N";
      String var16 = NO_VALUE;

      label212:
      for (int var17 = 0; var17 < var48.size(); var17++) {
         Element var18 = (Element)var48.get(var17);
         String var19 = var18.getAttributeValue("type");
         if (var19 != null) {
            if (var19.equals("Signal")) {
               var49 = "SQX";
            } else if (var19.equals("SignalFuzzy")) {
               var49 = "FUZ";
            }

            Element var20 = var18.getChild("Then");
            if (var20 != null) {
               Element var21 = var20.getChild("Item");
               if (var21 != null) {
                  String var22 = var21.getAttributeValue("mI");
                  if (var22 != null && var22.equals("Open")) {
                     String var23 = var21.getAttributeValue("key");
                     var50 = getValue(RenameToolSettings.SN_OrderTypes, var23);
                     var50 = !var50.equals(NO_VALUE) ? var50 : "N";
                     List var24 = var21.getChildren("Param");

                     for (int var25 = 0; var25 < var24.size(); var25++) {
                        Element var26 = (Element)var24.get(var25);
                        String var27 = var26.getAttributeValue("key");
                        if (var27 != null && var27.equals("#Price#")) {
                           ArrayList var28 = XMLUtil.getNestedElements(var26, "Item");

                           for (int var29 = 0; var29 < var28.size(); var29++) {
                              Element var30 = (Element)var28.get(var29);
                              String var31 = var30.getAttributeValue("mI");
                              if (var31 != null && !var31.equals("Functions") && !var31.equals("Comparisons")) {
                                 String var32 = var30.getAttributeValue("key");
                                 var16 = getValue(RenameToolSettings.SN_Indicators, var32);
                                 break label212;
                              }
                           }
                           break label212;
                        }
                     }
                     break;
                  }
               }
            }
         }
      }

      String var52 = "";
      String var53 = "";
      Element var54 = var4.getChild("Options");
      if (var54 != null) {
         Element var55 = var54.getChild("BuildTradingOptions");
         if (var55 != null) {
            Element var57 = var55.getChild("Params");
            if (var57 != null) {
               List var59 = var57.getChildren("Param");
               boolean var61 = false;
               boolean var63 = false;
               String var65 = "";
               String var67 = "";

               for (int var69 = 0; var69 < var59.size(); var69++) {
                  Element var70 = (Element)var59.get(var69);
                  String var71 = var70.getAttributeValue("key");
                  if (var71 != null) {
                     switch (var71) {
                        case "ExitAtEndOfDay":
                           var52 = isUsed(var70) ? "CD" : var52;
                           break;
                        case "ExitOnFriday":
                           var52 = var52.isEmpty() && isUsed(var70) ? "CF" : var52;
                           break;
                        case "LimitTimeRange":
                           var61 = isUsed(var70);
                           break;
                        case "SignalTimeRangeFrom":
                           var65 = getTimeRangeHour(var70);
                           break;
                        case "SignalTimeRangeTo":
                           var67 = getTimeRangeHour(var70);
                           break;
                        case "ExitAtEndOfRange":
                           var63 = isUsed(var70);
                     }
                  }
               }

               if (var61) {
                  var53 = var65 + var67 + (var63 ? "C" : "");
               }
            }
         }
      }

      var2 = var2 + getValue(RenameToolSettings.SN_Markets, var5);
      var2 = var2 + "_" + getValue(RenameToolSettings.SN_TimeFrames, var6);
      var2 = var2 + (var7 != null && !var7.equals(NO_VALUE) ? getValue(RenameToolSettings.SN_TimeFrames, var7) : "");
      var2 = var2 + "_" + var46;
      var2 = var2 + "_" + var50;
      var2 = var2 + "_" + var16;
      var2 = var2 + (!var52.isEmpty() ? "_" + var52 : "");
      var2 = var2 + (!var53.isEmpty() ? "_" + var53 : "");
      var2 = var2 + "_" + var49;
      Log.info("Original name: " + var1 + " -> generater name: " + var2);
      Element var56 = var47.getChild("Variables");
      if (var56 != null) {
         List var58 = var56.getChildren("variable");

         for (int var60 = 0; var60 < var58.size(); var60++) {
            Element var62 = (Element)var58.get(var60);
            Element var64 = var62.getChild("name");
            if (var64 != null) {
               String var66 = var64.getTextTrim();
               if (var66 != null && var66.equals("MagicNumber")) {
                  Element var68 = XMLUtil.tryAddElement(var62, "value");
                  var68.setText(var46);
                  break;
               }
            }
         }
      } else {
         Log.error("Cannot set magic number - element Variables is missing in strategy config");
      }

      var0.setName(var2);
   }

   private static String getValue(HashMap<String, String> var0, String var1) {
      if (var1 != null && !var1.equals(NO_VALUE)) {
         String var2 = null;
         int var3 = 0;

         for (String var5 : var0.keySet()) {
            if (var1.equals(var5)) {
               return (String)var0.get(var5);
            }

            if (var1.contains(var5)) {
               int var6 = var5.length();
               if (var6 > var3) {
                  var2 = (String)var0.get(var5);
                  var3 = var6;
               }
            }
         }

         return var2 != null ? var2 : NO_VALUE;
      } else {
         return NO_VALUE;
      }
   }

   private static String getNumber(String var0) {
      if (var0 != null && !var0.equals(NO_VALUE)) {
         String var1 = var0;

         while (!var1.isEmpty() && var1.startsWith("0")) {
            var1 = var1.substring(1);
         }

         try {
            Integer.valueOf(var1);
            return var0;
         } catch (Exception var3) {
            return "99";
         }
      } else {
         return "99";
      }
   }

   private static String getStrategyId(String var0) {
      if (var0 != null && var0.contains("Strategy ")) {
         int var1 = var0.indexOf("Strategy ");
         String var2 = var0.substring(var1 + "Strategy ".length());
         int var3 = 0;

         for (int var4 = 0; var4 < var2.length(); var4++) {
            try {
               String var5 = var2.substring(var4, var4 + 1);
               int var6 = Integer.parseInt(var5);
               var3 = var3 * 10 + var6;
            } catch (Exception var7) {
            }
         }

         var3 %= 100000;
         return String.format("%05d", var3);
      } else {
         strategyNumber++;
         return String.format("%05d", strategyNumber % 100000);
      }
   }

   private static boolean isUsed(Element var0) {
      String var1 = var0.getTextTrim();
      boolean var2 = false;

      try {
         var2 = Boolean.parseBoolean(var1);
      } catch (Exception var4) {
      }

      return var2;
   }

   private static String getTimeRangeHour(Element var0) {
      try {
         int var1 = Integer.parseInt(var0.getValue());
         return String.format("%02d", var1 / 3600);
      } catch (Exception var2) {
         return NO_VALUE;
      }
   }
}
