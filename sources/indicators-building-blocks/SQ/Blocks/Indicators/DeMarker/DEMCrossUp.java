package SQ.Blocks.Indicators.DeMarker;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "DeMarker crosses above Level", display = "DeMarker(@Chart@#Period#)[#Shift#] crosses above #Level#", returnType = 3)
@OppositeBlock(value = "DEMCrossDown", oscillator = true, middleValue = 0.5, field = "Level")
@SortOrder(500)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50"),
         @ParameterSet(weight = 3, set = "Period=14,Level=0.3"),
         @ParameterSet(weight = 3, set = "Period=20,Level=0.3"),
         @ParameterSet(weight = 3, set = "Period=30,Level=0.3"),
         @ParameterSet(weight = 3, set = "Period=40,Level=0.3"),
         @ParameterSet(weight = 3, set = "Period=50,Level=0.3"),
         @ParameterSet(weight = 3, set = "Period=14,Level=0.7"),
         @ParameterSet(weight = 3, set = "Period=20,Level=0.7"),
         @ParameterSet(weight = 3, set = "Period=30,Level=0.7"),
         @ParameterSet(weight = 3, set = "Period=40,Level=0.7"),
         @ParameterSet(weight = 3, set = "Period=50,Level=0.7")
   }
)
public class DEMCrossUp extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 1.0, step = 0.01)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      DeMarker var1 = this.Strategy.Indicators.DeMarker(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 < this.Level && var4 > this.Level;
   }
}
