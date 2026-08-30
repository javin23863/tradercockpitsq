package SQ.Blocks.Indicators.MACD;

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

@BuildingBlock(name = "(MACD) MACD", display = "MACD(@Chart@#Fast#, #Slow#, #Smooth#).#Line#[#Shift#]")
@Indicator(oscillator = true, middleValue = 0.0, min = -5.0, max = 5.0, step = 0.001)
@ParameterSets(
   {
         @ParameterSet(set = "Fast=12,Slow=26,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=24,Slow=52,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=8,Slow=17,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=3,Slow=10,Smooth=16,ComputedFrom=0")
   }
)
public class MACD extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "12", isPeriod = true)
   public int Fast;
   @Parameter(defaultValue = "26", isPeriod = true)
   public int Slow;
   @Parameter(defaultValue = "9", isPeriod = true)
   public int Smooth;
   @Output(name = "Main", color = "#FF0000")
   public DataSeries Main;
   @Output(name = "Signal", color = "#008000")
   public DataSeries Signal;
   private AverageCalculator fastEMACalculator;
   private AverageCalculator slowEMACalculator;
   private AverageCalculator signalCalculator;

   protected void OnInit() throws TradingException {
      this.fastEMACalculator = new AverageCalculator(1, this.Fast);
      this.slowEMACalculator = new AverageCalculator(1, this.Slow);
      this.signalCalculator = new AverageCalculator(0, this.Smooth);
   }

   protected void OnBarUpdate() throws TradingException {
      this.fastEMACalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.slowEMACalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      double var1 = this.fastEMACalculator.getValue();
      double var3 = this.slowEMACalculator.getValue();
      double var5 = var1 - var3;
      this.signalCalculator.onBarUpdate(var5, this.getCurrentBar());
      this.Main.set(0, var5);
      double var7 = this.signalCalculator.getValue();
      this.Signal.set(0, var7);
   }
}
