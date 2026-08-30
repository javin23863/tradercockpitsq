package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(EXP) Exponential", display = "Exponential(#Value#)", returnType = 6)
@Help("Exponential value of a number")
@SortOrder(700)
@IgnoreInBuilder
public class Exponential extends ValueBlock {
   @Parameter
   public IBlock Value;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return Math.exp(this.Value.evaluateBlock(var1));
   }
}
