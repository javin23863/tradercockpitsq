package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Current hour is #Hour#", returnType = 3)
@SortOrder(700)
@ForEngine("*,-SP,-SA")
public class CurrentHourIs extends ConditionBlock {
   @Parameter(minValue = 0.0, maxValue = 23.0, defaultValue = "0", step = 1.0)
   public int Hour;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      long var1;
      if (this.Strategy.isStockpicker()) {
         var1 = this.Strategy.Stockpicker.TimeCurrent();
      } else {
         var1 = this.Strategy.TimeCurrent();
      }

      return SQTime.getHour(var1) == this.Hour;
   }
}
