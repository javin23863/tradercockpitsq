package com.strategyquant.tradinglib.generator;

import org.jdom2.Element;

public class RuleType {
   public static final int Unknown = 0;
   public static final int Entry = 1;
   public static final int Exit = 2;

   public static boolean isConditionElement(String var0) {
      return var0.equals("If");
   }

   public static boolean isActionElement(String var0) {
      return var0.equals("Then");
   }

   public static int recognizeRuleTypeFromParents(Element var0) {
      int var1 = 0;
      var1++;
      Element var2 = var0.getParentElement();
      if (var2 == null) {
         return 0;
      } else {
         String var3 = var2.getName();
         if (var3.equals("signal")) {
            return recognizeTypeFromSignal(var2);
         } else if (var3.equals("Rule")) {
            return recognizeTypeFromRule(var2);
         } else if (var3.equals("Rules")) {
            return 0;
         } else {
            return var1 > 20 ? 0 : recognizeRuleTypeFromParents(var2);
         }
      }
   }

   private static int recognizeTypeFromRule(Element var0) {
      String var1 = var0.getAttributeValue("name");
      if (var1 == null) {
         return 0;
      } else {
         var1 = var1.toLowerCase();
         if (var1.contains("entry")) {
            return 1;
         } else {
            return var1.contains("exit") ? 2 : 0;
         }
      }
   }

   private static int recognizeTypeFromSignal(Element var0) {
      String var1 = var0.getAttributeValue("variable");
      if (var1 == null) {
         return 0;
      }

      switch (var1) {
         case "33333333-1111-1111-3333-333333333333":
         case "33333333-2222-1111-3333-333333333333":
            return 1;
         case "33333333-1111-2222-3333-333333333333":
         case "33333333-2222-2222-3333-333333333333":
            return 2;
         default:
            return 0;
      }
   }
}
