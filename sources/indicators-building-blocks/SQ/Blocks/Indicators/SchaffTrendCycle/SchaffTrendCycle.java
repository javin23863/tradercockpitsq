package SQ.Blocks.Indicators.SchaffTrendCycle;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
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

@BuildingBlock(name = "(SCHTC) Schaff Trend Cycle", display = "Schaff Trend Cycle(@Chart@#StochPeriod#,#FastPeriod#,#SlowPeriod#)[#Shift#]", returnType = 1)
@Indicator(oscillator = true, middleValue = 50.0, min = 0.0, max = 100.0, step = 0.01)
@Help("Schaff TrendCycle help text")
@ParameterSets(
   {
         @ParameterSet(set = "StochPeriod=12"),
         @ParameterSet(set = "StochPeriod=24"),
         @ParameterSet(set = "StochPeriod=48"),
         @ParameterSet(set = "StochPeriod=96"),
         @ParameterSet(set = "StochPeriod=120"),
         @ParameterSet(set = "StochPeriod=10"),
         @ParameterSet(set = "StochPeriod=20"),
         @ParameterSet(set = "StochPeriod=40"),
         @ParameterSet(set = "StochPeriod=60"),
         @ParameterSet(set = "StochPeriod=100"),
         @ParameterSet(set = "StochPeriod=200")
   }
)
public class SchaffTrendCycle extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int StochPeriod;
   @Parameter(defaultValue = "20", isPeriod = true, minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int FastPeriod;
   @Parameter(defaultValue = "50", isPeriod = true, minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int SlowPeriod;
   @Output
   public DataSeries Value;
   private AverageCalculator fastEmaAvgCalculator;
   private AverageCalculator slowEmaAvgCalculator;
   private HighestCalculator highestCalculator;
   private LowestCalculator lowestCalculator;
   private HighestCalculator highestCalculator2;
   private LowestCalculator lowestCalculator2;
   @Buffer
   public DataSeries Frac1;
   @Buffer
   public DataSeries Frac2;
   @Buffer
   public DataSeries PF;

   protected void OnInit() throws TradingException {
      this.fastEmaAvgCalculator = new AverageCalculator(1, this.FastPeriod);
      this.slowEmaAvgCalculator = new AverageCalculator(1, this.SlowPeriod);
      this.highestCalculator = new HighestCalculator(this.StochPeriod);
      this.lowestCalculator = new LowestCalculator(this.StochPeriod);
      this.highestCalculator2 = new HighestCalculator(this.StochPeriod);
      this.lowestCalculator2 = new LowestCalculator(this.StochPeriod);
   }

   protected void OnBarUpdate() throws TradingException {
      double var1 = 0.5;
      if (this.CurrentBar < this.SlowPeriod) {
         this.Value.set(0, 0.0);
         this.Frac1.set(0, 0.0);
         this.Frac2.set(0, 0.0);
      } else {
         this.fastEmaAvgCalculator.onBarUpdate(this.Input.Close.get(0), this.getCurrentBar());
         this.slowEmaAvgCalculator.onBarUpdate(this.Input.Close.get(0), this.getCurrentBar());
         double var3 = this.fastEmaAvgCalculator.getValue() - this.slowEmaAvgCalculator.getValue();
         this.highestCalculator.onBarUpdate(var3, this.getCurrentBar());
         this.lowestCalculator.onBarUpdate(var3, this.getCurrentBar());
         double var5 = this.lowestCalculator.getLowestValue();
         double var7 = this.highestCalculator.getHighestValue() - var5;
         if (var7 > 0.0) {
            this.Frac1.set(0, (var3 - var5) / var7 * 100.0);
         } else {
            this.Frac1.set(0, this.Frac1.get(1));
         }

         this.PF.set(0, this.PF.get(1) + var1 * (this.Frac1.get(0) - this.PF.get(1)));
         this.lowestCalculator2.onBarUpdate(this.PF.get(0), this.getCurrentBar());
         this.highestCalculator2.onBarUpdate(this.PF.get(0), this.getCurrentBar());
         double var9 = this.lowestCalculator2.getLowestValue();
         double var11 = this.highestCalculator2.getHighestValue() - var9;
         if (var11 > 0.0) {
            this.Frac2.set(0, (this.PF.get(0) - var9) / var11 * 100.0);
         } else {
            this.Frac2.set(0, this.Frac2.get(1));
         }

         this.Value.set(0, this.Value.get(1) + var1 * (this.Frac2.get(0) - this.Value.get(1)));
      }
   }
}
