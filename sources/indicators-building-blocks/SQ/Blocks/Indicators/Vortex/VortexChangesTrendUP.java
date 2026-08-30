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

@BuildingBlock(name = "(VCU) Vortex Changes Trend UP", display = "Vortex(@Chart@#Period#)[#Shift#] Changes Trend UP", returnType = 3)
@Help("Is triggered if Vortex changes direction upwards")
@OppositeBlock("VortexChangesTrendDown")
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
public class VortexChangesTrendUP extends ConditionBlock {
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
      double var4 = var1.VIPlusSumRge.getRounded(this.Shift + 1);
      double var6 = var1.VIMinusSumRge.getRounded(this.Shift);
      double var8 = var1.VIMinusSumRge.getRounded(this.Shift + 1);
      return var2 > var6 && var4 < var8;
   }
}
