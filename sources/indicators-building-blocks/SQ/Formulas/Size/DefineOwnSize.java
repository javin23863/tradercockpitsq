package SQ.Formulas.Size;

import SQ.Internal.MMFormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 100, name = "Define own size", formula = "Size")
@IgnoreInBuilder
public class DefineOwnSize extends MMFormulaBlock {
   @Parameter(defaultValue = "0.1", minValue = 0.01, maxValue = 9.99999999E8)
   public double Value;

   @Override
   public double computeSize(StrategyBase var1, String var2, byte var3, double var4, double var6) throws TradingException {
      return this.Value;
   }
}
