package SQ.Blocks.Indicators.MovingAverage;

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

@BuildingBlock(name = "(TEMA) Triple Exponential Moving Average", display = "TEMA(@Chart@#Period#)[#Shift#]", returnType = 2)
@Help("Triple Exponential Moving Average")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0")
   }
)
public class TEMA extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14")
   public int Period;
   @Output(name = "TEMA", color = "#FF0000")
   public DataSeries Value;
   private AverageCalculator ema1Calculator;
   private AverageCalculator ema2Calculator;
   private AverageCalculator ema3Calculator;

   protected void OnInit() throws TradingException {
      this.ema1Calculator = new AverageCalculator(1, this.Period);
      this.ema2Calculator = new AverageCalculator(1, this.Period);
      this.ema3Calculator = new AverageCalculator(1, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.ema1Calculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.ema2Calculator.onBarUpdate(this.ema1Calculator.getValue(), this.getCurrentBar());
      this.ema3Calculator.onBarUpdate(this.ema2Calculator.getValue(), this.getCurrentBar());
      this.Value.set(0, 3.0 * this.ema1Calculator.getValue() - 3.0 * this.ema2Calculator.getValue() + this.ema3Calculator.getValue());
   }
}
