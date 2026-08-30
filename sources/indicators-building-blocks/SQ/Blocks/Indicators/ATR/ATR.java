package SQ.Blocks.Indicators.ATR;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(ATR) Average True Range", display = "ATR(@Chart@#Period#)[#Shift#]", returnType = 7)
@Indicator(min = 0.0, max = 5000.0, step = 0.001)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50")
   }
)
public class ATR extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(category = "Default", name = "Period", minValue = 2.0, maxValue = 10000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Output(name = "ATR", color = "#008000")
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      double var1 = this.Chart.High.get(0);
      double var3 = this.Chart.Low.get(0);
      double var5 = var1 - var3;
      if (this.getCurrentBar() == 0) {
         this.Value.set(0, var5);
      } else {
         double var7 = this.Chart.Close.get(1);
         var5 = Math.max(Math.abs(var3 - var7), Math.max(var5, Math.abs(var1 - var7)));
         this.Value.set(0, ((Math.min(this.CurrentBar + 1, this.Period) - 1) * this.Value.get(1) + var5) / Math.min(this.CurrentBar + 1, this.Period));
      }
   }
}
