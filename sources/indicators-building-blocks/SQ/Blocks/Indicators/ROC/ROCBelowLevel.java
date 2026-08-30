package SQ.Blocks.Indicators.ROC;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(ROCBL) ROC is below Level", display = "ROC(@Chart@#Period#)[#Shift#] is below #Level#", returnType = 3)
@Help("Is triggered if ROC is above Level")
@OppositeBlock(value = "ROCAboveLevel", oscillator = true, middleValue = 0.0, field = "Level")
@ParameterSets(
   {
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50"),
         @ParameterSet(set = "Period=60"),
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=48"),
         @ParameterSet(set = "Period=96"),
         @ParameterSet(set = "Period=120"),
         @ParameterSet(set = "Period=100")
   }
)
public class ROCBelowLevel extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0", minValue = -100.0, maxValue = 100.0, step = 0.001)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      ROC var1 = this.Strategy.Indicators.ROC(this.Chart, this.Period);
      double var2 = var1.Value.getRounded(this.Shift);
      return var2 < this.Level;
   }
}
