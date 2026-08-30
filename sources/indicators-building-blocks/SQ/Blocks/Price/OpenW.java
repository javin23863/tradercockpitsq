package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import SQ.Utils.TimeDiffUtil;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name = "(WO) Weekly Open", returnType = 2, display = "OpenW[@Chart@#Shift#]")
@SortOrder(500)
public class OpenW extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.Strategy.isStockpicker()) {
         return this.Strategy.Stockpicker.data.OpenW(this.chartIndex, var1 + this.Shift);
      } else if (Engines.isTradestationEngine(this.Strategy.getEngine())) {
         int var2 = TimeDiffUtil.getDiffInWeeks(this.Chart, var1);
         return this.Chart.OpenW(this.Shift + var2);
      } else {
         return this.Chart.OpenW(this.Shift + var1);
      }
   }
}
