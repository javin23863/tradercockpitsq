package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Always True", display = "Always True", returnType = 3)
@Help("Condition that is always true.")
@SortOrder(5000)
@CategoryOrder(100)
public class AlwaysTrue extends ConditionBlock {
   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      return true;
   }
}
