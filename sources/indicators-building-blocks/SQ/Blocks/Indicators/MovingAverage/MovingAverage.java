package SQ.Blocks.Indicators.MovingAverage;

import SQ.Calculators.AverageCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(MA) Moving Average", display = "MA(@Chart@#Period#, #MAMethod#)[#Shift#]", returnType = 2)
@IgnoreInBuilder
public class MovingAverage extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14")
   public int Period;
   @Parameter(name = "Method", defaultValue = "0")
   @Editor(type = 40, values = "Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
   public int MAMethod;
   @Output(name = "MA", color = "#FF0000")
   public DataSeries Value;
   private AverageCalculator averageCalculator;

   protected void OnInit() throws TradingException {
      this.MAMethod = SQUtils.fixAllowedRange(this.MAMethod, 0, 3, 0);
      this.averageCalculator = new AverageCalculator(this.MAMethod, this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.averageCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.Value.set(0, this.averageCalculator.getValue());
   }
}
