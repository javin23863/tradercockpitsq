package SQ.Blocks.Indicators.BullsPower;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(BUP) Bulls Power", display = "BullsPower(@Chart@#Period#, #ComputedFrom#)[#Shift#]", returnType = 1)
@Indicator(oscillator = true, middleValue = 0.0, min = -0.5, max = 0.5, step = 0.01)
@OppositeBlock("BearsPower")
@ParameterSets(
   {
         @ParameterSet(set = "Period=13,ComputedFrom=0"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=15,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0")
   }
)
public class BullsPower extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(minValue = 2.0, maxValue = 10000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Output(name = "Value", color = "#008000")
   public DataSeries Value;
   private AverageCalculator averageCalculator;

   protected void OnInit() throws TradingException {
      this.averageCalculator = new AverageCalculator(1, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.averageCalculator.onBarUpdate(this.Input.getSeries(this.ComputedFrom).get(0), this.getCurrentBar());
      this.Value.set(0, this.Input.High.get(0) - this.averageCalculator.getValue());
   }
}
