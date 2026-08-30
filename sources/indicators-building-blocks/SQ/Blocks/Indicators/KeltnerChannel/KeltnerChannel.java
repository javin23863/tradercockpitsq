package SQ.Blocks.Indicators.KeltnerChannel;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(KC) Keltner Channel", display = "Keltner Channel(@Chart@#Period#, #Deviation#).#Line#[#Shift#]", returnType = 2)
@ParameterSets(
   {
         @ParameterSet(set = "Period=20,Deviation=1.5,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,Deviation=2,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,Deviation=2.25,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,Deviation=2.5,ComputedFrom=0")
   }
)
public class KeltnerChannel extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(name = "Period", defaultValue = "20", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "1.5", minValue = 0.01, maxValue = 10.0, step = 0.01, builderMinValue = 0.1, builderMaxValue = 7.0, builderStep = 0.1)
   public double Deviation;
   @Output
   public DataSeries Upper;
   @Output
   public DataSeries Lower;
   @Buffer
   public DataSeries diff;
   @Buffer
   public DataSeries typical;
   private AverageCalculator middleAverageCalculator;
   private AverageCalculator offsetAverageCalculator;

   protected void OnInit() throws TradingException {
      this.middleAverageCalculator = new AverageCalculator(0, this.Period);
      this.offsetAverageCalculator = new AverageCalculator(0, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.middleAverageCalculator.onBarUpdate(this.Input.Typical.get(0), this.getCurrentBar());
      this.offsetAverageCalculator.onBarUpdate(this.Input.High.get(0) - this.Input.Low.get(0), this.getCurrentBar());
      double var1 = this.middleAverageCalculator.getValue();
      double var3 = this.offsetAverageCalculator.getValue() * this.Deviation;
      this.Upper.set(0, var1 + var3);
      this.Lower.set(0, var1 - var3);
   }
}
