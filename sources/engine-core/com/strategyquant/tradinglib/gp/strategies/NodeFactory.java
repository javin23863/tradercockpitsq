package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.generator.StrategyGenerator;
import com.strategyquant.tradinglib.generator.StrategyGeneratorCache;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import com.strategyquant.tradinglib.options.parameters.StockpickerOptions;
import com.strategyquant.tradinglib.util.StockpickerGenFixer;
import java.io.IOException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFactory extends AbstractFactory<Node> {
   private static final Logger Log = LoggerFactory.getLogger("NodeFactory");
   private BuildSettings buildSettings;
   private StrategyGenerator strategyGenerator;

   public NodeFactory(BuildSettings var1) {
      this.buildSettings = var1;
   }

   public Node generateRandomCandidate(IRandomGenerator var1) throws Exception {
      Element var2 = this.generateStrategy(var1);
      return new Node(var2);
   }

   private Element generateStrategy(IRandomGenerator var1) throws JDOMException, IOException, Exception {
      if (this.strategyGenerator == null) {
         this.strategyGenerator = StrategyGeneratorCache.getGenerator(
            this.buildSettings.getGeneratorHash(),
            var1,
            this.buildSettings.getStrategyTemplate(),
            this.buildSettings.getReplacements(),
            this.buildSettings.getBuildSettingsMap(),
            this.buildSettings.getATMGenerateConfig()
         );
      }

      Element var2 = this.strategyGenerator.generate("Strategy");
      int var3 = StockpickerGenFixer.recognizeEngine(var2);
      if (var3 == 1316847364 || var3 == -1816889229) {
         this.addStockpickerFixes(var2, var1);
      }

      return var2;
   }

   private void addStockpickerFixes(Element var1, IRandomGenerator var2) throws Exception {
      if (this.buildSettings.tradingOptions != null) {
         boolean var3 = false;
         int var4 = 0;
         int var5 = 0;
         int var6 = 0;
         int var7 = 0;
         int var8 = 0;
         int var9 = 0;

         for (TradingOption var11 : this.buildSettings.tradingOptions) {
            if (var11 instanceof StockpickerOptions) {
               StockpickerOptions var12 = (StockpickerOptions)var11;
               var3 = true;
               var4 = var12.PickerEntryType;
               var5 = var12.PickerExitType;
               var6 = var12.PickerEndOfDayLong;
               var7 = var12.PickerEndOfDayShort;
               var8 = var12.PickerMaxOpenPositionsLong;
               var9 = var12.PickerMaxOpenPositionsShort;
               break;
            }
         }

         if (!var3) {
            return;
         }

         Element var13 = var1.getChild("Strategy");
         StockpickerGenFixer.fixEntryExitTriggers(var2, var13, var4, var5);
         StockpickerGenFixer.fixExits(var2, var13, var6, var7);
         StockpickerGenFixer.fixMaxOpenPositions(var13, var8, var9);
      }
   }

   @Override
   public AbstractFactory<Node> clone() {
      return new NodeFactory(this.buildSettings.clone());
   }

   public Element generateRandomBlock(String var1, Element var2, int var3, int var4) throws Exception {
      if (this.strategyGenerator == null) {
         throw new Exception("StrategyGenerator should be initialized at this point!");
      } else {
         return this.strategyGenerator.generateMutatedRandomBlock(var1, var2, var3, var4);
      }
   }

   @Override
   public void destroy() {
      if (this.strategyGenerator != null) {
         this.strategyGenerator.freeGenerator();
      }
   }

   @Override
   public Element mutateATM(Element var1, IRandomGenerator var2) throws Exception {
      if (this.strategyGenerator == null) {
         throw new Exception("StrategyGenerator should be initialized at this point!");
      } else {
         return this.strategyGenerator.mutateATM(var1, var2);
      }
   }

   public Element verifyParameters(String var1, Element var2, Element var3) throws Exception {
      if (this.strategyGenerator == null) {
         throw new Exception("StrategyGenerator should be initialized at this point!");
      } else {
         return this.strategyGenerator.verifyParameters(var1, var2, var3);
      }
   }
}
