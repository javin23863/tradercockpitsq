package SQ.Formulas.SLPT;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 600, name = "Value from formula (in pips)", formula = "SLPT")
@IgnoreInBuilder
public class ValueFromFormula extends FormulaBlock {
   @Parameter
   public IBlock Value;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      double var6 = this.Value.evaluateBlock();
      double var8 = var1.convertPipsToRealPrice(var2, var6);
      return this.getLevelByDirection(var5, var5, var3, var8);
   }

   private double getLevelByDirection(int var1, int var2, double var3, double var5) {
      return var2 == 1 ? var3 + var5 : var3 - var5;
   }
}
