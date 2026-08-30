package SQ.Blocks.Indicators.ADX;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "ADX crosses below Level", display = "ADX(@Chart@#Period#)[#Shift#] crosses below #Level#", returnType = 3)
@OppositeBlock(value = "ADXCrossDown", middleValue = 50.0, field = "Level")
@SortOrder(600)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14,ComputeFrom=0"),
         @ParameterSet(set = "Period=20,ComputeFrom=0"),
         @ParameterSet(set = "Period=30,ComputeFrom=0"),
         @ParameterSet(set = "Period=40,ComputeFrom=0"),
         @ParameterSet(set = "Period=14,ComputeFrom=0,Level=20"),
         @ParameterSet(set = "Period=20,ComputeFrom=0,Level=20"),
         @ParameterSet(set = "Period=30,ComputeFrom=0,Level=20"),
         @ParameterSet(set = "Period=40,ComputeFrom=0,Level=20"),
         @ParameterSet(set = "Period=14,ComputeFrom=0,Level=30"),
         @ParameterSet(set = "Period=20,ComputeFrom=0,Level=30"),
         @ParameterSet(set = "Period=30,ComputeFrom=0,Level=30"),
         @ParameterSet(set = "Period=40,ComputeFrom=0,Level=30"),
         @ParameterSet(set = "Period=14,ComputeFrom=0,Level=50"),
         @ParameterSet(set = "Period=20,ComputeFrom=0,Level=50"),
         @ParameterSet(set = "Period=30,ComputeFrom=0,Level=50"),
         @ParameterSet(set = "Period=40,ComputeFrom=0,Level=50")
   }
)
public class ADXCrossDown extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0", minValue = 1.0, maxValue = 99.0, step = 1.0)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      ADX var1 = this.Strategy.Indicators.ADX(this.Input, this.Period);
      double var2 = var1.Main.getRounded(this.Shift + 1);
      double var4 = var1.Main.getRounded(this.Shift);
      return var2 > this.Level && var4 < this.Level;
   }
}
