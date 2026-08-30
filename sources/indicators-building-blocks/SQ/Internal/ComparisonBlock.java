package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ComparisonBlock extends StandardBlock {
   public static final Logger Log = LoggerFactory.getLogger("ComparisonBlock");

   public abstract boolean OnEvaluateComparison() throws TradingException;

   public double evaluateBlock(int var1) throws TradingException {
      return this.OnEvaluateComparison() ? 1.0 : 0.0;
   }

   public double evaluateBlock() throws TradingException {
      return this.OnEvaluateComparison() ? 1.0 : 0.0;
   }
}
