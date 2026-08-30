package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Spread in pips", returnType = 1)
@SortOrder(400)
@IgnoreInBuilder
@ForEngine("*,-SP,-SA")
public class SpreadInPips extends ValueBlock {
   @Parameter(defaultValue = "Current")
   public String Symbol;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy
         .convertRealPriceToPips(this.Symbol, Math.abs(this.Strategy.MarketData.Chart(this.Symbol).Ask() - this.Strategy.MarketData.Chart(this.Symbol).Bid()));
   }
}
