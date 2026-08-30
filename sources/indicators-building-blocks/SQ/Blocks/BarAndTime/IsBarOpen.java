package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Is Bar Open", display = "IsBarOpen", returnType = 3)
@Help(
   "Is triggered on bar open. This happens only once during the bar, so you can use this condition for strategies that should open the trade only on bar opening."
)
@SortOrder(100)
@CategoryOrder(100)
@IgnoreInBuilder
public class IsBarOpen extends ConditionBlock {
   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      return this.Strategy.UpdateEventType == 2;
   }
}
