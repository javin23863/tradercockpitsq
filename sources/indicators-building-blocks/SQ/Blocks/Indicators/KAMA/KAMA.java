package SQ.Blocks.Indicators.KAMA;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.SumCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(
   name = "(KAMA) Kaufman's  Adaptive Moving Average",
   display = "Kaufman's Adaptive Moving Average(@Chart@#ERPeriod#,#ShortPeriod#,#LongPeriod#)[#Shift#]",
   returnType = 2
)
@Help("KAMA is another smoothed MA.")
public class KAMA extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int ERPeriod;
   @Parameter(defaultValue = "2", isPeriod = true, minValue = 2.0, maxValue = 500.0, step = 1.0)
   public int ShortPeriod;
   @Parameter(defaultValue = "30", isPeriod = true, minValue = 2.0, maxValue = 500.0, step = 1.0)
   public int LongPeriod;
   @Output(name = "KAMA", color = "#0000FF")
   public DataSeries Value;
   private SumCalculator volsumcalculator;
   private AverageCalculator fastestExponentialMovingAverage;
   private AverageCalculator slowestExponentialMovingAverage;

   protected void OnInit() throws TradingException {
      this.volsumcalculator = new SumCalculator(this.ERPeriod);
   }

   protected void OnBarUpdate() throws TradingException {
      double var1 = Math.max(Math.max(this.ShortPeriod, this.LongPeriod), this.ERPeriod);
      if (this.CurrentBar < var1) {
         this.Value.set(0, 0.0);
      } else {
         double var3 = 2.0 / (this.ShortPeriod + 1);
         double var5 = 2.0 / (this.LongPeriod + 1);
         double var7 = this.kaufmanEfficiencyRatio(this.Chart, this.ERPeriod, this.volsumcalculator);
         double var9 = Math.pow(var7 * (var3 - var5) + var5, 2.0);
         double var11 = this.Value.get(1) + var9 * (this.Chart.Close.get(0) - this.Value.get(1));
         this.Value.set(0, var11);
      }
   }

   private double kaufmanEfficiencyRatio(ChartData var1, int var2, SumCalculator var3) throws TradingException {
      double var4 = Math.abs(var1.Close.get(0) - var1.Close.get(var2));
      var3.onBarUpdate(Math.abs(var1.Close.get(0) - var1.Close.get(1)), this.getCurrentBar());
      double var6 = var3.getValue();
      return SQUtils.safeDivide(var4, var6);
   }
}
