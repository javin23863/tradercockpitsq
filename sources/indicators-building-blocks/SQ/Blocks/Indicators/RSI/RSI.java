package SQ.Blocks.Indicators.RSI;

import SQ.Calculators.RSICalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(RSI) Relative Strength Index", display = "RSI(@Chart@#Period#)[#Shift#]", returnType = 1)
@Indicator(oscillator = true, middleValue = 50.0, min = 0.0, max = 100.0, step = 0.5)
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
public class RSI extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Output(name = "RSI", color = "#FF0000")
   public DataSeries Value;
   private RSICalculator rsiCalculator;

   protected void OnInit() throws TradingException {
      this.rsiCalculator = new RSICalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.rsiCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.Value.set(0, this.rsiCalculator.getValue());
   }
}
