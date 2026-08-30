package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(MIN) Minimum", display = "Min(#Value1#, #Value2#)", returnType = 6)
@Help("Minimum of two values")
@SortOrder(900)
@IgnoreInBuilder
public class Minimum extends ValueBlock {
   @Parameter
   public IBlock Value1;
   @Parameter
   public IBlock Value2;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return Math.min(this.Value1.evaluateBlock(var1), this.Value2.evaluateBlock(var1));
   }
}
