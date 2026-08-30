package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(ABS) Absolute value", display = "Abs(#Value#)", returnType = 6)
@Help("Absolute value of a number")
@SortOrder(800)
@IgnoreInBuilder
public class Abs extends ValueBlock {
   @Parameter
   public IBlock Value;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return Math.abs(this.Value.evaluateBlock(var1));
   }
}
