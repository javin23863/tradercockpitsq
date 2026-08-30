package SQ.Blocks.Indicators.HighestLowest;

import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(LI) Lowest index", display = "LowestIndex(@Chart@#Period#)[#Shift#]", returnType = 1, mainIndicator = "LowestIndex")
@Help("returns shift of the bar with lowest value from the bars of given type")
@OppositeBlock("LowestIndex")
@IgnoreInBuilder
public class LowestIndex extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(category = "Default", name = "Period", minValue = 0.0, maxValue = 10000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Output
   public DataSeries Value;
   private LowestCalculator lowestCalculator;

   protected void OnInit() throws TradingException {
      this.lowestCalculator = new LowestCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.lowestCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.Value.set(0, this.lowestCalculator.getLowestIndex());
   }
}
