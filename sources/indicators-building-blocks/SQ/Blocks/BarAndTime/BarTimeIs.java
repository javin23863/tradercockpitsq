package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Bar[#Shift#] time is #Hour#:#Minute#", returnType = 3)
@SortOrder(200)
@IgnoreInBuilder
@ForEngine("*,-SP,-SA")
public class BarTimeIs extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(minValue = 0.0, maxValue = 23.0, defaultValue = "0", step = 1.0)
   public int Hour;
   @Parameter(minValue = 0.0, maxValue = 59.0, defaultValue = "0", step = 1.0)
   public int Minute;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      long var1;
      if (this.Strategy.isStockpicker()) {
         var1 = this.Strategy.Stockpicker.data.TimeD(this.Shift);
      } else {
         var1 = this.Chart.Time(this.Shift);
      }

      return SQTime.getHour(var1) == this.Hour && SQTime.getMinute(var1) == this.Minute;
   }
}
