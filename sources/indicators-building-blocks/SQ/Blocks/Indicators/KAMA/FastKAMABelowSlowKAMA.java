package SQ.Blocks.Indicators.KAMA;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MT5ExtendedTemplate;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(
   name = "(FBSK)  Fast KAMA is below Slow KAMA",
   display = "Fast KAMA(@Chart@#FastERPeriod#,#FastShortPeriod#,#FastLongPeriod#)[#Shift#] is below Slow KAMA(@Chart@#SlowERPeriod#,#SlowShortPeriod#,#SlowLongPeriod#)",
   returnType = 3
)
@Help("Is triggered if Fast KAMA is below Slow KAMA")
@MT5ExtendedTemplate
@OppositeBlock("FastKAMAAboveSlowKAMA")
public class FastKAMABelowSlowKAMA extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Input;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int FastERPeriod;
   @Parameter(defaultValue = "2", isPeriod = true, minValue = 2.0, maxValue = 30.0, step = 1.0)
   public int FastShortPeriod;
   @Parameter(defaultValue = "30", isPeriod = true, minValue = 2.0, maxValue = 50.0, step = 1.0)
   public int FastLongPeriod;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int SlowERPeriod;
   @Parameter(defaultValue = "5", isPeriod = true, minValue = 2.0, maxValue = 30.0, step = 1.0)
   public int SlowShortPeriod;
   @Parameter(defaultValue = "30", isPeriod = true, minValue = 2.0, maxValue = 50.0, step = 1.0)
   public int SlowLongPeriod;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      KAMA var1 = this.Strategy.Indicators.KAMA(this.Input, this.FastERPeriod, this.FastShortPeriod, this.FastLongPeriod);
      KAMA var2 = this.Strategy.Indicators.KAMA(this.Input, this.SlowERPeriod, this.SlowShortPeriod, this.SlowLongPeriod);
      double var3 = var1.Value.getRounded(this.Shift);
      double var5 = var2.Value.getRounded(this.Shift);
      return var3 < var5;
   }
}
