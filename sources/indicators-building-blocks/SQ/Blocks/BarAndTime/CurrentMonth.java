package SQ.Blocks.BarAndTime;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(returnType = 1)
@Help("Current month (in broker time). 1=January,..., 12=December")
@SortOrder(900)
public class CurrentMonth extends ValueBlock {
   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      long var2;
      if (this.Strategy.isStockpicker()) {
         var2 = this.Strategy.Stockpicker.TimeCurrent();
      } else {
         var2 = this.Strategy.TimeCurrent();
      }

      return SQTime.getMonthOriginal(var2);
   }
}
