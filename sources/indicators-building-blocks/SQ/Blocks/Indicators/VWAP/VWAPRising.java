package SQ.Blocks.Indicators.VWAP;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(VWAPR) VWAP is rising", display = "VWAP(@Chart@#VWAPPeriod#)[#Shift#] is rising", returnType = 3)
@Help("Is triggered if VWAP is rising")
@OppositeBlock("VWAPFalling")
@ParameterSets(
   {
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=80"),
         @ParameterSet(set = "Period=100"),
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=96"),
         @ParameterSet(set = "Period=120")
   }
)
public class VWAPRising extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "10", minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int VWAPPeriod;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      VWAP var1 = this.Strategy.Indicators.VWAP(this.Chart, this.VWAPPeriod);
      double var2 = var1.Value.getRounded(this.Shift);
      double var4 = var1.Value.getRounded(this.Shift + 1);
      return var2 > var4;
   }
}
