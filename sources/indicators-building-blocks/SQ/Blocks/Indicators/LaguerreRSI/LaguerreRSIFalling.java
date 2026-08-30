package SQ.Blocks.Indicators.LaguerreRSI;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(LRSIF) Laguerre RSI is falling", display = "Laguerre RSI(@Chart@#Gamma#)[#Shift#] is falling", returnType = 3)
@Help("Is triggered if Laguerre RSI is falling")
@OppositeBlock("LaguerreRSIRising")
@ParameterSets(
   {
         @ParameterSet(set = "Gamma=0.1"),
         @ParameterSet(set = "Gamma=0.2"),
         @ParameterSet(set = "Gamma=0.3"),
         @ParameterSet(set = "Gamma=0.4"),
         @ParameterSet(set = "Gamma=0.5"),
         @ParameterSet(set = "Gamma=0.6"),
         @ParameterSet(set = "Gamma=0.7"),
         @ParameterSet(set = "Gamma=0.8"),
         @ParameterSet(set = "Gamma=0.9")
   }
)
public class LaguerreRSIFalling extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "0.5", minValue = 0.0, maxValue = 0.95, step = 0.01)
   public double Gamma;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      LaguerreRSI var1 = this.Strategy.Indicators.LaguerreRSI(this.Chart, this.Gamma);
      double var2 = var1.LRSI.getRounded(this.Shift);
      double var4 = var1.LRSI.getRounded(this.Shift + 1);
      return var2 < var4;
   }
}
