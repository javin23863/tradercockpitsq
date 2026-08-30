package SQ.Formulas.CloseSize;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 100, name = "Full position", formula = "CloseSize")
public class FullPosition extends FormulaBlock {
   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      return 0.0;
   }
}
