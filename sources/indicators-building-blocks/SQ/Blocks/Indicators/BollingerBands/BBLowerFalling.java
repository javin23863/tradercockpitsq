package SQ.Blocks.Indicators.BollingerBands;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(BB) Lower band is falling", display = "BB(@Chart@#Period#, #Deviation#).Lower[#Shift#] is falling", returnType = 3)
@Help("Is triggered if BB is falling")
@OppositeBlock("BBUpperRising")
@ParameterSets(
   {
         @ParameterSet(set = "Period=20,Deviation=2,ComputedFrom=0"),
         @ParameterSet(set = "Period=10,Deviation=1.9,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,Deviation=2.1,ComputedFrom=0")
   }
)
public class BBLowerFalling extends ConditionBlock {
   @Parameter
   public DataSeries Chart;
   @Parameter(minValue = 2.0, maxValue = 10000.0, defaultValue = "20", step = 1.0)
   public int Period;
   @Parameter(defaultValue = "2", minValue = 0.01, maxValue = 10.0, step = 0.01, builderMinValue = 0.1, builderMaxValue = 7.0, builderStep = 0.1)
   public double Deviation;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Strategy.Indicators.BollingerBands(this.Chart, this.Period, this.Deviation).Lower.getRounded(this.Shift + 1);
      double var3 = this.Strategy.Indicators.BollingerBands(this.Chart, this.Period, this.Deviation).Lower.getRounded(this.Shift);
      return var1 > var3;
   }
}
