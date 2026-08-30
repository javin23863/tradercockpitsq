package SQ.Formulas.Price;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 400, name = "Use formula", formula = "Price", priceLevel = true)
public class UseFormula extends FormulaBlock {
   @Parameter
   public IBlock Value;
   private int isBoolean = -1;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      return this.Value.evaluateBlock();
   }

   @Override
   public boolean isBooleanValue() {
      this.checkIsBoolean();
      return this.isBoolean > 0;
   }

   private void checkIsBoolean() {
      if (this.isBoolean == -1) {
         if (this.Value.getClass().getCanonicalName().contains("Boolean")) {
            this.isBoolean = 1;
         } else {
            BuildingBlock var1 = this.Value.getClass().getAnnotation(BuildingBlock.class);
            if (var1 != null && var1.returnType() == 3) {
               this.isBoolean = 1;
            } else {
               this.isBoolean = 0;
            }
         }
      }
   }
}
