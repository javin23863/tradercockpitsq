package SQ.Blocks.Indicators.DirectionalIndex;

import SQ.Blocks.Indicators.ADX.ADX;
import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "DI+ crosses below DI-",
   display = "DI+(@Chart@#Period#)[#Shift#] crosses below DI-(@Chart@#Period#)[#Shift#]",
   returnType = 3,
   mainIndicator = "ADX"
)
@SortOrder(600)
@OppositeBlock("DICrossUp")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50")
   }
)
public class DICrossDown extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      ADX var1 = this.Strategy.Indicators.ADX(this.Input, this.Period);
      double var2 = var1.DIPlus.getRounded(this.Shift + 1);
      double var4 = var1.DIMinus.getRounded(this.Shift + 1);
      double var6 = var1.DIPlus.getRounded(this.Shift);
      double var8 = var1.DIMinus.getRounded(this.Shift);
      return var2 > var4 && var6 < var8;
   }
}
