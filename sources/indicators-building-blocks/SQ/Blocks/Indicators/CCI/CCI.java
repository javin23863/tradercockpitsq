package SQ.Blocks.Indicators.CCI;

import SQ.Calculators.AverageCalculator;
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

@BuildingBlock(name = "(CCI) Commodity Channel Index", display = "CCI(@Chart@#Period#)[#Shift#]", returnType = 1)
@Help("Commodity channel index")
@Indicator(oscillator = true, middleValue = 0.0, min = -500.0, max = 500.0, step = 5.0)
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
public class CCI extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Output(name = "CCI", color = "#FF0000")
   public DataSeries Value;
   private AverageCalculator averageCalculator;

   protected void OnInit() throws TradingException {
      this.averageCalculator = new AverageCalculator(0, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.averageCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      if (this.getCurrentBar() == 0) {
         this.Value.set(0, 0.0);
      } else {
         double var1 = 0.0;
         double var3 = this.averageCalculator.getValue();

         for (int var5 = Math.min(this.CurrentBar, this.Period - 1); var5 >= 0; var5--) {
            var1 += Math.abs(this.Input.get(var5) - var3);
         }

         if (var1 < 1.0E-10) {
            this.Value.set(0, 0.0);
         } else {
            double var7 = (this.Input.get(0) - var3) / (var1 == 0.0 ? 1.0 : 0.015 * (var1 / Math.min(this.Period, this.CurrentBar + 1)));
            this.Value.set(0, var7);
         }
      }
   }
}
