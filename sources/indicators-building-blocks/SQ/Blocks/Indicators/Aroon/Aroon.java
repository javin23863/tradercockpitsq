package SQ.Blocks.Indicators.Aroon;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(ARO) Aroon", display = "Aroon(@Chart@#Period#).#Line#[#Shift#]")
@Indicator(oscillator = true, middleValue = 50.0, min = 0.0, max = 100.0, step = 5.0)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50")
   }
)
public class Aroon extends IndicatorBlock {
   @Parameter(category = "Default", name = "Input", defaultValue = "0")
   public ChartData Chart;
   @Parameter(category = "Default", name = "Period", minValue = 0.0, maxValue = 1000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Output(name = "Up", color = "#008000")
   public DataSeries Up;
   @Output(name = "Down", color = "#FF0000")
   public DataSeries Down;
   private HighestCalculator highestCalculator;
   private LowestCalculator lowestCalculator;

   protected void OnInit() throws TradingException {
      this.highestCalculator = new HighestCalculator(this.Period);
      this.lowestCalculator = new LowestCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.highestCalculator.onBarUpdate(this.Chart.High.get(0), this.getCurrentBar());
      this.lowestCalculator.onBarUpdate(this.Chart.Low.get(0), this.getCurrentBar());
      int var1 = this.highestCalculator.getHighestIndex();
      int var2 = this.lowestCalculator.getLowestIndex();
      this.Up.set(0, 100.0 - 100.0 / this.Period * var1);
      this.Down.set(0, 100.0 - 100.0 / this.Period * var2);
   }
}
