package SQ.Blocks.Indicators.MovingAverage;

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

@BuildingBlock(name = "Bar closes below Moving Average", display = "Bar closes below #Type# Moving Average(@Chart@#Period#)[#Shift#]", returnType = 3)
@OppositeBlock("MABarClosesAbove")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=50"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class MABarClosesBelow extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(name = "Method", defaultValue = "0")
   @Editor(type = 40, values = "Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
   public int Type;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6,Volume=7")
   public int ComputedFrom;
   @Parameter(defaultValue = "14")
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      this.Type = SQUtils.fixAllowedRange(this.Type, 0, 3, 0);
      this.ComputedFrom = SQUtils.fixAllowedRange(this.ComputedFrom, 0, 6, 0);
      switch (this.Type) {
         case 0:
            return this.Strategy.Indicators.SMA(this.Chart.getSeries(this.ComputedFrom), this.Period).Value.getRounded(this.Shift)
               > this.Chart.Close(this.Shift);
         case 1:
            return this.Strategy.Indicators.EMA(this.Chart.getSeries(this.ComputedFrom), this.Period).Value.getRounded(this.Shift)
               > this.Chart.Close(this.Shift);
         case 2:
            return this.Strategy.Indicators.SMMA(this.Chart.getSeries(this.ComputedFrom), this.Period).Value.getRounded(this.Shift)
               > this.Chart.Close(this.Shift);
         case 3:
            return this.Strategy.Indicators.LWMA(this.Chart.getSeries(this.ComputedFrom), this.Period).Value.getRounded(this.Shift)
               > this.Chart.Close(this.Shift);
         default:
            return this.Strategy.Indicators.SMA(this.Chart.getSeries(this.ComputedFrom), this.Period).Value.getRounded(this.Shift)
               > this.Chart.Close(this.Shift);
      }
   }
}
