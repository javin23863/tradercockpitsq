package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.generator.GenerateException;
import com.strategyquant.tradinglib.generator.StrategyGenerator;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.gp.IEvolutionaryOperator;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixCustomBlocks implements IEvolutionaryOperator<Node> {
   private static final Logger Log = LoggerFactory.getLogger("FixCustomBlocks");

   public FixCustomBlocks() throws GenerateException {
   }

   @Override
   public List<Node> apply(List<Node> var1, IRandomGenerator var2, AbstractFactory<Node> var3, int var4, int var5) throws Exception {
      for (int var6 = 0; var6 < var1.size(); var6++) {
         Node var7 = (Node)var1.get(var6);
         this.fix(var7.getStrategy());
      }

      return var1;
   }

   private void fix(Element var1) throws GenerateException {
      StrategyGenerator.fixValuesInCDataIndy(var1);
      HashMap var2 = new HashMap();
      this.fillCBMap(var1, var2);
      Element var3 = var1.getChild("Strategy");
      Element var4 = var3.getChild("CustomBlocks");
      if (var4 != null) {
         var3.removeChild("CustomBlocks");
      }

      var4 = new Element("CustomBlocks");
      var4.setAttribute("genby", "Neg");
      var3.addContent(var4);
      StrategyGenerator.addBlocksToXML(var4, var2);
   }

   private void fillCBMap(Element var1, HashMap<String, Element> var2) throws GenerateException {
      if (var1.getName().equals("Item")) {
         String var3 = var1.getAttributeValue("key");
         if (var3 != null && var3.startsWith("CBlock")) {
            String var4 = var3;
            if (var4 != null) {
               Element var5 = CustomBlocks.getElement(var4);
               if (var5 == null) {
                  throw new GenerateException("Cannot find definition for Custom Block '" + var4 + "'");
               }

               var2.put(var4, var5);
            }
         }
      }

      List var6 = var1.getChildren();
      if (var6 != null && var6.size() > 0) {
         for (int var7 = 0; var7 < var6.size(); var7++) {
            Element var8 = (Element)var6.get(var7);
            this.fillCBMap(var8, var2);
         }
      }
   }
}
