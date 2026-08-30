package SQ.Blocks.Indicators.UlcerIndex;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "(UIDS) Ulcer Index Down Below Safe Level",
   display = "Ulcer Index Down(@Chart@#Mode#,#Period#)[#Shift#] is Below #SafeLevel#",
   returnType = 3
)
@Help("Is triggered if UlcerIndex Down is below Safe Level")
@OppositeBlock("UlcerIndexUPBelowSafeLevel")
@ParameterSets(
   {
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=96"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=100"),
         @ParameterSet(set = "Period=200")
   }
)
public class UlcerIndexDownBelowSafeLevel extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Input;
   @Parameter(defaultValue = "24", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0.2", isPeriod = false, minValue = 0.01, maxValue = 20.0, step = 0.01)
   public double SafeLevel;
   @Parameter
   public int Shift;
   @Parameter(defaultValue = "2", minValue = 2.0, maxValue = 2.0, step = 1.0)
   public int Mode = 2;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      UlcerIndex var1 = this.Strategy.Indicators.UlcerIndex(this.Input, this.Mode, this.Period);
      double var2 = var1.Value.getRounded(this.Shift);
      return var2 < this.SafeLevel;
   }
}
