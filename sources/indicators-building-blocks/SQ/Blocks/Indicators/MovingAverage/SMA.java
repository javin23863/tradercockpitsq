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

@BuildingBlock(name = "(SMA) Simple Moving Average", display = "SMA(@Chart@#Period#)[#Shift#]", returnType = 2)
@Help("Simple Moving Average")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class SMA extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14")
   public int Period;
   @Output(name = "SMA", color = "#FF0000")
   public DataSeries Value;
   private AverageCalculator averageCalculator;

   protected void OnInit() throws TradingException {
      this.averageCalculator = new AverageCalculator(0, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.Calls++;
      this.averageCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.Value.set(0, this.averageCalculator.getValue());
   }
}
