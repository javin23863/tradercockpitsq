package SQ.Blocks.Indicators.Reflex;

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
   name = "(RCU) Fast Reflex Crosses UP Slow Reflex",
   display = "Fast Reflex(@Chart@#FastPeriod#)[#Shift#] Crosses UP Slow Reflex(#SlowPeriod#)",
   returnType = 3
)
@Help("Is triggered if fast Reflex crosses Slow reflex UP")
@MT5ExtendedTemplate
@OppositeBlock("FastReflexCrossDownSlowReflex")
@ParameterSets(
   {
         @ParameterSet(set = "FastPeriod=6,FastPeriod=12"),
         @ParameterSet(set = "FastPeriod=12,FastPeriod=24"),
         @ParameterSet(set = "FastPeriod=12,FastPeriod=36"),
         @ParameterSet(set = "FastPeriod=24,FastPeriod=48"),
         @ParameterSet(set = "FastPeriod=12,FastPeriod=120"),
         @ParameterSet(set = "FastPeriod=10,FastPeriod=20"),
         @ParameterSet(set = "FastPeriod=20,FastPeriod=40"),
         @ParameterSet(set = "FastPeriod=50,FastPeriod=100")
   }
)
public class FastReflexCrossUPSlowReflex extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "24", minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int FastPeriod;
   @Parameter(defaultValue = "36", minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int SlowPeriod;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      Reflex var1 = this.Strategy.Indicators.Reflex(this.Chart, this.FastPeriod);
      Reflex var2 = this.Strategy.Indicators.Reflex(this.Chart, this.SlowPeriod);
      double var3 = var1.Value.getRounded(this.Shift);
      double var5 = var2.Value.getRounded(this.Shift);
      double var7 = var1.Value.getRounded(this.Shift + 1);
      double var9 = var2.Value.getRounded(this.Shift + 1);
      return var3 > var5 & var7 < var9;
   }
}
