package SQ.Blocks.Indicators.GannHiLo;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(GHU) GannHiLo is in UP Trend", display = "GannHiLo(@Chart@#Period#)[#Shift#] is in UP Trend", returnType = 3)
@Help("Is triggered if Close > GannHiLo")
@OppositeBlock("GannHiLoDownTrend")
@ParameterSets(
   {
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=5"),
         @ParameterSet(set = "Period=10")
   }
)
public class GannHiLoUPTrend extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "10", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      GannHiLo var1 = this.Strategy.Indicators.GannHiLo(this.Chart, this.Period);
      double var2 = var1.GHA.getRounded(this.Shift);
      double var4 = this.Chart.Close(this.Shift);
      return var4 > var2;
   }
}
