package SQ.Blocks.Indicators.Momentum;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Momentum changes direction upwards", display = "Momentum(@Chart@#Period#)[#Shift#] changes direction upwards", returnType = 3)
@OppositeBlock("MomChangesDown")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class MomChangesUp extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Momentum var1 = this.Strategy.Indicators.Momentum(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 2);
      double var4 = var1.Value.getRounded(this.Shift + 1);
      double var6 = var1.Value.getRounded(this.Shift);
      return var2 > var4 && var4 < var6;
   }
}
