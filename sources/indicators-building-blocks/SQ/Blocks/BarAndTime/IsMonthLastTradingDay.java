package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Is month last trading day", display = "IsMonthLastTradingDay", returnType = 3)
@SortOrder(100)
@NoShift
public class IsMonthLastTradingDay extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "false", showIfDefault = false)
   public boolean IncludeWeekends;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      long var1;
      if (this.Strategy.isStockpicker()) {
         var1 = this.Strategy.Stockpicker.TimeCurrent();
      } else {
         var1 = this.Chart.Time();
      }

      int var3 = SQTime.getDaysInMonth(var1);
      long var4 = SQTime.setDayOfMonth(var1, var3);
      if (!this.IncludeWeekends) {
         if (SQTime.getDayOfWeek(var4) == 6) {
            var4 = SQTime.addDays(var4, -1);
         } else if (SQTime.getDayOfWeek(var4) == 7) {
            var4 = SQTime.addDays(var4, -2);
         }
      }

      return SQTime.getDay(var1) == SQTime.getDay(var4);
   }
}
