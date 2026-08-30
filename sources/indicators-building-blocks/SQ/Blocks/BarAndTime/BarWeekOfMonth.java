package SQ.Blocks.BarAndTime;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(returnType = 1, display = "WeekOfMonth[#Shift#]")
@Help("Returns week of the month (1-6)")
@SortOrder(400)
@ForEngine("SP,SA,MT4,MT5,TS,MC")
public class BarWeekOfMonth extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      long var2;
      if (this.Strategy.isStockpicker()) {
         var2 = this.Strategy.Stockpicker.data.TimeD(var1 + this.Shift);
      } else {
         var2 = this.Chart.Time(var1 + this.Shift);
      }

      return SQTime.getWeekOfMonth(var2);
   }
}
