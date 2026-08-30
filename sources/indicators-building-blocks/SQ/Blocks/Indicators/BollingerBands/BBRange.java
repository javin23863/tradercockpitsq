package SQ.Blocks.Indicators.BollingerBands;

import SQ.Calculators.StdDevCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(BBR) BB Range", display = "BB Range(@Chart@#Period#, #Deviation#)[#Shift#]", returnType = 7)
@Indicator(min = 0.0, max = 5000.0, step = 0.001)
@ParameterSets(
   {
         @ParameterSet(set = "Period=20,Deviation=2,ComputedFrom=0"),
         @ParameterSet(set = "Period=10,Deviation=1.9,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,Deviation=2.1,ComputedFrom=0")
   }
)
public class BBRange extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(minValue = 2.0, maxValue = 10000.0, defaultValue = "20", step = 1.0)
   public int Period;
   @Parameter(defaultValue = "2", minValue = 0.01, maxValue = 10.0, step = 0.01, builderMinValue = 0.1, builderMaxValue = 7.0, builderStep = 0.1)
   public double Deviation;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Output(name = "BB Range", color = "#008000")
   public DataSeries Value;
   private StdDevCalculator stdDevCalculator;

   protected void OnInit() throws TradingException {
      this.stdDevCalculator = new StdDevCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.stdDevCalculator.onBarUpdate(this.Chart.getSeries(this.ComputedFrom).get(this.Shift), this.getCurrentBar());
      this.Value.set(0, 2.0 * this.Deviation * this.stdDevCalculator.getValue());
   }
}
