package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(H) High", returnType = 2, display = "High[@Chart@#Shift#]")
@SortOrder(600)
@OppositeBlock("Low")
public class High extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.Strategy.isStockpicker()) {
         return this.Strategy.Stockpicker.strategyTriggeredAt() == 5 && var1 + this.Shift == 0
            ? this.Strategy.Stockpicker.data.Open(this.chartIndex, var1 + this.Shift)
            : this.Strategy.Stockpicker.data.High(this.chartIndex, var1 + this.Shift);
      } else {
         return this.Chart.High(var1 + this.Shift);
      }
   }
}
