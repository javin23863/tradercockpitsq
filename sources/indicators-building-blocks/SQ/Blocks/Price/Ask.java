package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(ASK) Ask", display = "Ask", returnType = 2)
@OppositeBlock("Bid")
@SortOrder(100)
@CategoryOrder(100)
@ForEngine("*,-SP,-SA")
public class Ask extends ValueBlock {
   @Parameter(defaultValue = "Current")
   public String Symbol;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.MarketData.Chart(this.Symbol).Ask();
   }
}
