package SQ.Blocks.StrategyControl;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(ACC) AccountEquity", display = "AccountEquity()", returnType = 1)
@Help("Returns account equity in account currency.")
@SortOrder(1300)
@IgnoreInBuilder
public class AccountEquity extends ValueBlock {
   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.getAccountEquity();
   }
}
