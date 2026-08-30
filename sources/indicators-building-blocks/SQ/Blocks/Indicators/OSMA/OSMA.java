package SQ.Blocks.Indicators.OSMA;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(OSMA) Moving Average Of Oscillator", display = "OSMA(@Chart@#FastEMA#, #SlowEMA#, #SignalPeriod#)[#Shift#]", returnType = 1)
@Indicator(oscillator = true, middleValue = 0.0, min = -0.3, max = 0.3, step = 0.001)
@ParameterSets(
   {
         @ParameterSet(set = "FastEMA=12,SlowEMA=26,SignalPeriod=9,ComputedFrom=0"),
         @ParameterSet(set = "FastEMA=24,SlowEMA=52,SignalPeriod=9,ComputedFrom=0"),
         @ParameterSet(set = "FastEMA=8,SlowEMA=17,SignalPeriod=9,ComputedFrom=0"),
         @ParameterSet(set = "FastEMA=3,SlowEMA=10,SignalPeriod=16,ComputedFrom=0")
   }
)
public class OSMA extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(name = "Fast EMA", defaultValue = "12", isPeriod = true)
   public int FastEMA;
   @Parameter(name = "Slow EMA", defaultValue = "26", isPeriod = true)
   public int SlowEMA;
   @Parameter(defaultValue = "9", isPeriod = true)
   public int SignalPeriod;
   @Output(name = "OSMA", color = "#FF0000")
   public DataSeries Value;
   private AverageCalculator fastEMACalculator;
   private AverageCalculator slowEMACalculator;
   private AverageCalculator signalCalculator;

   protected void OnInit() throws TradingException {
      this.fastEMACalculator = new AverageCalculator(1, this.FastEMA);
      this.slowEMACalculator = new AverageCalculator(1, this.SlowEMA);
      this.signalCalculator = new AverageCalculator(0, this.SignalPeriod);
   }

   protected void OnBarUpdate() throws TradingException {
      this.fastEMACalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.slowEMACalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      double var1 = this.fastEMACalculator.getValue() - this.slowEMACalculator.getValue();
      this.signalCalculator.onBarUpdate(var1, this.getCurrentBar());
      this.Value.set(0, var1 - this.signalCalculator.getValue());
   }
}
