package com.strategyquant.tradinglib.generator;

import org.jdom2.Element;

public class RulePart {
   public static final int Unknown = 1;
   public static final int Condition = 2;
   public static final int Action = 3;

   public static boolean isConditionElement(String var0) {
      return var0.equals("If");
   }

   public static boolean isActionElement(String var0) {
      return var0.equals("Then");
   }

   public static int recognizeRulePartFromParents(Element var0) {
      Element var1 = var0.getParentElement();
      if (var1 == null) {
         return 1;
      } else if (isConditionElement(var1.getName())) {
         return 2;
      } else {
         return isActionElement(var1.getName()) ? 3 : recognizeRulePartFromParents(var1);
      }
   }
}
