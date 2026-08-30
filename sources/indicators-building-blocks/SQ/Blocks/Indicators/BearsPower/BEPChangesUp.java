package SQ.Blocks.Indicators.BearsPower;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Bears Power changes direction upwards", display = "BearsPower(@Chart@#Period#)[#Shift#] changes direction upwards", returnType = 3)
@Help("Is triggered if Bears Power changes direction upwards")
@OppositeBlock("BUPChangesDown")
@SortOrder(700)
@ParameterSets(
   {
         @ParameterSet(set = "Period=13,ComputedFrom=0"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=15,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0")
   }
)
public class BEPChangesUp extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      BearsPower var1 = this.Strategy.Indicators.BearsPower(this.Input, this.Period, this.ComputedFrom);
      double var2 = var1.Value.getRounded(this.Shift + 2);
      double var4 = var1.Value.getRounded(this.Shift + 1);
      double var6 = var1.Value.getRounded(this.Shift);
      return var2 > var4 && var4 < var6;
   }
}
