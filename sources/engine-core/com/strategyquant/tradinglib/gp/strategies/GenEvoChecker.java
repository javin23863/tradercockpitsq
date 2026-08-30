package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.XMLUtil;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenEvoChecker {
   public static final Logger Log = LoggerFactory.getLogger("GenEvoChecker");

   public static Element fixZeroPeriodsAndExits(Element var0) {
      if (var0 == null) {
         return null;
      }

      List var1 = var0.getChildren();
      if (var1 != null && !var1.isEmpty()) {
         for (int var2 = 0; var2 < var1.size(); var2++) {
            Element var3 = (Element)var1.get(var2);
            if (var3.getName().equals("Param")) {
               String var4 = var3.getAttributeValue("key");
               if (var4 != null && var4.equals("#Period#") && !verifyParam(var3)) {
                  return null;
               }
            } else if (var3.getName().equals("Formula")) {
               String var5 = var3.getAttributeValue("key");
               if (var5.equals("SQ.Formulas.SLPT.FixedValue")) {
                  if (!verifyFormulaParam(var3, "#Value#")) {
                     return null;
                  }
               } else if (var5.equals("SQ.Formulas.SLPT.ATRBasedValue")) {
                  if (!verifyFormulaParam(var3, "#Value#")) {
                     return null;
                  }

                  if (!verifyFormulaParam(var3, "#AtrPeriod#")) {
                     return null;
                  }
               }
            }

            fixZeroPeriodsAndExits(var3);
         }

         return var0;
      } else {
         return var0;
      }
   }

   private static boolean verifyFormulaParam(Element var0, String var1) {
      Element var2 = XMLUtil.getItemParameterNoException(var0, var1);
      return var2 != null ? verifyParam(var2) : true;
   }

   private static boolean verifyParam(Element var0) {
      String var1 = var0.getAttributeValue("variable");
      if (var1 == null || !var1.equals("true")) {
         String var2 = var0.getText();
         if (var2 != null && !var2.isBlank()) {
            try {
               double var3 = Double.parseDouble(var2);
               if (var3 == 0.0) {
                  return false;
               }
            } catch (NumberFormatException var5) {
               return true;
            }
         }
      }

      return true;
   }

   public static boolean passedRRRCheck(Element var0, boolean var1, double var2, double var4) {
      if (!var1) {
         return true;
      }

      Element var6 = XMLUtil.findFirstWithKey(var0, "Param", "#StopLoss.StopLoss#");
      if (var6 == null) {
         return true;
      }

      Element var7 = XMLUtil.findFirstWithKey(var0, "Param", "#ProfitTarget.ProfitTarget#");
      if (var7 == null) {
         return true;
      }

      Element var8 = var6.getChild("Formula");
      if (var8 == null) {
         return true;
      }

      String var9 = var8.getAttributeValue("key");
      Element var10 = var7.getChild("Formula");
      if (var10 == null) {
         return true;
      }

      String var11 = var8.getAttributeValue("key");
      if (!var9.equals(var11)) {
         return true;
      }

      Element var12 = XMLUtil.findFirstWithKey(var8, "Param", "#Value#");
      if (var12 == null) {
         return true;
      }

      Element var13 = XMLUtil.findFirstWithKey(var10, "Param", "#Value#");
      if (var13 == null) {
         return true;
      }

      double var14 = parseSLPTValue(var12.getText());
      double var16 = parseSLPTValue(var13.getText());
      double var18 = var14 / var16;
      return !(var18 < var2) && !(var18 > var4);
   }

   public static boolean passedRRRCheck2(Element var0, boolean var1, double var2, double var4) {
      if (!var1) {
         return true;
      }

      Element var6 = XMLUtil.findFirstWithKey(var0, "Param", "#StopLoss.StopLoss#");
      if (var6 == null) {
         return true;
      }

      Element var7 = XMLUtil.findFirstWithKey(var0, "Param", "#ProfitTarget.ProfitTarget#");
      if (var7 == null) {
         return true;
      }

      Element var8 = var6.getChild("Formula");
      if (var8 == null) {
         return true;
      }

      String var9 = var8.getAttributeValue("key");
      Element var10 = var7.getChild("Formula");
      if (var10 == null) {
         return true;
      }

      String var11 = var8.getAttributeValue("key");
      if (!var9.equals(var11)) {
         return true;
      }

      Element var12 = XMLUtil.findFirstWithKey(var8, "Param", "#Value#");
      if (var12 == null) {
         return true;
      }

      Element var13 = XMLUtil.findFirstWithKey(var10, "Param", "#Value#");
      if (var13 == null) {
         return true;
      }

      double var14 = parseSLPTValue(var12.getText());
      double var16 = parseSLPTValue(var13.getText());
      double var18 = var14 / var16;
      if (!(var18 < var2) && !(var18 > var4)) {
         return true;
      }

      Log.info("RRR mutation bad detected {} (limit {})= {} / {}", new Object[]{var18, var2, var14, var16});
      return false;
   }

   private static double parseSLPTValue(String var0) {
      try {
         return Double.parseDouble(var0);
      } catch (Exception var2) {
         return 0.0;
      }
   }
}
