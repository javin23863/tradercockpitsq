package SQ.Blocks.Indicators.KAMA;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MT5ExtendedTemplate;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "(CAK) Bar crosses above KAMA", display = "Bar crosses above KAMA(@Chart@#ERPeriod#,#ShortPeriod#,#LongPeriod#)[#Shift#]", returnType = 3)
@Help("Is triggered if Bar closes above KAMA")
@MT5ExtendedTemplate
@OppositeBlock("BarClosesBelowKAMA")
public class BarClosesAboveKAMA extends ConditionBlock {
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
      double var2 = var1.Value.getRounded(this.Shift);
      double var4 = var1.Value.getRounded(this.Shift + 1);
      double var6 = this.Input.Close(this.Shift);
      double var8 = this.Input.Close(this.Shift + 1);
      return var2 < var6 && var4 > var8;
   }
}
