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
   name = "WaveTrend Main line is higher than Level",
   display = "WaveTrend(@Chart@#ChannelLength#,#AverageLength#).Main[#Shift#] > #Level#",
   returnType = 3
)
@SortOrder(1100)
@ForEngine("MT4,MT5,TS,MC")
@OppositeBlock(value = "WaveTrendMainLower", oscillator = true, middleValue = 0.0, field = "Level")
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
public class WaveTrendMainHigher extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(category = "Default", name = "ChannelLength", minValue = 2.0, maxValue = 200.0, defaultValue = "10", step = 1.0, isPeriod = true)
   public int ChannelLength;
   @Parameter(category = "Default", name = "AverageLength", minValue = 2.0, maxValue = 200.0, defaultValue = "21", step = 1.0, isPeriod = true)
   public int AverageLength;
   @Parameter(defaultValue = "0", minValue = -5000.0, maxValue = 5000.0, step = 0.01, builderMinValue = -5.0, builderMaxValue = 5.0, builderStep = 0.001)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      WaveTrend var1 = this.Strategy.Indicators.WaveTrend(this.Chart, this.ChannelLength, this.AverageLength);
      double var2 = var1.WT1.getRounded(this.Shift);
      return var2 > this.Level;
   }
}
