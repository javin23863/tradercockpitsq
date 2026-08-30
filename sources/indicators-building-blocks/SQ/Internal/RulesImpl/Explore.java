package SQ.Internal.RulesImpl;

import SQ.Internal.ITradingOptionsEvaluator;
import SQ.Internal.Rule;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.IBlock;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Explore extends Rule {
   public static final Logger Log = LoggerFactory.getLogger("Explore");
   private ObjectArrayList<IBlock> blocks = null;
   private ObjectArrayList<String> names = null;

   @Override
   public void evaluateRule(int var1, ITradingOptionsEvaluator var2, String var3) throws Exception {
      if (this.Strategy.Explore == null) {
         this.Strategy.Explore = new com.strategyquant.tradinglib.explore.Explore(this.names);
      }

      String var4 = this.Strategy.getSymbol();
      boolean var5 = false;
      long var6 = 0L;
      if (this.Strategy.isStockpicker()) {
         if (this.Strategy.Stockpicker.entryType() == this.Strategy.Stockpicker.strategyTriggeredAt()) {
            var5 = true;
            var6 = this.Strategy.Stockpicker.data.getCurrentTime();
         }
      } else {
         var5 = true;
         var6 = this.Strategy.MarketData.TimeCurrent();
      }

      if (var5) {
         for (int var10 = 0; var10 < this.blocks.size(); var10++) {
            double var8;
            try {
               IBlock var11 = (IBlock)this.blocks.get(var10);
               var8 = var11.evaluateBlock();
            } catch (Exception var12) {
               var8 = 0.0;
            }

            this.Strategy.Explore.add(var4, var6, var10, var8);
         }
      }
   }

   @Override
   protected void parseXml(Element var1) throws BlockDefinitionException {
      this.blocks = new ObjectArrayList();
      this.names = new ObjectArrayList();

      for (Element var3 : var1.getChildren()) {
         if (!var3.getName().contains("Description")) {
            int var4 = 0;

            for (Element var6 : var3.getChildren()) {
               if (!var6.getName().equals("Item")) {
                  throw new BlockDefinitionException("Block has an unallowed name '" + var6.getName() + "'");
               }

               IBlock var7 = Blocks.getBlockObject(var6.getAttributeValue("key"), this.Strategy, var6);
               BuildingBlock var8 = var7.getClass().getAnnotation(BuildingBlock.class);
               if (var8 != null) {
                  var7.setReturnType(var8.returnType());
               }

               String var9 = var6.getAttributeValue("exploreName");
               String var10 = var9 == null ? "Value " + (var4 + 1) : var9;
               this.blocks.add(var7);
               this.names.add(var10);
               var4++;
            }
         }
      }
   }
}
