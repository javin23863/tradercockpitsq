package SQ.Blocks.Indicators.MovingAverage;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Moving Average is falling", display = "#Type# Moving Average(@Chart@#Period#)[#Shift#] is falling", returnType = 3)
@Help("Is triggered if MA is falling")
@OppositeBlock("MARising")
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
public class MAFalling extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(minValue = 2.0, maxValue = 10000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Parameter(name = "Method", defaultValue = "0")
   @Editor(type = 40, values = "Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
   public int Type;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      this.Type = SQUtils.fixAllowedRange(this.Type, 0, 3, 0);
      switch (this.Type) {
         case 0:
            return this.Strategy.Indicators.SMA(this.Input, this.Period).Value.getRounded(this.Shift)
               < this.Strategy.Indicators.SMA(this.Input, this.Period).Value.getRounded(this.Shift + 1);
         case 1:
            return this.Strategy.Indicators.EMA(this.Input, this.Period).Value.getRounded(this.Shift)
               < this.Strategy.Indicators.EMA(this.Input, this.Period).Value.getRounded(this.Shift + 1);
         case 2:
            return this.Strategy.Indicators.SMMA(this.Input, this.Period).Value.getRounded(this.Shift)
               < this.Strategy.Indicators.SMMA(this.Input, this.Period).Value.getRounded(this.Shift + 1);
         case 3:
            return this.Strategy.Indicators.LWMA(this.Input, this.Period).Value.getRounded(this.Shift)
               < this.Strategy.Indicators.LWMA(this.Input, this.Period).Value.getRounded(this.Shift + 1);
         default:
            return this.Strategy.Indicators.SMA(this.Input, this.Period).Value.getRounded(this.Shift)
               < this.Strategy.Indicators.SMA(this.Input, this.Period).Value.getRounded(this.Shift + 1);
      }
   }
}
