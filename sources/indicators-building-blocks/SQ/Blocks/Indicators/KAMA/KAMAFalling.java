package SQ.Blocks.Indicators.KAMA;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(KAF) KAMA is falling", display = "KAMA(@Chart@#ERPeriod#,#ShortPeriod#,#LongPeriod#)[#Shift#] is falling", returnType = 3)
@Help("Is triggered if KAMA is falling 2 bars")
@OppositeBlock("KAMARising")
public class KAMAFalling extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Input;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int ERPeriod;
   @Parameter(defaultValue = "2", isPeriod = true, minValue = 2.0, maxValue = 30.0, step = 1.0)
   public int ShortPeriod;
   @Parameter(defaultValue = "30", isPeriod = true, minValue = 2.0, maxValue = 50.0, step = 1.0)
   public int LongPeriod;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      KAMA var1 = this.Strategy.Indicators.KAMA(this.Input, this.ERPeriod, this.ShortPeriod, this.LongPeriod);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 > var4;
   }
}
