package SQ.Blocks.Indicators.WilliamsPR;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Williams%R is higher than Level", display = "Williams%R(@Chart@#Period#)[#Shift#] > #Level#", returnType = 3)
@OppositeBlock(value = "WPRLower", oscillator = true, middleValue = -50.0, field = "Level")
@SortOrder(300)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14,Level=-20"),
         @ParameterSet(set = "Period=20,Level=-20"),
         @ParameterSet(set = "Period=30,Level=-20"),
         @ParameterSet(set = "Period=14,Level=-80"),
         @ParameterSet(set = "Period=20,Level=-80"),
         @ParameterSet(set = "Period=30,Level=-80"),
         @ParameterSet(set = "Period=14,Level=-50"),
         @ParameterSet(set = "Period=20,Level=-50"),
         @ParameterSet(set = "Period=30,Level=-50")
   }
)
public class WPRHigher extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0", minValue = -100.0, maxValue = 0.0, step = 0.5)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      WilliamsPR var1 = this.Strategy.Indicators.WilliamsPR(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift);
      return var2 > this.Level;
   }
}
