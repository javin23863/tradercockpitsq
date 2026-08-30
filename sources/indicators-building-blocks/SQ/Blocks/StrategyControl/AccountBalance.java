package SQ.Blocks.StrategyControl;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(ACB) AccountBalance", display = "AccountBalance()", returnType = 1)
@Help("Returns account balance in account currency.")
@SortOrder(1400)
@IgnoreInBuilder
@ForEngine("*,-MC,-TS,-SP,-SA")
public class AccountBalance extends ValueBlock {
   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.getAccountBalance();
   }
}
