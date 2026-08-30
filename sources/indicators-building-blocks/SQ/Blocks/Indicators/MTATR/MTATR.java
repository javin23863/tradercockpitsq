package SQ.Blocks.Indicators.MTATR;

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

@BuildingBlock(name = "(MTATR) Average True Range", display = "MTATR(@Chart@#Period#)[#Shift#]", returnType = 7)
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
public class MTATR extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(category = "Default", name = "Period", minValue = 2.0, maxValue = 10000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Output(name = "ATR", color = "#008000")
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      if (this.getCurrentBar() == 0) {
         this.Value.set(0, this.Chart.High.get(0) - this.Chart.Low.get(0));
      } else {
         double var1 = 0.0;
         int var3 = Math.min(this.Period, this.CurrentBar);

         for (int var4 = 0; var4 < var3; var4++) {
            double var5 = this.Chart.High.get(var4);
            double var7 = this.Chart.Low.get(var4);
            double var9 = this.Chart.Close.get(var4 + 1);
            double var11 = var5 - var7;
            double var13 = Math.abs(var7 - var9);
            double var15 = Math.abs(var5 - var9);
            double var17 = Math.max(var11, Math.max(var13, var15));
            var1 += var17;
         }

         double var19 = var1 / var3;
         this.Value.set(0, var19);
      }
   }
}
