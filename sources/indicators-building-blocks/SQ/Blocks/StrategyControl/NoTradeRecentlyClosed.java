package SQ.Blocks.StrategyControl;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(display = "NoTradeRecentlyClosed(#Symbol#, #MagicNumber#)", returnType = 3)
@Help(
   "Returns true if there wasn't active trade that was closed recently - which means at the same bar or even the same minute. This block can be used to filter too frequent trading and improve backtesting accuracy."
)
@SortOrder(400)
@IgnoreInBuilder
public class NoTradeRecentlyClosed extends TradeRecentlyClosed {
   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      return Engines.isTradestationEngine(this.Strategy.getEngine()) ? true : !this.checkTradeClosedRecently();
   }
}
