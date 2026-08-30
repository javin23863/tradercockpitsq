package SQ.Blocks.BarAndTime;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(returnType = 1)
@Help("Current day of week (in broker time). Sunday=0,Monday=1,Tuesday=2,Wednesday=3,Thursday=4,Friday=5,Saturday=6")
@SortOrder(900)
public class CurrentDayOfWeek extends ValueBlock {
   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      long var2;
      if (this.Strategy.isStockpicker()) {
         var2 = this.Strategy.Stockpicker.TimeCurrent();
      } else {
         var2 = this.Strategy.TimeCurrent();
      }

      return SQTime.getDayOfWeekOriginal(var2);
   }
}
