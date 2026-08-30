package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(BID) Bid", display = "Bid", returnType = 2)
@OppositeBlock("Ask")
@SortOrder(200)
@ForEngine("*,-SP,-SA")
public class Bid extends ValueBlock {
   @Parameter(defaultValue = "Current")
   public String Symbol;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.MarketData.Chart(this.Symbol).Bid();
   }
}
