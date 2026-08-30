package SQ.Formulas.Range;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 500, name = "Range in pips", formula = "Range")
@IgnoreInBuilder
public class PriceRange extends FormulaBlock {
   @Parameter(postfix = "pips")
   public IBlock Value;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      return this.Value.evaluateBlock();
   }
}
