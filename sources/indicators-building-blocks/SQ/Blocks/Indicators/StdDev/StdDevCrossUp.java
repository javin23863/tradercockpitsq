package SQ.Blocks.Indicators.StdDev;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "StdDev crosses above Level", display = "StdDev(@Chart@#Period#)[#Shift#] crosses above #Level#", returnType = 3)
@OppositeBlock("StdDevCrossUp")
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
public class StdDevCrossUp extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0.01", minValue = 1.0E-4, maxValue = 3.0, step = 0.01, builderMinValue = 1.0E-4, builderMaxValue = 3.0, builderStep = 2.5E-4)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      StdDev var1 = this.Strategy.Indicators.StdDev(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 < this.Level && var4 > this.Level;
   }
}
