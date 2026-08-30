package SQ.Blocks.Indicators.Reflex;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(RFXU) Reflex Changes Direction UP", display = "Reflex(@Chart@#Period#)[#Shift#] changes direction upwards", returnType = 3)
@Help("Is triggered if Reflex changes direction upwards")
@OppositeBlock("ReflexChangesDirectionDown")
@ParameterSets(
   {
         @ParameterSet(set = "Period=6"),
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=36"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=240"),
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=100"),
         @ParameterSet(set = "Period=200")
   }
)
public class ReflexChangesDirectionUP extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "24", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Reflex var1 = this.Strategy.Indicators.Reflex(this.Chart, this.Period);
      double var2 = var1.Value.getRounded(this.Shift);
      double var4 = var1.Value.getRounded(this.Shift + 1);
      double var6 = var1.Value.getRounded(this.Shift + 2);
      double var8 = var1.Value.getRounded(this.Shift + 3);
      return var2 > var4 && var6 > var4 || var2 > var4 && var8 > var6;
   }
}
