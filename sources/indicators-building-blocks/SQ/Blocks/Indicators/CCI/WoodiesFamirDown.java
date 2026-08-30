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

@BuildingBlock(name = "(WFD) Woodies Famir Down", display = "Woodies Famir Down(@Chart@#Period#,#Factor#)[#Shift#]", returnType = 3)
@Help(
   "Is triggered if CCI makes ZLR UP pattern and next bar fails. It is counter trend pattern; Factor = minimum distance between top and bottom hook on CCI chart."
)
@OppositeBlock("WoodiesFamirUP")
@ParameterSets({@ParameterSet(set = "Period=14,Factor = 5"), @ParameterSet(set = "Period=12,Factor = 5"), @ParameterSet(set = "Period=24,Factor = 5")})
public class WoodiesFamirDown extends ConditionBlock {
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
      boolean var16 = var2 > var4 && var6 > var4 && var14 < var2;
      boolean var17 = var2 - var4 > this.Factor && var6 - var4 > this.Factor;
      return var14 < 50.0 && var2 > 0.0 && var4 > 0.0 && var6 > 0.0 && var8 > 0.0 && var10 > 0.0 && var12 > 0.0 && var16 && var17;
   }
}
