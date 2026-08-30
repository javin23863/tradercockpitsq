package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(HA L) Heiken Ashi Low", returnType = 2, display = "Heiken Ashi Low[@Chart@#Shift#]")
@SortOrder(2300)
@OppositeBlock("HeikenAshiHigh")
@ForEngine("*,-SP,-SA")
public class HeikenAshiLow extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return Math.min(
         this.Chart.Low(var1 + this.Shift),
         Math.min(
            this.Strategy.Indicators.HeikenAshi(this.Chart).HAOpen.getRounded(var1 + this.Shift),
            this.Strategy.Indicators.HeikenAshi(this.Chart).HAClose.getRounded(var1 + this.Shift)
         )
      );
   }
}
