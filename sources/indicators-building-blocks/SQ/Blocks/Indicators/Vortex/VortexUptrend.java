package SQ.Blocks.Indicators.Vortex;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(VXU) Vortex is in Up Trend", display = "Vortex(@Chart@#Period#)[#Shift#] is in Up Trend", returnType = 3)
@Help("Is triggered when Vortex is in uptrend")
@OppositeBlock("VortexDowntrend")
@ParameterSets(
   {
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=120")
   }
)
public class VortexUptrend extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Vortex var1 = this.Strategy.Indicators.Vortex(this.Chart, this.Period);
      double var2 = var1.VIPlusSumRge.getRounded(this.Shift);
      double var4 = var1.VIMinusSumRge.getRounded(this.Shift);
      return var2 > var4;
   }
}
