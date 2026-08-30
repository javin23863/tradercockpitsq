package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import SQ.Utils.TimeDiffUtil;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name = "(DO) Daily Open", returnType = 2, display = "OpenD[@Chart@#Shift#]")
@SortOrder(500)
public class OpenD extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.Strategy.isStockpicker()) {
         return this.Strategy.Stockpicker.data.OpenD(this.chartIndex, var1 + this.Shift);
      } else if (Engines.isTradestationEngine(this.Strategy.getEngine())) {
         int var2 = TimeDiffUtil.getDiffInDays(this.Chart, var1, true);
         return this.Chart.OpenD(this.Shift + var2);
      } else {
         return this.Chart.OpenD(this.Shift + var1);
      }
   }
}
