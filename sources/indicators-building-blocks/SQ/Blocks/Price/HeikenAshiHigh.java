package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(HA H) Heiken Ashi High", returnType = 2, display = "Heiken Ashi High[@Chart@#Shift#]")
@SortOrder(2200)
@OppositeBlock("HeikenAshiLow")
@ForEngine("*,-SP,-SA")
public class HeikenAshiHigh extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return Math.max(
         this.Chart.High(var1 + this.Shift),
         Math.max(
            this.Strategy.Indicators.HeikenAshi(this.Chart).HAOpen.getRounded(var1 + this.Shift),
            this.Strategy.Indicators.HeikenAshi(this.Chart).HAClose.getRounded(var1 + this.Shift)
         )
      );
   }
}
