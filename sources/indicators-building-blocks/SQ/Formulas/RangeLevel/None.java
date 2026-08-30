package SQ.Formulas.RangeLevel;

import SQ.Internal.FormulaBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 100, name = "None", formula = "RangeLevel", noneValue = true)
public class None extends FormulaBlock {
   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      return -9.9999999E7;
   }
}
