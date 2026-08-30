package SQ.Blocks.Indicators.AwesomeOscillator;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(AWO) Awesome Oscillator", display = "AwesomeOscillator(@Chart@)[#Shift#]", returnType = 1)
@Indicator(oscillator = true, middleValue = 0.0, min = -5.0, max = 5.0, step = 0.01)
public class AwesomeOscillator extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Output(name = "AWO", color = "#FF0000")
   public DataSeries Value;
   private static final int PERIOD_FAST = 5;
   private static final int PERIOD_SLOW = 34;
   private AverageCalculator fastAverageCalculator;
   private AverageCalculator slowAverageCalculator;

   protected void OnInit() throws TradingException {
      this.fastAverageCalculator = new AverageCalculator(0, 5);
      this.slowAverageCalculator = new AverageCalculator(0, 34);
   }

   protected void OnBarUpdate() throws TradingException {
      if (this.getCurrentBar() != 0) {
         this.fastAverageCalculator.onBarUpdate(this.Input.Median.get(0), this.getCurrentBar());
         this.slowAverageCalculator.onBarUpdate(this.Input.Median.get(0), this.getCurrentBar());
         double var1 = this.fastAverageCalculator.getValue();
         double var3 = this.slowAverageCalculator.getValue();
         this.Value.set(0, var1 - var3);
      }
   }
}
