package SQ.Blocks.Indicators.HighestLowest;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "Bar opens below Highest after opened above",
   display = "Bar opens below Highest(@Chart@#Period#)[#Shift#] after opened above",
   returnType = 3,
   mainIndicator = "Highest"
)
@OppositeBlock("BarOpensAboveLowestAfterOpenBelow")
@ParameterSets(
   {
         @ParameterSet(set = "Period=3,ComputedFrom=2"),
         @ParameterSet(set = "Period=5,ComputedFrom=2"),
         @ParameterSet(set = "Period=10,ComputedFrom=2"),
         @ParameterSet(set = "Period=14,ComputedFrom=2"),
         @ParameterSet(set = "Period=20,ComputedFrom=2"),
         @ParameterSet(set = "Period=25,ComputedFrom=2"),
         @ParameterSet(set = "Period=30,ComputedFrom=2"),
         @ParameterSet(set = "Period=40,ComputedFrom=2"),
         @ParameterSet(set = "Period=50,ComputedFrom=2")
   }
)
public class BarOpensBelowHighestAfterOpenAbove extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "14")
   public int Period;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Strategy.Indicators.Highest(this.Chart.getSeries(this.ComputedFrom), this.Period).Value.getRounded(this.Shift + 1);
      return this.Chart.Open(this.Shift + 1) > var1 && this.Chart.Open(this.Shift) < var1;
   }
}
