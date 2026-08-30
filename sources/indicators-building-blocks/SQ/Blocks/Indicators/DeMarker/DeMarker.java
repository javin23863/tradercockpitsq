package SQ.Blocks.Indicators.DeMarker;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(DE) DeMarker", display = "DeMarker(@Chart@#Period#)[#Shift#]", returnType = 1)
@Help("DeMarker")
@Indicator(oscillator = true, middleValue = 0.5, min = 0.0, max = 1.0, step = 0.01)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50")
   }
)
public class DeMarker extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Output(name = "DeMarker", color = "#FF0000")
   public DataSeries Value;
   @Buffer
   public DataSeries DeMin;
   @Buffer
   public DataSeries DeMax;
   private AverageCalculator DeMinAverageCalculator;
   private AverageCalculator DeMaxAverageCalculator;

   protected void OnInit() throws TradingException {
      this.DeMinAverageCalculator = new AverageCalculator(0, this.Period);
      this.DeMaxAverageCalculator = new AverageCalculator(0, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      if (this.getCurrentBar() == 0) {
         this.Value.set(0, 0.0);
         this.DeMinAverageCalculator.onBarUpdate(0.0, this.getCurrentBar());
         this.DeMaxAverageCalculator.onBarUpdate(0.0, this.getCurrentBar());
      } else {
         this.DeMaxAverageCalculator.onBarUpdate(Math.max(this.Chart.High(0) - this.Chart.High(1), 0.0), this.getCurrentBar());
         this.DeMinAverageCalculator.onBarUpdate(Math.max(this.Chart.Low(1) - this.Chart.Low(0), 0.0), this.getCurrentBar());
         double var1 = this.DeMinAverageCalculator.getValue();
         double var3 = this.DeMaxAverageCalculator.getValue();
         double var5 = var1 + var3;
         if (var5 != 0.0) {
            this.Value.set(0, var3 / var5);
         } else {
            this.Value.set(0, 0.0);
         }
      }
   }
}
