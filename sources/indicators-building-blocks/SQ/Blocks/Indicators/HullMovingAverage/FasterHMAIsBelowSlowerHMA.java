package SQ.Blocks.Indicators.HullMovingAverage;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MT5ExtendedTemplate;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "(FBS) Fast HMA Is Below Slow HMA",
   display = "Fast HMA(@Chart@#FasterPeriod#)[#Shift#] Is Below Slow HMA(@Chart@#SlowerPeriod#)[#Shift#]",
   returnType = 3
)
@Help("Is triggered if Fast HMA Is below Slow HMA")
@OppositeBlock("FasterHMAIsAboveSlowerHMA")
@ParameterSets(
   {
         @ParameterSet(set = "FasterPeriod=12,SlowerPeriod=24,ComputedFrom=0"),
         @ParameterSet(set = "FasterPeriod=24,SlowerPeriod=48,ComputedFrom=0"),
         @ParameterSet(set = "FasterPeriod=12,SlowerPeriod=120,ComputedFrom=0"),
         @ParameterSet(set = "FasterPeriod=10,SlowerPeriod=50,ComputedFrom=0"),
         @ParameterSet(set = "FasterPeriod=5,SlowerPeriod=20,ComputedFrom=0")
   }
)
@MT5ExtendedTemplate
public class FasterHMAIsBelowSlowerHMA extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "12", minValue = 5.0, maxValue = 10000.0, step = 1.0)
   public int FasterPeriod;
   @Parameter(defaultValue = "24", minValue = 10.0, maxValue = 10000.0, step = 1.0)
   public int SlowerPeriod;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      HullMovingAverage var1 = this.Strategy.Indicators.HullMovingAverage(this.Input, this.FasterPeriod);
      HullMovingAverage var2 = this.Strategy.Indicators.HullMovingAverage(this.Input, this.SlowerPeriod);
      double var3 = var1.Value.getRounded(this.Shift);
      double var5 = var2.Value.getRounded(this.Shift);
      return var3 < var5;
   }
}
