package SQ.Blocks.Indicators.StdDev;

import SQ.Calculators.StdDevCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(STDDEV) Standard Deviation", display = "StdDev(@Chart@#Period#)[#Shift#]", returnType = 1)
@Help("Simple Moving Average")
@Indicator(min = 0.0, max = 1.0, step = 0.01)
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
public class StdDev extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(category = "Default", name = "Period", minValue = 2.0, maxValue = 10000.0, defaultValue = "20", step = 1.0)
   public int Period;
   @Output
   public DataSeries Value;
   private StdDevCalculator stdDevCalculator;

   protected void OnInit() throws TradingException {
      this.stdDevCalculator = new StdDevCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.stdDevCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.Value.set(0, this.stdDevCalculator.getValue());
   }
}
