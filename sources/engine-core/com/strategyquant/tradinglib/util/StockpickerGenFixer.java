package com.strategyquant.tradinglib.util;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.simulator.Engines;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StockpickerGenFixer {
   public static final Logger Log = LoggerFactory.getLogger("StockpickerGenFixer");

   public static void fixEntryExitTriggers(IRandomGenerator var0, Element var1, int var2, int var3) throws Exception {
      var1.setAttribute("entryTriggeredAt", translateEntryExitType(var0, var2));
      var1.setAttribute("exitTriggeredAt", translateEntryExitType(var0, var3));
   }

   private static String translateEntryExitType(IRandomGenerator var0, int var1) throws Exception {
      if (var1 == 0) {
         switch (var0.nextInt(3)) {
            case 1:
               var1 = 15;
               break;
            case 2:
               var1 = 5;
               break;
            default:
               var1 = 10;
         }
      }

      switch (var1) {
         case 5:
            return "OnBarOpen";
         case 10:
            return "OnBarClose";
         case 15:
            return "BeforeBarOpen";
         default:
            throw new Exception("Unknown order type: " + var1);
      }
   }

   public static void fixExits(IRandomGenerator var0, Element var1, int var2, int var3) throws Exception {
      int var4 = getProbabilityFromAttribute(var1, "probabilityExitRule", 0);
      int var5 = getProbabilityFromAttribute(var1, "probabilityEODRule", 0);
      ArrayList var6 = new ArrayList();
      XMLUtil.findAll(var1, "Exit", var6);
      if (var6.size() > 0) {
         for (Element var8 : var6) {
            String var9 = var8.getParentElement().getAttributeValue("name");
            int var10 = var9.equalsIgnoreCase("long") ? var2 : var3;
            String var11 = translateEndOfDay(var0, var10, var5);
            String var12 = var8.getAttributeValue("atEndOfDay");
            if (var12 != null) {
               var8.setAttribute("atEndOfDay", var11);
            } else {
               probabilityExitRemove(var0, var8, var4);
            }
         }
      }
   }

   private static String translateEndOfDay(IRandomGenerator var0, int var1, int var2) throws Exception {
      if (var2 == 0) {
         return "None";
      } else {
         return var2 < 100 && var0.nextInt(100) < var2 ? "None" : "DayClose";
      }
   }

   private static void probabilityExitRemove(IRandomGenerator var0, Element var1, int var2) {
      boolean var3 = false;
      int var4 = -1;
      if (var2 == 0) {
         var3 = true;
      } else if (var2 < 100) {
         var4 = var0.nextInt(100);
         if (var4 < var2) {
            var3 = true;
         }
      }

      if (var3 && !wasGenerated(var1)) {
         var3 = false;
      }

      if (var3) {
         var1.removeContent();
      }
   }

   private static boolean wasGenerated(Element var0) {
      boolean var1 = false;

      for (Element var4 : var0.getChildren()) {
         String var5 = var4.getAttributeValue("generated");
         if (var5 != null && !var5.equals("none")) {
            var1 = true;
            break;
         }
      }

      return var1;
   }

   private static int getProbabilityFromAttribute(Element var0, String var1, int var2) {
      String var3 = var0.getAttributeValue(var1);
      return var3 == null ? var2 : Integer.parseInt(var3);
   }

   public static void fixMaxOpenPositions(Element var0, int var1, int var2) {
      if (XMLUtil.getBooleanAttr(var0, "singleAsset", false)) {
         var1 = 1;
         var2 = 1;
      }

      if (var1 > 0 || var2 > 0) {
         ArrayList var4 = new ArrayList();
         XMLUtil.findAll(var0, "Rule", var4);

         for (Element var6 : var4) {
            String var7 = var6.getAttributeValue("type");
            if (var7 != null && var7.equals("StockpickerPS")) {
               String var8 = var6.getAttributeValue("name");
               byte var3;
               if (var8.equalsIgnoreCase("Position Score Long")) {
                  var3 = 1;
               } else {
                  var3 = -1;
               }

               ArrayList var9 = new ArrayList();
               XMLUtil.findAllWithKey(var6, "Item", "AssignVariable", var9);

               for (Element var11 : var9) {
                  if (XMLUtil.findFirstWithKey(var11, "Param", "#MaxOpenPositions#") != null) {
                     Element var12 = XMLUtil.findFirstWithKey(var11, "Param", "#Value#");
                     if (var12 != null) {
                        if (var3 == 1) {
                           var12.setText(Integer.toString(var1));
                        } else {
                           var12.setText(Integer.toString(var2));
                        }
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   public static int recognizeEngine(Element var0) {
      Element var1 = var0.getChild("Strategy");
      String var2 = var1.getAttributeValue("engine");
      return Engines.getEngine(var2);
   }
}
