package SQ.Blocks.Indicators.KaufmanEfficiencyRatio;

import SQ.Calculators.SumCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(KER) Kaufman Efficiency Ratio", display = "Kaufman Efficiency Ratio(@Chart@#Period#)[#Shift#]", returnType = 1)
@Help("Kaufman Efficiency Ratio")
@Indicator(oscillator = true, middleValue = 0.5, min = 0.0, max = 1.0, step = 0.01)
@ParameterSets(
   {
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=120")
   }
)
public class KaufmanEfficiencyRatio extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Output(name = "KFE", color = "#0000FF")
   public DataSeries Value;
   private SumCalculator volsumcalculator;

   protected void OnInit() throws TradingException {
      this.volsumcalculator = new SumCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      if (this.CurrentBar < this.Period) {
         this.Value.set(0, 0.0);
      } else {
         double var1 = Math.abs(this.Chart.Close.get(0) - this.Chart.Close.get(this.Period));
         this.volsumcalculator.onBarUpdate(Math.abs(this.Chart.Close.get(0) - this.Chart.Close.get(1)), this.getCurrentBar());
         double var3 = this.volsumcalculator.getValue();
         double var5 = var1 / var3;
         this.Value.set(0, var5);
      }
   }
}
