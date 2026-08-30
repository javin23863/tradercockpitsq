package SQ.Blocks.Indicators.SRPercentRank;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "(PRAL) SR Percent Rank is above Level",
   display = " SR Percent Rank(@Chart@#Mode#,#Length#,#ATRPeriod#)[#Shift#] is above #Level#",
   returnType = 3
)
@Help("Is triggered if SRPercentRank is below Level")
@OppositeBlock("SRPercRankAboveLevel")
@ParameterSets(
   {
         @ParameterSet(set = "Mode=1,Length=12,ATRPeriod=12,Level = 50"),
         @ParameterSet(set = "Mode=1,Length=24,ATRPeriod=12,Level = 60"),
         @ParameterSet(set = "Mode=1,Length=48,ATRPeriod=12,Level = 70"),
         @ParameterSet(set = "Mode=1,Length=120,ATRPeriod=12,Level = 80"),
         @ParameterSet(set = "Mode=1,Length=240,ATRPeriod=12,Level = 90"),
         @ParameterSet(set = "Mode=1,Length=480,ATRPeriod=12"),
         @ParameterSet(set = "Mode=2,Length=12,ATRPeriod=12,Level = 50"),
         @ParameterSet(set = "Mode=2,Length=24,ATRPeriod=12,Level = 60"),
         @ParameterSet(set = "Mode=2,Length=48,ATRPeriod=12,Level = 70"),
         @ParameterSet(set = "Mode=2,Length=120,ATRPeriod=12,Level = 80"),
         @ParameterSet(set = "Mode=2,Length=240,ATRPeriod=12,Level = 90"),
         @ParameterSet(set = "Mode=2,Length=480,ATRPeriod=12")
   }
)
public class SRPercRankAboveLevel extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "2")
   @Editor(type = 40, values = "Basic Mode=1,ATR Mode=2")
   public int Mode;
   @Parameter(defaultValue = "120", isPeriod = false, minValue = 4.0, maxValue = 480.0, step = 4.0)
   public int Length;
   @Parameter(defaultValue = "12", isPeriod = true, minValue = 2.0, maxValue = 240.0, step = 2.0)
   public int ATRPeriod;
   @Parameter(defaultValue = "80", isPeriod = true, minValue = 0.0, maxValue = 99.5, step = 0.5)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      SRPercentRank var1 = this.Strategy.Indicators.SRPercentRank(this.Chart, this.Mode, this.Length, this.ATRPeriod);
      double var2 = var1.Value.getRounded(this.Shift);
      return var2 > this.Level;
   }
}
