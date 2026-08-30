package SQ.Blocks.BarAndTime;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(returnType = 1, display = "CurrentBar")
@NoShift
@SortOrder(200)
@IgnoreInBuilder
public class CurrentBar extends ValueBlock {
   @Parameter
   public ChartData Chart;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.isStockpicker() ? this.Strategy.Stockpicker.getCurrentBar(0) : this.Chart.getCurrentBar();
   }
}
