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

@BuildingBlock(name = "(ZBD) CCI Zero Line Break Down", display = "CCI(@Chart@#Period#)[#Shift#] Zero Line Break Down", returnType = 3)
@Help("Is triggered if CCI cross zero line down")
@OppositeBlock("WoodiesCCIZeroLineBreakUP")
@ParameterSets({@ParameterSet(set = "Period=6"), @ParameterSet(set = "Period=12"), @ParameterSet(set = "Period=24"), @ParameterSet(set = "Period=48")})
public class WoodiesCCIZeroLineBreakDown extends ConditionBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      CCI var1 = this.Strategy.Indicators.CCI(this.Input, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 > 0.0 && var4 < 0.0;
   }
}
