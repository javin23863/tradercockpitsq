package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Last week of month", display = "LastWeekOfMonth", returnType = 3)
@SortOrder(100)
@NoShift
@ForEngine("SP,SA")
public class LastWeekOfMonth extends ConditionBlock {
   @Parameter
   public ChartData Chart;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      long var1;
      if (this.Strategy.isStockpicker()) {
         var1 = this.Strategy.Stockpicker.TimeCurrent();
      } else {
         var1 = this.Chart.Time();
      }

      long var3 = SQTime.setDayOfMonth(var1, SQTime.getDaysInMonth(var1));
      return SQTime.getWeekOfMonth(var1) == SQTime.getWeekOfMonth(var3);
   }
}
