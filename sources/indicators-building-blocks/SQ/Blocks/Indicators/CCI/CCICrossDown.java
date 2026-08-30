package SQ.Blocks.Indicators.CCI;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "CCI crosses below Level", display = "CCI(@Chart@#Period#)[#Shift#] crosses below #Level#", returnType = 3)
@SortOrder(600)
@OppositeBlock(value = "CCICrossUp", oscillator = true, middleValue = 0.0, field = "Level")
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
         @ParameterSet(set = "Period=50,ComputedFrom=0"),
         @ParameterSet(weight = 3, set = "Period=14,ComputedFrom=0,Level=0"),
         @ParameterSet(weight = 3, set = "Period=20,ComputedFrom=0,Level=0"),
         @ParameterSet(weight = 3, set = "Period=30,ComputedFrom=0,Level=0"),
         @ParameterSet(weight = 3, set = "Period=40,ComputedFrom=0,Level=0"),
         @ParameterSet(weight = 3, set = "Period=50,ComputedFrom=0,Level=0")
   }
)
public class CCICrossDown extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0", minValue = -5000.0, maxValue = 5000.0, builderMinValue = -300.0, builderMaxValue = 300.0, step = 1.0)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      CCI var1 = this.Strategy.Indicators.CCI(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 1, 4);
      double var4 = var1.Value.getRounded(this.Shift, 4);
      return var2 > this.Level && var4 < this.Level;
   }
}
