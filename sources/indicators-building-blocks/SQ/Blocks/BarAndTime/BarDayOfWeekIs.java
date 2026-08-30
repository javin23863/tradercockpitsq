package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Bar[#Shift#] day of week = #Day#", returnType = 3)
@OppositeBlock("BarDayOfWeekIs")
@SortOrder(500)
public class BarDayOfWeekIs extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Sunday=0,Monday=1,Tuesday=2,Wednesday=3,Thursday=4,Friday=5,Saturday=6")
   public int Day;
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

      return SQTime.getDayOfWeekOriginal(var1) == this.Day;
   }
}
