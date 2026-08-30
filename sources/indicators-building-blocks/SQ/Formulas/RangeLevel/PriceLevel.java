package SQ.Formulas.RangeLevel;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 700, name = "Price level", formula = "RangeLevel")
public class PriceLevel extends FormulaBlock {
   @Parameter
   public IBlock Value;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      return this.Value.evaluateBlock();
   }
}
