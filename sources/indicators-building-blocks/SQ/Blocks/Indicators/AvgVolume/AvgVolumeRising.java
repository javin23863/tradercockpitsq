package SQ.Blocks.Indicators.AvgVolume;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Average Volume is rising", display = "AV(@Chart@#Period#)[#Shift#] is rising", returnType = 3)
@OppositeBlock("AvgVolumeRising")
@ParameterSets({@ParameterSet(set = "Period=14"), @ParameterSet(set = "Period=20"), @ParameterSet(set = "Period=30")})
public class AvgVolumeRising extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "14", minValue = 2.0, maxValue = 10000.0, step = 1.0)
   public int Period;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      AvgVolume var1 = this.Strategy.Indicators.AvgVolume(this.Chart, this.Period);
      double var2 = var1.Value.getRounded(this.Shift + 1, 4);
      double var4 = var1.Value.getRounded(this.Shift, 4);
      return var2 < var4;
   }
}
