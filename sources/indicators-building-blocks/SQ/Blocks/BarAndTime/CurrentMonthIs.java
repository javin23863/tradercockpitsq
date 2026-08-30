package SQ.Blocks.BarAndTime;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(display = "Current month is #Month#", returnType = 3)
@SortOrder(900)
public class CurrentMonthIs extends ConditionBlock {
   @Parameter(defaultValue = "1")
   @Editor(type = 40, values = "January=1,February=2,March=3,April=4,May=5,Jun=6,July=7,August=8,September=9,October=10,November=11,December=12")
   public int Month;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      long var1;
      if (this.Strategy.isStockpicker()) {
         var1 = this.Strategy.Stockpicker.TimeCurrent();
      } else {
         var1 = this.Strategy.TimeCurrent();
      }

      return SQTime.getMonthOriginal(var1) == this.Month;
   }
}
