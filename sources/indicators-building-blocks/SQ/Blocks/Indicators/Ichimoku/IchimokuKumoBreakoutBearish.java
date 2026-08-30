package SQ.Blocks.Indicators.Ichimoku;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;

@BuildingBlock(
   name = "(Ichimoku) Kumo breakout bearish",
   display = "Ichimoku(@Chart@#TenkanPeriod#,#KijunPeriod#,#SenkouPeriod#)[#Shift#] Kumo breakout bearish",
   returnType = 3
)
@OppositeBlock("IchimokuKumoBreakoutBullish")
@ParameterSet(set = "TenkanPeriod=9,KijunPeriod=26,SenkouPeriod=52")
public class IchimokuKumoBreakoutBearish extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "9", name = "Tenkan", isPeriod = true)
   public int TenkanPeriod;
   @Parameter(defaultValue = "26", name = "Kijun", isPeriod = true)
   public int KijunPeriod;
   @Parameter(defaultValue = "52", name = "Senkou", isPeriod = true)
   public int SenkouPeriod;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Ichimoku var1 = this.Strategy.Indicators.Ichimoku(this.Chart, this.TenkanPeriod, this.KijunPeriod, this.SenkouPeriod);
      double var2 = this.Chart.Open(this.Shift);
      double var4 = this.Chart.Close(this.Shift);
      double var6 = var1.SenkouSpanA.getRounded(this.Shift);
      double var8 = var1.SenkouSpanB.getRounded(this.Shift);
      if (var8 > var6) {
         var8 = var6;
      }

      return var2 > var8 && var4 < var8;
   }
}
