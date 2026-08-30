package SQ.Formulas.SLPT;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SLPTValue;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 300, name = "Percent value", formula = "SLPT")
public class PctValue extends FormulaBlock {
   @Parameter(defaultValue = "5", minValue = 1.0, builderMinValue = 5.0, builderMaxValue = 50.0, maxValue = 99.0, step = 0.1, postfix = "%")
   @SLPTValue(-4000)
   public double Value;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      double var6 = var3 / 100.0 * this.Value;
      return var5 > 0 ? var3 + var6 : var3 - var6;
   }
}
