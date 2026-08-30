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

@BuildingBlock(name = "Aroon Down falls from top", display = "Aroon(@Chart@#Period#).Down[#Shift#] falls from top", returnType = 3)
@Help("Is triggered if Aroon Down falls from top and Aroon Up is around middle of the range")
@OppositeBlock("AroonUpFallFromTop")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50")
   }
)
public class AroonFallFromTop extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(category = "Default", name = "Period", minValue = 0.0, maxValue = 1000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Aroon var1 = this.Strategy.Indicators.Aroon(this.Input, this.Period);
      double var2 = var1.Down.getRounded(this.Shift + 1);
      double var4 = var1.Down.getRounded(this.Shift);
      double var6 = var1.Up.getRounded(this.Shift);
      return var2 == 100.0 && var4 < 100.0 && var6 < 70.0;
   }
}
