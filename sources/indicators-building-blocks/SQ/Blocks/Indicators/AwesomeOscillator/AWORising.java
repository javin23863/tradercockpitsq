package SQ.Blocks.Indicators.AwesomeOscillator;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.CategoryOrder;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Awesome Oscillator is rising", display = "AwesomeOscillator(@Chart@)[#Shift#] is rising", returnType = 3)
@Help("Is triggered if Awesome Oscillator is rising")
@OppositeBlock("AWOFalling")
@SortOrder(100)
@CategoryOrder(1200)
public class AWORising extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      AwesomeOscillator var1 = this.Strategy.Indicators.AwesomeOscillator(this.Input);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 < var4;
   }
}
