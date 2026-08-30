package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Bar[#Shift#] hour < #Hour#", returnType = 3)
@SortOrder(300)
@ForEngine("*,-SP,-SA")
public class BarHourIsSmaller extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(minValue = 0.0, maxValue = 23.0, defaultValue = "0", step = 1.0)
   public int Hour;
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

      return SQTime.getHour(var1) < this.Hour;
   }
}
