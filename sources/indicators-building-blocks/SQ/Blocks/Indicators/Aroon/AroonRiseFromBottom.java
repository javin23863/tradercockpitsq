package SQ.Blocks.Indicators.Aroon;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Aroon Up rises from bottom", display = "Aroon(@Chart@#Period#).Up[#Shift#] rises from bottom", returnType = 3)
@Help("Is triggered if Aroon Up rises from bottom and Aroon Down is around middle of the range")
@OppositeBlock("AroonDownRiseFromBottom")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50")
   }
)
public class AroonRiseFromBottom extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(category = "Default", name = "Period", minValue = 0.0, maxValue = 1000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Aroon var1 = this.Strategy.Indicators.Aroon(this.Input, this.Period);
      double var2 = var1.Up.getRounded(this.Shift + 1);
      double var4 = var1.Up.getRounded(this.Shift);
      double var6 = var1.Down.getRounded(this.Shift);
      return var2 <= 8.0 && var4 > 8.0 && var6 > 30.0;
   }
}
