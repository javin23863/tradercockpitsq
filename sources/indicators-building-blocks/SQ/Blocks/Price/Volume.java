package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(V) Volume", returnType = 1, display = "Volume[@Chart@#Shift#]")
@IgnoreInBuilder
public class Volume extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.Strategy.isStockpicker()) {
         return this.Strategy.Stockpicker.strategyTriggeredAt() == 5 && var1 + this.Shift == 0
            ? 0.0
            : this.Strategy.Stockpicker.data.Volume(this.chartIndex, var1 + this.Shift);
      } else {
         return this.Chart.Volume(var1 + this.Shift);
      }
   }
}
