package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.generator.GenerateException;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.gp.IEvolutionaryOperator;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixUnusedDependentFormulas implements IEvolutionaryOperator<Node> {
   private static final Logger Log = LoggerFactory.getLogger("FixUnusedDependentFormulas");

   public FixUnusedDependentFormulas() throws GenerateException {
   }

   @Override
   public List<Node> apply(List<Node> var1, IRandomGenerator var2, AbstractFactory<Node> var3, int var4, int var5) throws Exception {
      for (int var6 = 0; var6 < var1.size(); var6++) {
         Node var7 = (Node)var1.get(var6);
         fix(var7.getStrategy());
      }

      return var1;
   }

   public static void fix(Element var0) throws GenerateException {
      List var1 = var0.getChildren();

      for (int var2 = 0; var2 < var1.size(); var2++) {
         Element var3 = (Element)var1.get(var2);
         if (var3.getName().equals("Param")) {
            String var4 = var3.getAttributeValue("key");
            String var5 = null;
            if (var4.equals("#TrailingStop.TrailingActivation#")) {
               var5 = "TrailingStop.TrailingStop";
            }

            if (var4.equals("#MoveSL2BE.SL2BEAddPips#")) {
               var5 = "MoveSL2BE.MoveSL2BE";
            }

            if (var5 != null) {
               Element var6 = getDependentParamFormula(var0, var5);
               if (var6 != null && !isFormulaUsed(var6)) {
                  Element var7 = var3.getChild("Formula");
                  if (var7 != null) {
                     String var8 = var7.getAttributeValue("key");
                     var8 = setNoneFormula(var8);
                     var7.setAttribute("key", var8);
                     var7.removeContent();
                  }
               }
            }
         }

         List var9 = var3.getChildren();
         if (var9 != null) {
            fix(var3);
         }
      }
   }

   private static String setNoneFormula(String var0) {
      int var1 = var0.lastIndexOf(".");
      return var1 > 0 ? var0.substring(0, var1) + ".None" : var0;
   }

   private static boolean isFormulaUsed(Element var0) {
      String var1 = var0.getAttributeValue("key");
      return !var1.endsWith(".None");
   }

   private static Element getDependentParamFormula(Element var0, String var1) {
      List var2 = var0.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("key");
         if (var5 != null && var5.contains(var1)) {
            return var4.getChild("Formula");
         }
      }

      return null;
   }
}
