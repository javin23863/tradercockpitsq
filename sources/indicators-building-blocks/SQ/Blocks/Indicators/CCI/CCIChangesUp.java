package SQ.Blocks.Indicators.CCI;

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

@BuildingBlock(name = "CCI changes direction upwards", display = "CCI(@Chart@#Period#)[#Shift#] changes direction upwards", returnType = 3)
@Help("Is triggered if CCI changes direction upwards")
@OppositeBlock("CCIChangesDown")
@SortOrder(700)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class CCIChangesUp extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      CCI var1 = this.Strategy.Indicators.CCI(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 2, 4);
      double var4 = var1.Value.getRounded(this.Shift + 1, 4);
      double var6 = var1.Value.getRounded(this.Shift, 4);
      return var2 > var4 && var4 < var6;
   }
}
