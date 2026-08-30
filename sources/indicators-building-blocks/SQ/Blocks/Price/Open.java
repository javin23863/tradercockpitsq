package SQ.Blocks.Price;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(O) Open", returnType = 2, display = "Open[@Chart@#Shift#]")
@SortOrder(500)
public class Open extends ValueBlock {
   @Parameter
   public ChartData Chart;
   @Parameter
   public int Shift;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      return this.Strategy.isStockpicker() ? this.Strategy.Stockpicker.data.Open(this.chartIndex, var1 + this.Shift) : this.Chart.Open(var1 + this.Shift);
   }
}
