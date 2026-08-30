package SQ.Blocks.Indicators.Momentum;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(MO) Momentum", display = "Momentum(@Chart@#Period#)[#Shift#]", returnType = 1)
@Indicator(oscillator = true, middleValue = 100.0, min = 96.0, max = 104.0, step = 0.1)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class Momentum extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public DataSeries Input;
   @Parameter(defaultValue = "14")
   public int Period;
   @Output(name = "Momentum", color = "#FF0000")
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      if (this.getCurrentBar() == 0) {
         this.Value.set(0, 0.0);
      } else {
         this.Value.set(0, this.Input.get(0) * 100.0 / this.Input.get(Math.min(this.Period, this.CurrentBar)));
      }
   }
}
