package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import java.io.IOException;
import java.util.ArrayList;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyStandardTemplateSQ4Fuzzy extends StrategyStandardTemplateSQ4 {
   public static final Logger Log = LoggerFactory.getLogger("StrategyStandardTemplateSQ4Fuzzy");
   private int minTrueEntry;
   private int minTrueExit;

   public StrategyStandardTemplateSQ4Fuzzy(Element var1, IRandomGenerator var2) throws JDOMException, IOException, Exception {
      super(var1, var2);
      Element var3 = var1.getChild("WhatToBuild");
      int var4 = this.getIntAttribute(var3.getChild("StrategyType").getAttributeValue("minTrue"), 0, 100, 70);
      int var5 = this.getIntAttribute(var3.getChild("StrategyType").getAttributeValue("maxTrue"), 0, 100, 70);
      if (var4 > var5) {
         int var6 = var4;
         var4 = var5;
         var5 = var6;
      }

      if (var4 == var5) {
         this.minTrueEntry = var4;
         this.minTrueExit = var4;
      } else {
         this.minTrueEntry = var4 + var2.nextInt(var5 - var4);
         this.minTrueExit = var4 + var2.nextInt(var5 - var4);
      }
   }

   public static void ramdomizeFuzzyPercentags(Element var0, int var1, int var2, IRandomGenerator var3) {
      if (var1 > var2) {
         int var6 = var1;
         var1 = var2;
         var2 = var6;
      }

      int var4;
      int var5;
      if (var1 == var2) {
         var4 = var1;
         var5 = var1;
      } else {
         var4 = var1 + var3.nextInt(var2 - var1);
         var5 = var1 + var3.nextInt(var2 - var1);
      }

      ArrayList var12 = XMLUtil.getNestedElements(var0, "signal");

      for (int var7 = 0; var7 < var12.size(); var7++) {
         Element var8 = (Element)var12.get(var7);
         String var9 = var8.getAttributeValue("variable");
         if (var9 != null) {
            switch (var9) {
               case "33333333-1111-1111-3333-333333333333":
                  var8.setAttribute("minTrue", Integer.toString(var4));
                  break;
               case "33333333-2222-1111-3333-333333333333":
                  var8.setAttribute("minTrue", Integer.toString(var4));
                  break;
               case "33333333-1111-2222-3333-333333333333":
                  var8.setAttribute("minTrue", Integer.toString(var5));
                  break;
               case "33333333-2222-2222-3333-333333333333":
                  var8.setAttribute("minTrue", Integer.toString(var5));
            }
         }
      }
   }

   private int getIntAttribute(String var1, int var2, int var3, int var4) {
      if (var1 == null) {
         return var4;
      }

      try {
         int var5 = Integer.parseInt(var1);
         if (var5 < var2) {
            var5 = var2;
         }

         if (var5 > var3) {
            var5 = var3;
         }

         return var5;
      } catch (NumberFormatException var6) {
         return var4;
      }
   }

   @Override
   protected Element getSignalRule() throws JDOMException, IOException, Exception {
      Element var1 = super.getSignalRule();
      var1.setAttribute("type", "SignalFuzzy");
      return var1;
   }

   @Override
   protected Element getEntrySignalCondition(int var1, String var2, String var3) throws JDOMException, IOException, Exception {
      Element var4 = super.getEntrySignalCondition(var1, var2, var3);
      var4.setAttribute("minTrue", Integer.toString(this.minTrueEntry));
      return var4;
   }

   @Override
   protected Element getExitSignalCondition(int var1, String var2, String var3) throws JDOMException, IOException, Exception {
      Element var4 = super.getExitSignalCondition(var1, var2, var3);
      var4.setAttribute("minTrue", Integer.toString(this.minTrueExit));
      return var4;
   }
}
