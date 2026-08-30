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

@BuildingBlock(name = "MACD Main line is lower than Signal", display = "MACD(@Chart@#Fast#, #Slow#, #Smooth#).Main[#Shift#] < MACD.Signal", returnType = 3)
@SortOrder(1400)
@OppositeBlock("MACDMainHigherSignal")
@ParameterSets(
   {
         @ParameterSet(set = "Fast=12,Slow=26,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=24,Slow=52,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=8,Slow=17,Smooth=9,ComputedFrom=0"),
         @ParameterSet(set = "Fast=3,Slow=10,Smooth=16,ComputedFrom=0")
   }
)
public class MACDMainLowerSignal extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "12", isPeriod = true)
   public int Fast;
   @Parameter(defaultValue = "26", isPeriod = true)
   public int Slow;
   @Parameter(defaultValue = "9", isPeriod = true)
   public int Smooth;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      MACD var1 = this.Strategy.Indicators.MACD(this.Input, this.Fast, this.Slow, this.Smooth);
      double var2 = var1.Main.getRounded(this.Shift);
      double var4 = var1.Signal.getRounded(this.Shift);
      return var2 < var4;
   }
}
