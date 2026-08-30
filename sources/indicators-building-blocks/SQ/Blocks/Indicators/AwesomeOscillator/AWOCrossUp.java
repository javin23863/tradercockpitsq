package SQ.Blocks.Indicators.AwesomeOscillator;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Awesome Oscillator crosses above Level", display = "AwesomeOscillator(@Chart@)[#Shift#] crosses above #Level#", returnType = 3)
@OppositeBlock(value = "AWOCrossDown", oscillator = true, middleValue = 0.0, field = "Level")
@ParameterSet(weight = 10, set = "Level=0")
@SortOrder(500)
public class AWOCrossUp extends ConditionBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "0", minValue = -500.0, maxValue = 500.0, step = 0.001, builderMinValue = -5.0, builderMaxValue = 5.0, builderStep = 1.0E-4)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      AwesomeOscillator var1 = this.Strategy.Indicators.AwesomeOscillator(this.Input);
      double var2 = var1.Value.getRounded(this.Shift + 1);
      double var4 = var1.Value.getRounded(this.Shift);
      return var2 < this.Level && var4 > this.Level;
   }
}
