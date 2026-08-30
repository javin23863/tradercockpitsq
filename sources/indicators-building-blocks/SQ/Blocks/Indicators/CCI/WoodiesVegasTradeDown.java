package SQ.Blocks.Indicators.CCI;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(WVD) Woodies Vegas Trade Down", display = "Woodies Vegas Trade Down(@Chart@#Period#,#Factor#)[#Shift#]", returnType = 3)
@Help(
   "Is triggered if CCI falls back from 200 leves and make U. It is counter trend pattern, Factor = minimum distance between top and bottom hook on CCI chart"
)
@OppositeBlock("WoodiesVegasTradeUP")
@ParameterSets(
   {
         @ParameterSet(set = "Period=14,Factor=5"),
         @ParameterSet(set = "Period=12,Factor=5"),
         @ParameterSet(set = "Period=24,Factor=5"),
         @ParameterSet(set = "Period=48,Factor=5"),
         @ParameterSet(set = "Period=96,Factor=5"),
         @ParameterSet(set = "Period=120,Factor=5")
   }
)
public class WoodiesVegasTradeDown extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "5", minValue = 1.0, maxValue = 50.0, step = 1.0)
   public int Factor;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      CCI var1 = this.Strategy.Indicators.CCI(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift + 2);
      double var6 = var1.Value.getRounded(this.Shift + 3);
      double var8 = var1.Value.getRounded(this.Shift + 4);
      double var10 = var1.Value.getRounded(this.Shift + 5);
      double var12 = var1.Value.getRounded(this.Shift + 6);
      double var14 = var1.Value.getRounded(this.Shift);
      boolean var16 = var8 - var6 > this.Factor && var2 - var4 > this.Factor;
      boolean var17 = var2 > 0.0 && var4 > 0.0 && var6 > 0.0 && var8 > 0.0 && var10 > 0.0 && var12 > 0.0;
      boolean var18 = var2 > var4 && var2 > var6 && var8 > var4 && var8 > var6;
      boolean var19 = var2 > 200.0 || var4 > 200.0 || var6 > 200.0 || var8 > 200.0 || var10 > 200.0;
      return var19 && var18 && var16 && var17 && var14 < (var6 + var4) / 2.0;
   }
}
