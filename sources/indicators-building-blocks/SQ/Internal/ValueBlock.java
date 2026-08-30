package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ValueBlock extends StandardBlock {
   public static final Logger Log = LoggerFactory.getLogger("ValueBlock");

   public abstract double OnBlockEvaluate(int var1) throws TradingException;

   public double evaluateBlock() throws TradingException {
      return this.OnBlockEvaluate(0);
   }

   public double evaluateBlock(int var1) throws TradingException {
      return this.OnBlockEvaluate(var1);
   }
}
