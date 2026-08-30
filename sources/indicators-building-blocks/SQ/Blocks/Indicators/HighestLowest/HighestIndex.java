package SQ.Blocks.Indicators.HighestLowest;

import SQ.Calculators.HighestCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(HI) Highest index", display = "HighestIndex(@Chart@#Period#)[#Shift#]", returnType = 1, mainIndicator = "HighestIndex")
@Help("returns shift of the bar with highest value from the bars of given type")
@OppositeBlock("HighestIndex")
@IgnoreInBuilder
public class HighestIndex extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(minValue = 0.0, maxValue = 10000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Output
   public DataSeries Value;
   private HighestCalculator highestCalculator;

   protected void OnInit() throws TradingException {
      this.highestCalculator = new HighestCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.highestCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.Value.set(0, this.highestCalculator.getHighestIndex());
   }
}
