package SQ.Blocks.Indicators.BollingerBands;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.StdDevCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(BB) Bollinger Bands", display = "BollingerBands(@Chart@#Period#, #Deviation#).#Line#[#Shift#]", returnType = 2)
@ParameterSets(
   {
         @ParameterSet(set = "Period=20,Deviation=2,ComputedFrom=0"),
         @ParameterSet(set = "Period=10,Deviation=1.9,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,Deviation=2.1,ComputedFrom=0")
   }
)
public class BollingerBands extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(minValue = 2.0, maxValue = 10000.0, defaultValue = "20", step = 1.0)
   public int Period;
   @Parameter(defaultValue = "2", minValue = 0.01, maxValue = 10.0, step = 0.01, builderMinValue = 0.1, builderMaxValue = 7.0, builderStep = 0.1)
   public double Deviation;
   @Output(name = "Upper", color = "#008000")
   public DataSeries Upper;
   @Output(name = "Lower", color = "#FF0000")
   public DataSeries Lower;
   private AverageCalculator averageCalculator;
   private StdDevCalculator stdDevCalculator;

   protected void OnInit() throws TradingException {
      this.averageCalculator = new AverageCalculator(0, this.Period);
      this.stdDevCalculator = new StdDevCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.averageCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.stdDevCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      double var1 = this.averageCalculator.getValue();
      double var3 = this.stdDevCalculator.getValue();
      this.Upper.set(0, var1 + this.Deviation * var3);
      this.Lower.set(0, var1 - this.Deviation * var3);
   }
}
