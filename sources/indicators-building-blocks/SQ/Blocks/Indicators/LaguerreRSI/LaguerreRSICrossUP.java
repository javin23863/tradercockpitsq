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

@BuildingBlock(name = "(LRSIU) Laguerre RSI crosses above Level", display = "Laguerre RSI(@Chart@#Gamma#)[#Shift#] crosses above #Level#", returnType = 3)
@Help("Is triggered if Laguerre RSI crosses above Level")
@OppositeBlock(value = "LaguerreRSICrossDown", oscillator = true, middleValue = 0.5, field = "Level")
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
         @ParameterSet(set = "Gamma=0.9"),
         @ParameterSet(set = "Level=0.1"),
         @ParameterSet(set = "Level=0.2"),
         @ParameterSet(set = "Level=0.3"),
         @ParameterSet(set = "Level=0.4"),
         @ParameterSet(set = "Level=0.5"),
         @ParameterSet(set = "Level=0.6"),
         @ParameterSet(set = "Level=0.7"),
         @ParameterSet(set = "Level=0.8"),
         @ParameterSet(set = "Level=0.9")
   }
)
public class LaguerreRSICrossUP extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "0.5", minValue = 0.0, maxValue = 0.95, step = 0.01)
   public double Gamma;
   @Parameter
   public int Shift;
   @Parameter(defaultValue = "0.5", minValue = 0.05, maxValue = 0.95, builderMinValue = 0.05, builderMaxValue = 0.95, step = 0.05)
   public double Level;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      LaguerreRSI var1 = this.Strategy.Indicators.LaguerreRSI(this.Chart, this.Gamma);
      double var2 = var1.LRSI.getRounded(this.Shift);
      double var4 = var1.LRSI.getRounded(this.Shift + 1);
      return var2 > this.Level && var4 < this.Level;
   }
}
