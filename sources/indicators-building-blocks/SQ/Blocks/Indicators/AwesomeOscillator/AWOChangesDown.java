package SQ.Blocks.Indicators.AwesomeOscillator;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(
   name = "Awesome Oscillator changes direction downwards",
   display = "AwesomeOscillator(@Chart@)[#Shift#] changes direction downwards",
   returnType = 3
)
@Help("Is triggered if AWO changes direction downwards")
@OppositeBlock("AWOChangesUp")
@SortOrder(100)
public class AWOChangesDown extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      AwesomeOscillator var1 = this.Strategy.Indicators.AwesomeOscillator(this.Input);
      double var2 = var1.Value.getRounded(this.Shift + 2);
      double var4 = var1.Value.getRounded(this.Shift + 1);
      double var6 = var1.Value.getRounded(this.Shift);
      return var2 < var4 && var4 > var6;
   }
}
