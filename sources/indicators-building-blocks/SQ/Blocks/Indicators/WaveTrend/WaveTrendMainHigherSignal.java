package SQ.Blocks.Indicators.WaveTrend;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "WaveTrend Main line is higher than Signal",
   display = "WaveTrend(@Chart@#ChannelLength#,#AverageLength#).Main[#Shift#] > WaveTrend.Signal",
   returnType = 3
)
@SortOrder(1300)
@ForEngine("MT4,MT5,TS,MC")
@OppositeBlock("WaveTrendMainLowerSignal")
@ParameterSets(
   {
         @ParameterSet(set = "ChannelLength=10,AverageLength=21"),
         @ParameterSet(set = "ChannelLength=9,AverageLength=12"),
         @ParameterSet(set = "ChannelLength=14,AverageLength=21"),
         @ParameterSet(set = "ChannelLength=10,AverageLength=21,ComputedFrom=0"),
         @ParameterSet(set = "ChannelLength=9,AverageLength=12,ComputedFrom=0"),
         @ParameterSet(set = "ChannelLength=14,AverageLength=21,ComputedFrom=0")
   }
)
public class WaveTrendMainHigherSignal extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(category = "Default", name = "ChannelLength", minValue = 2.0, maxValue = 200.0, defaultValue = "10", step = 1.0, isPeriod = true)
   public int ChannelLength;
   @Parameter(category = "Default", name = "AverageLength", minValue = 2.0, maxValue = 200.0, defaultValue = "21", step = 1.0, isPeriod = true)
   public int AverageLength;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      WaveTrend var1 = this.Strategy.Indicators.WaveTrend(this.Chart, this.ChannelLength, this.AverageLength);
      double var2 = var1.WT1.getRounded(this.Shift);
      double var4 = var1.WT2.getRounded(this.Shift);
      return var2 > var4;
   }
}
