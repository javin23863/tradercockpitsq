package SQ.Blocks.Indicators.HullMovingAverage;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(HMA) Hull Moving Average ", display = "HMA(@Chart@#Period#)[#Shift#]", returnType = 2)
@Help("HMA is an adaptive MA")
@ParameterSets(
   {
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=240"),
         @ParameterSet(set = "Period=12,ComputedFrom=0"),
         @ParameterSet(set = "Period=24,ComputedFrom=0"),
         @ParameterSet(set = "Period=48,ComputedFrom=0"),
         @ParameterSet(set = "Period=120,ComputedFrom=0"),
         @ParameterSet(set = "Period=240,ComputedFrom=0")
   }
)
public class HullMovingAverage extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Output(name = "HMA", color = "#0000FF")
   public DataSeries Value;
   private AverageCalculator fastwma;
   private AverageCalculator slowwma;
   private AverageCalculator hma;
   private int fastMaPeriod;
   private int squaredPeriod;

   protected void OnInit() throws TradingException {
      this.fastMaPeriod = (int)Math.floor(this.Period / 2);
      this.squaredPeriod = (int)Math.floor(Math.sqrt(this.Period));
      this.fastwma = new AverageCalculator(3, this.fastMaPeriod);
      this.slowwma = new AverageCalculator(3, this.Period);
      this.hma = new AverageCalculator(3, this.squaredPeriod);
   }

   protected void OnBarUpdate() throws TradingException {
      if (this.CurrentBar > this.squaredPeriod) {
         this.fastwma.onBarUpdate(this.Input.get(0), this.getCurrentBar());
         this.slowwma.onBarUpdate(this.Input.get(0), this.getCurrentBar());
         double var3 = this.fastwma.getValue();
         double var5 = this.slowwma.getValue();
         double var1 = 2.0 * var3 - var5;
         this.hma.onBarUpdate(var1, this.getCurrentBar());
         this.Value.set(0, this.hma.getValue());
      }
   }
}
