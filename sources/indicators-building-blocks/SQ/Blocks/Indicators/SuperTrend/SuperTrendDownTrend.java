package SQ.Blocks.Indicators.SuperTrend;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(STD) SuperTrend Down Trend", display = "SuperTrend(@Chart@#Mode#,#ATRPeriod#,#ATRMult#)[#Shift#] is falling", returnType = 3)
@Help("Is triggered if SuperTrend is falling")
@OppositeBlock("SuperTrendUPTrend")
@ParameterSets(
   {
         @ParameterSet(set = "ATRPeriod=12"),
         @ParameterSet(set = "ATRPeriod=24"),
         @ParameterSet(set = "ATRPeriod=48"),
         @ParameterSet(set = "ATRPeriod=120"),
         @ParameterSet(set = "ATRPeriod=480"),
         @ParameterSet(set = "ATRPeriod=10"),
         @ParameterSet(set = "ATRPeriod=20"),
         @ParameterSet(set = "ATRPeriod=40"),
         @ParameterSet(set = "ATRPeriod=100"),
         @ParameterSet(set = "ATRPeriod=500")
   }
)
public class SuperTrendDownTrend extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Input;
   @Parameter(defaultValue = "1")
   @Editor(type = 40, values = "Basic=1")
   public int Mode;
   @Parameter(defaultValue = "24", isPeriod = true, minValue = 2.0, maxValue = 240.0, step = 1.0)
   public int ATRPeriod;
   @Parameter(defaultValue = "3", isPeriod = true, minValue = 0.5, maxValue = 10.0, step = 0.1)
   public double ATRMult;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      SuperTrend var1 = this.Strategy.Indicators.SuperTrend(this.Input, this.Mode, this.ATRPeriod, this.ATRMult);
      double var2 = var1.Value.getRounded(this.Shift);
      double var4 = var1.Value.getRounded(this.Shift + 1);
      return var2 < var4;
   }
}
