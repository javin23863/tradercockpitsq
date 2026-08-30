package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(HA C) Heiken Ashi Close", returnType = 2, display = "Heiken Ashi Close[@Chart@#Shift#]")
@SortOrder(2400)
@ForEngine("*,-SP,-SA")
public class HeikenAshiClose extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.Indicators.HeikenAshi(this.Chart).HAClose.getRounded(var1 + this.Shift);
   }
}
