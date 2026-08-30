package SQ.Blocks.Indicators.VWAP;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MT5ExtendedTemplate;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "(DVWAPD) Faster VWAP is below Slower VWAP",
   display = "Faster VWAP(@Chart@#FastPeriod#)[#Shift#] is below Slower VWAP(@Chart@#SlowPeriod#)[#Shift#]",
   returnType = 3
)
@Help("Is triggered if Faster VWAP is below Slower VWAP")
@MT5ExtendedTemplate
@OppositeBlock("FastVWAPAboveSlowVWAP")
@ParameterSets(
   {
         @ParameterSet(set = "FastPeriod=10,SlowPeriod=20"),
         @ParameterSet(set = "FastPeriod=20,SlowPeriod=40"),
         @ParameterSet(set = "FastPeriod=40,SlowPeriod=80"),
         @ParameterSet(set = "FastPeriod=100,SlowPeriod=200"),
         @ParameterSet(set = "FastPeriod=12,SlowPeriod=24"),
         @ParameterSet(set = "FastPeriod=24,SlowPeriod=48"),
         @ParameterSet(set = "FastPeriod=48,SlowPeriod=96"),
         @ParameterSet(set = "FastPeriod=120,SlowPeriod=240")
   }
)
public class FastVWAPBelowSlowVWAP extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "10", minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int FastPeriod;
   @Parameter(defaultValue = "20", minValue = 2.0, maxValue = 480.0, step = 1.0)
   public int SlowPeriod;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      VWAP var1 = this.Strategy.Indicators.VWAP(this.Chart, this.FastPeriod);
      VWAP var2 = this.Strategy.Indicators.VWAP(this.Chart, this.SlowPeriod);
      double var3 = var1.Value.getRounded(this.Shift);
      double var5 = var2.Value.getRounded(this.Shift);
      return var3 < var5;
   }
}
