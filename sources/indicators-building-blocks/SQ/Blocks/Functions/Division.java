package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(/) Division", display = "(#Left# / #Right#)", returnType = 6)
@SortOrder(400)
@IgnoreInBuilder
public class Division extends ValueBlock {
   @Parameter
   public IBlock Left;
   @Parameter
   public IBlock Right;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      double var2 = this.Right.evaluateBlock(var1);
      return var2 != 0.0 ? this.Left.evaluateBlock(var1) / var2 : Double.MAX_VALUE;
   }
}
