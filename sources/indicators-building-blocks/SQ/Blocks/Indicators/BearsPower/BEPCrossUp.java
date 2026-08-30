package SQ.Blocks.Indicators.BearsPower;

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

@BuildingBlock(name = "Bears Power crosses above Level", display = "BearsPower(@Chart@#Period#)[#Shift#] crosses above #Level#", returnType = 3)
@OppositeBlock(value = "BUPCrossDown", oscillator = true, middleValue = 0.0, field = "Level")
@SortOrder(500)
@ParameterSets(
   {
         @ParameterSet(set = "Period=13,ComputedFrom=0"),
         @ParameterSet(set = "Period=13,ComputedFrom=0,Level=0"),
         @ParameterSet(set = "Period=14,ComputedFrom=0,Level=0"),
         @ParameterSet(set = "Period=15,ComputedFrom=0,Level=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0,Level=0")
   }
)
public class BEPCrossUp extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Parameter(defaultValue = "0", minValue = -5000.0, maxValue = 5000.0, step = 0.001, builderMinValue = -5.0, builderMaxValue = 5.0, builderStep = 0.001)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      BearsPower var1 = this.Strategy.Indicators.BearsPower(this.Input, this.Period, this.ComputedFrom);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 < this.Level && var4 > this.Level;
   }
}
