package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(*) Multiplication", display = "(#Left# * #Right#)", returnType = 6)
@SortOrder(300)
@IgnoreInBuilder
public class Multiplication extends ValueBlock {
   @Parameter
   public IBlock Left;
   @Parameter
   public IBlock Right;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Left.evaluateBlock(var1) * this.Right.evaluateBlock(var1);
   }
}
