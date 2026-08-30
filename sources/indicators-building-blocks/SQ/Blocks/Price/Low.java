package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(L) Low", returnType = 2, display = "Low[@Chart@#Shift#]")
@SortOrder(700)
@OppositeBlock("High")
public class Low extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.Strategy.isStockpicker()) {
         return this.Strategy.Stockpicker.strategyTriggeredAt() == 5 && var1 + this.Shift == 0
            ? this.Strategy.Stockpicker.data.Open(this.chartIndex, var1 + this.Shift)
            : this.Strategy.Stockpicker.data.Low(this.chartIndex, var1 + this.Shift);
      } else {
         return this.Chart.Low(var1 + this.Shift);
      }
   }
}
