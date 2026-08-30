package SQ.Blocks.Indicators.SchaffTrendCycle;

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
   name = "(STCBL) Schaff TrendCycle is below Level",
   display = "Schaff Trend Cycle(@Chart@#StochPeriod#,#FastPeriod#,#SlowPeriod#)[#Shift#] is below #Level#",
   returnType = 3
)
@Help("Is triggered if Schaff Trend Cycle is below Level")
@OppositeBlock(value = "SchaffTrendCycleAboveLevel", oscillator = true, middleValue = 50.0, field = "Level")
@ParameterSets(
   {
         @ParameterSet(set = "Level=20"),
         @ParameterSet(set = "Level=40"),
         @ParameterSet(set = "Level=60"),
         @ParameterSet(set = "Level=80"),
         @ParameterSet(set = "Level=95")
   }
)
public class SchaffTrendCycleBelowLevel extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int StochPeriod;
   @Parameter(defaultValue = "20", isPeriod = true, minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int FastPeriod;
   @Parameter(defaultValue = "50", isPeriod = true, minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int SlowPeriod;
   @Parameter(defaultValue = "20", isPeriod = false, minValue = 0.1, maxValue = 90.0, step = 0.1)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      SchaffTrendCycle var1 = this.Strategy.Indicators.SchaffTrendCycle(this.Input, this.StochPeriod, this.FastPeriod, this.SlowPeriod);
      double var2 = var1.Value.getRounded(this.Shift);
      double var4 = this.Level;
      return var2 < var4;
   }
}
