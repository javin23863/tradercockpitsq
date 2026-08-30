package SQ.Blocks.Indicators.Momentum;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Momentum is lower than Level", display = "Momentum(@Chart@#Period#)[#Shift#] < #Level#", returnType = 3)
@SortOrder(400)
@OppositeBlock(value = "MomHigher", oscillator = true, middleValue = 100.0, field = "Level")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class MomLower extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0", minValue = 0.0, maxValue = 200.0, step = 0.01, builderMinValue = 96.0, builderMaxValue = 104.0, builderStep = 0.1)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Momentum var1 = this.Strategy.Indicators.Momentum(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift);
      return var2 < this.Level;
   }
}
