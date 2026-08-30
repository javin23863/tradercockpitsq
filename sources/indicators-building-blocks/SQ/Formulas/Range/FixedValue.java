package SQ.Formulas.Range;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 200, name = "Fixed value (in pips)", formula = "Range")
public class FixedValue extends FormulaBlock {
   @Parameter(
      defaultValue = "50",
      minValue = 1.0,
      maxValue = 9999999.0,
      step = 1.0,
      builderMinValue = 5.0,
      builderMaxValue = 100.0,
      builderStep = 1.0,
      postfix = "pips"
   )
   public double Value;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      return this.Value == 0.0 ? 0.0 : var1.convertPipsToRealPrice(var2, this.Value);
   }
}
