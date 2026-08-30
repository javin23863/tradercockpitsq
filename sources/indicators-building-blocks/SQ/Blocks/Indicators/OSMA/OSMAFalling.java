package SQ.Blocks.Indicators.OSMA;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "OSMA is falling", display = "OSMA(@Chart@#FastEMA#, #SlowEMA#, #SignalPeriod#)[#Shift#] is falling", returnType = 3)
@Help("Is triggered if OSMA is falling")
@OppositeBlock("OSMARising")
@SortOrder(200)
@ParameterSets(
   {
         @ParameterSet(set = "FastEMA=12,SlowEMA=26,SignalPeriod=9,ComputedFrom=0"),
         @ParameterSet(set = "FastEMA=24,SlowEMA=52,SignalPeriod=9,ComputedFrom=0"),
         @ParameterSet(set = "FastEMA=8,SlowEMA=17,SignalPeriod=9,ComputedFrom=0"),
         @ParameterSet(set = "FastEMA=3,SlowEMA=10,SignalPeriod=16,ComputedFrom=0")
   }
)
public class OSMAFalling extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(name = "Fast EMA", defaultValue = "12", isPeriod = true)
   public int FastEMA;
   @Parameter(name = "Slow EMA", defaultValue = "26", isPeriod = true)
   public int SlowEMA;
   @Parameter(defaultValue = "9", isPeriod = true)
   public int SignalPeriod;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      OSMA var1 = this.Strategy.Indicators.OSMA(this.Input, this.FastEMA, this.SlowEMA, this.SignalPeriod);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 > var4;
   }
}
