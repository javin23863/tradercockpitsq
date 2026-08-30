package SQ.Blocks.Indicators.BullsPower;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Bulls Power is higher than Level", display = "BullsPower(@Chart@#Period#)[#Shift#] > #Level#", returnType = 3)
@OppositeBlock(value = "BEPLower", oscillator = true, middleValue = 0.0, field = "Level")
@SortOrder(300)
@ParameterSets(
   {
         @ParameterSet(set = "Period=13,ComputedFrom=0"),
         @ParameterSet(set = "Period=13,ComputedFrom=0,Level=0"),
         @ParameterSet(set = "Period=14,ComputedFrom=0,Level=0"),
         @ParameterSet(set = "Period=15,ComputedFrom=0,Level=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0,Level=0")
   }
)
public class BUPHigher extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Parameter(defaultValue = "0", minValue = -5000.0, maxValue = 5000.0, step = 0.001, builderMinValue = -0.5, builderMaxValue = 0.5, builderStep = 0.01)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      BullsPower var1 = this.Strategy.Indicators.BullsPower(this.Input, this.Period, this.ComputedFrom);
      double var2 = var1.Value.getRounded(this.Shift);
      return var2 > this.Level;
   }
}
