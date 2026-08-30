package SQ.Blocks.Indicators.Ichimoku;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "(Ichimoku) Senkou Span cross bearish",
   display = "Ichimoku(@Chart@#TenkanPeriod#,#KijunPeriod#,#SenkouPeriod#)[#Shift#] Senkou Span cross bearish",
   returnType = 3
)
@OppositeBlock("IchimokuSenkouSpanCrossBullish")
@ParameterSets(
   {@ParameterSet(set = "TenkanPeriod=9,KijunPeriod=26,SenkouPeriod=52"), @ParameterSet(set = "TenkanPeriod=9,KijunPeriod=26,SenkouPeriod=52,SignalStrength=2")}
)
public class IchimokuSenkouSpanCrossBearish extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "9", name = "Tenkan", isPeriod = true)
   public int TenkanPeriod;
   @Parameter(defaultValue = "26", name = "Kijun", isPeriod = true)
   public int KijunPeriod;
   @Parameter(defaultValue = "52", name = "Senkou", isPeriod = true)
   public int SenkouPeriod;
   @Parameter(name = "Signal strength (at least)", defaultValue = "1")
   @Editor(type = 40, values = "Weak=0,Neutral=1,Strong=2")
   public int SignalStrength;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Ichimoku var1 = this.Strategy.Indicators.Ichimoku(this.Chart, this.TenkanPeriod, this.KijunPeriod, this.SenkouPeriod);
      double var2 = var1.SenkouSpanA.getRounded(this.Shift + 1);
      double var4 = var1.SenkouSpanA.getRounded(this.Shift);
      double var6 = var1.SenkouSpanB.getRounded(this.Shift + 1);
      double var8 = var1.SenkouSpanB.getRounded(this.Shift);
      double var10 = Math.max(var4, var8);
      double var12 = Math.min(var4, var8);
      boolean var14 = var2 > var6 && var4 < var8;
      this.SignalStrength = SQUtils.fixAllowedRange(this.SignalStrength, 0, 2, 1);
      if (this.SignalStrength == 2) {
         var14 = var14 && this.Chart.Close(this.Shift) < var12;
      } else if (this.SignalStrength == 1) {
         var14 = var14 && this.Chart.Close(this.Shift) < var10;
      } else if (this.SignalStrength == 0) {
      }

      return var14;
   }
}
