package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(RND) Round", display = "Round(#Value#, #Decimals#)", returnType = 6)
@Help("Rounds value to given decimal places")
@SortOrder(1000)
@IgnoreInBuilder
public class Round extends ValueBlock {
   @Parameter
   public IBlock Value;
   @Parameter(defaultValue = "5", minValue = 0.0, maxValue = 8.0, step = 1.0)
   public int Decimals;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return SQUtils.round(this.Value.evaluateBlock(var1), this.Decimals);
   }
}
