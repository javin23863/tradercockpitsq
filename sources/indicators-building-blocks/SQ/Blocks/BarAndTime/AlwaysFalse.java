package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Always False", display = "Always False", returnType = 3)
@Help("Condition that is always false.")
@SortOrder(5000)
@CategoryOrder(100)
public class AlwaysFalse extends ConditionBlock {
   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      return false;
   }
}
