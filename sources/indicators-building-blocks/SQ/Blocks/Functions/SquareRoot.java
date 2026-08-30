package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(SQRT) Sqrt", display = "Sqrt(#Value#)", returnType = 6)
@SortOrder(600)
@IgnoreInBuilder
public class SquareRoot extends ValueBlock {
   @Parameter
   public IBlock Value;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return Math.sqrt(this.Value.evaluateBlock(var1));
   }
}
