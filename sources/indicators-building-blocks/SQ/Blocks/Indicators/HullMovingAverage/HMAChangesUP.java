package SQ.Blocks.Indicators.HullMovingAverage;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "HMA changes direction upwards", display = "HMA(@Chart@#Period#)[#Shift#] changes direction upwards", returnType = 3)
@Help("Is triggered if HMA changes direction upwards")
@OppositeBlock("HMAChangesDown")
@ParameterSets(
   {
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=240"),
         @ParameterSet(set = "Period=12,ComputedFrom=0"),
         @ParameterSet(set = "Period=24,ComputedFrom=0"),
         @ParameterSet(set = "Period=48,ComputedFrom=0"),
         @ParameterSet(set = "Period=120,ComputedFrom=0"),
         @ParameterSet(set = "Period=240,ComputedFrom=0")
   }
)
public class HMAChangesUP extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 5.0, maxValue = 252.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      HullMovingAverage var1 = this.Strategy.Indicators.HullMovingAverage(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 2);
      double var4 = var1.Value.getRounded(this.Shift + 1);
      double var6 = var1.Value.getRounded(this.Shift);
      return var2 > var4 && var4 < var6;
   }
}
