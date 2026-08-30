package SQ.Blocks.Indicators.LinReg;

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
   name = "Bar opens above LinReg",
   display = "Bar opens above LinReg(@Chart@#Period#)[#Shift#]",
   returnType = 3,
   mainIndicator = "LinearRegression"
)
@OppositeBlock("LinRegBarOpensBelow")
@ParameterSets(
   {
         @ParameterSet(set = "Period=13,ComputedFrom=0"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class LinRegBarOpensAbove extends ConditionBlock {
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
      double var1 = this.Strategy.Indicators.LinearRegression(this.Chart.getSeries(this.ComputedFrom), this.Period).Value.getRounded(this.Shift + 1);
      return this.Chart.Open(this.Shift) > var1;
   }
}
