package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.StrategyBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MMFormulaBlock extends FormulaBlock {
   public static final Logger Log = LoggerFactory.getLogger("MMFormulaBlock");

   public abstract double computeSize(StrategyBase var1, String var2, byte var3, double var4, double var6) throws TradingException;

   public double evaluateFormula(StrategyBase var1, String var2, double var3, int var5) throws TradingException {
      throw new TradingException("This method shouldn't be called!");
   }
}
