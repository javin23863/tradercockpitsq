package SQ.Blocks.Indicators.BarRange;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(BR) BarRange", display = "BarRange(@Chart@)[#Shift#]", returnType = 7)
@Indicator(min = 0.0, max = 5000.0, step = 0.001)
public class BarRange extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Output(name = "BarRange", color = "#FF0000")
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      this.Value.set(0, this.Chart.High(0) - this.Chart.Low(0));
   }
}
