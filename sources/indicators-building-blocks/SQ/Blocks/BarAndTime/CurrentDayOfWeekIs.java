package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Current day of week is #Day#", returnType = 3)
@SortOrder(900)
public class CurrentDayOfWeekIs extends ConditionBlock {
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Sunday=0,Monday=1,Tuesday=2,Wednesday=3,Thursday=4,Friday=5,Saturday=6")
   public int Day;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      long var1;
      if (this.Strategy.isStockpicker()) {
         var1 = this.Strategy.Stockpicker.TimeCurrent();
      } else {
         var1 = this.Strategy.TimeCurrent();
      }

      return SQTime.getDayOfWeekOriginal(var1) == this.Day;
   }
}
