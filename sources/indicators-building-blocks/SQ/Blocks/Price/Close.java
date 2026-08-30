package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(C) Close", returnType = 2, display = "Close[@Chart@#Shift#]")
@SortOrder(800)
public class Close extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.Strategy.isStockpicker()) {
         return this.Strategy.Stockpicker.strategyTriggeredAt() == 5 && var1 + this.Shift == 0
            ? this.Strategy.Stockpicker.data.Open(this.chartIndex, var1 + this.Shift)
            : this.Strategy.Stockpicker.data.Close(this.chartIndex, var1 + this.Shift);
      } else {
         return this.Chart.Close(var1 + this.Shift);
      }
   }
}
