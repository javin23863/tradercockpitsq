package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.NegatersList;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.generator.Checkers;
import com.strategyquant.tradinglib.generator.CheckersList;
import com.strategyquant.tradinglib.generator.GenerateException;
import com.strategyquant.tradinglib.generator.Negaters;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;
import java.util.ArrayList;
import java.util.HashMap;
import org.jdom2.Element;

public class ReplacementsConfig extends HashMap<String, ReplacementConfig> {
   public NegatersList negatersList;
   public CheckersList checkersList = Checkers.list();

   public ReplacementsConfig(Element var1, BlocksConfig var2, IRandomGenerator var3, SettingsMap var4) throws GenerateException {
      this.negatersList = Negaters.list();

      for (Element var6 : var1.getChildren()) {
         ReplacementConfig var7 = new ReplacementConfig(this, var2, var6, var4, this.checkersList);
         this.put(var6.getAttributeValue("identification"), var7);
      }
   }

   public IBlock negateBlock(IBlock var1, StrategyBase var2) throws BlockDefinitionException, XmlStrategyException {
      return this.negatersList.negate(var1, var2);
   }

   public void checkAllExist(ArrayList<String> var1) throws GenerateException {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         if (!this.containsKey(var1.get(var2))) {
            throw new GenerateException(
               String.format("Bad configuration - identifier '%s' referenced in Random block or parameter doesn't exist in ReplacementsConfig!", var1.get(var2))
            );
         }
      }
   }

   public ReplacementConfig get(String var1) throws GenerateException {
      if (!this.containsKey(var1)) {
         throw new GenerateException("No config for placeholder '" + var1 + "'");
      } else {
         return (ReplacementConfig)super.get(var1);
      }
   }

   public ReplacementParameter findReplacementParamByKey(String var1, String var2) {
      for (ReplacementConfig var4 : this.values()) {
         ReplacementBlock var5 = var4.findBlockByKey(var1);
         if (var5 != null) {
            return var5.findReplacementParamByKey(var2);
         }
      }

      return null;
   }
}
