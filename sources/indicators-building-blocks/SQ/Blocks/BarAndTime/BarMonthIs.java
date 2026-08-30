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

@BuildingBlock(display = "Bar[#Shift#] month = #Month#", returnType = 3)
@OppositeBlock("BarMonthIs")
@SortOrder(500)
public class BarMonthIs extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "1")
   @Editor(type = 40, values = "January=1,February=2,March=3,April=4,May=5,Jun=6,July=7,August=8,September=9,October=10,November=11,December=12")
   public int Month;
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

      return SQTime.getMonthOriginal(var1) == this.Month;
   }
}
