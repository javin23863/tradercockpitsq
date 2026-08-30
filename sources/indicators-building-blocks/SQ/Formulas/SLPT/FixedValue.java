package SQ.Formulas.SLPT;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SLPTValue;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 300, name = "Fixed value (in pips)", formula = "SLPT")
public class FixedValue extends FormulaBlock {
   @Parameter(defaultValue = "50", minValue = 1.0, builderMinValue = 5.0, builderMaxValue = 500.0, maxValue = 9999999.0, step = 1.0, postfix = "pips")
   @SLPTValue(-1000)
   public double Value;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      double var6 = var1.convertPipsToRealPrice(var2, this.Value);
      return var5 > 0 ? var3 + var6 : var3 - var6;
   }
}
