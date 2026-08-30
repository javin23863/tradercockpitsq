package SQ.Blocks.Indicators.KeltnerChannel;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(KC) Upper band is falling", display = "Keltner Channel(@Chart@#Period#, #Deviation#).Upper[#Shift#] is falling", returnType = 3)
@Help("Is triggered if KC Upper band is falling")
@OppositeBlock("KCLowerRising")
@ParameterSets(
   {
         @ParameterSet(set = "Period=20,Deviation=1.5,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,Deviation=2,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,Deviation=2.25,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,Deviation=2.5,ComputedFrom=0")
   }
)
public class KCUpperFalling extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(name = "Period", defaultValue = "20", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "1.5", minValue = 0.01, maxValue = 10.0, step = 0.01, builderMinValue = 0.1, builderMaxValue = 7.0, builderStep = 0.1)
   public double Deviation;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Strategy.Indicators.KeltnerChannel(this.Input, this.Period, this.Deviation).Upper.getRounded(this.Shift + 1);
      double var3 = this.Strategy.Indicators.KeltnerChannel(this.Input, this.Period, this.Deviation).Upper.getRounded(this.Shift);
      return var1 > var3;
   }
}
