package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConditionBlock extends StandardBlock {
   public static final Logger Log = LoggerFactory.getLogger("ConditionBlock");

   public abstract boolean OnBlockEvaluate() throws TradingException;

   public double evaluateBlock() throws TradingException {
      return this.OnBlockEvaluate() ? 1.0 : 0.0;
   }

   public double evaluateBlock(int var1) throws TradingException {
      throw new TradingException("This shouldn't be called!");
   }
}
