package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Is month first trading day", display = "IsMonthFirstTradingDay", returnType = 3)
@SortOrder(100)
@NoShift
public class IsMonthFirstTradingDay extends ConditionBlock {
   private long monthFirstTradingDay = -1L;
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

      long var3 = SQTime.getDateInMs(var1);
      if (this.monthFirstTradingDay == -1L) {
         this.monthFirstTradingDay = var3;
      }

      if (SQTime.getMonth(this.monthFirstTradingDay) != SQTime.getMonth(var3)) {
         if (!this.IncludeWeekends) {
            if (SQTime.getDayOfWeek(var3) != 6 && SQTime.getDayOfWeek(var3) != 7) {
               this.monthFirstTradingDay = var3;
            }
         } else {
            this.monthFirstTradingDay = var3;
         }
      }

      return this.monthFirstTradingDay == var3;
   }
}
