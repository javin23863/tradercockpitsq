package SQ.Blocks.Indicators.MACD;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "MACD Main line crosses above Level",
   display = "MACD(@Chart@#Fast#, #Slow#, #Smooth#).Main[#Shift#] crosses above #Level#",
   returnType = 3
)
@SortOrder(300)
@OppositeBlock(value = "MACDMainCrossBelow", oscillator = true, middleValue = 0.0, field = "Level")
@ParameterSets(
   {
         @ParameterSet(set = "Fast=12,Slow=26,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=24,Slow=52,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=8,Slow=17,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=3,Slow=10,Smooth=16,ComputedFrom=0")
   }
)
public class MACDMainCrossAbove extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "12", isPeriod = true)
   public int Fast;
   @Parameter(defaultValue = "26", isPeriod = true)
   public int Slow;
   @Parameter(defaultValue = "9", isPeriod = true)
   public int Smooth;
   @Parameter(defaultValue = "0", minValue = -5000.0, maxValue = 5000.0, step = 0.01, builderMinValue = -5.0, builderMaxValue = 5.0, builderStep = 0.001)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      MACD var1 = this.Strategy.Indicators.MACD(this.Input, this.Fast, this.Slow, this.Smooth);
      double var2 = var1.Main.getRounded(this.Shift + 1);
      double var4 = var1.Main.getRounded(this.Shift);
      return var2 < this.Level && var4 > this.Level;
   }
}
