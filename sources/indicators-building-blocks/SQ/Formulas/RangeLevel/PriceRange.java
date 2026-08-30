package SQ.Formulas.RangeLevel;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 500, name = "Range in pips", formula = "RangeLevel")
@IgnoreInBuilder
public class PriceRange extends FormulaBlock {
   @Parameter(postfix = "pips")
   public IBlock Value;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      double var6 = this.Value.evaluateBlock();
      double var8 = var1.convertPipsToRealPrice(var2, var6);
      return this.getLevelByDirection(var5, var3, var8);
   }

   private double getLevelByDirection(int var1, double var2, double var4) {
      return var1 == 1 ? var2 - var4 : var2 + var4;
   }
}
