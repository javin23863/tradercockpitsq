package SQ.Blocks.Indicators.HighestLowest;

import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Lowest", display = "Lowest(@Chart@#Period#)[#Shift#]", returnType = 2, mainIndicator = "LowestIndex")
@Help("Returns lowest value of given period of bars")
@OppositeBlock("Highest")
@ParameterSets(
   {
         @ParameterSet(set = "Period=3"),
         @ParameterSet(set = "Period=5"),
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=25"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50")
   }
)
public class Lowest extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14")
   public int Period;
   @Output(name = "Lowest", color = "#008000")
   public DataSeries Value;
   private LowestCalculator lowestCalculator;

   protected void OnInit() throws TradingException {
      this.lowestCalculator = new LowestCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      this.lowestCalculator.onBarUpdate(this.Input.get(0), this.getCurrentBar());
      this.Value.set(0, this.lowestCalculator.getLowestValue());
   }
}
