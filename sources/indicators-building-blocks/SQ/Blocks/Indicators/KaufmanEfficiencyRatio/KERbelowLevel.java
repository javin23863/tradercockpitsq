package SQ.Blocks.Indicators.KaufmanEfficiencyRatio;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "Kaufman Efficiency Ratio is lower than Level", display = "KER(@Chart@#Period#)[#Shift#] < #Level#", returnType = 3)
@Help("Is triggered if Kaufman Efficiency Ratio is below value")
@OppositeBlock("KERbelowLevel")
@ParameterSets({@ParameterSet(set = "Period=12"), @ParameterSet(set = "Period=24"), @ParameterSet(set = "Period=48"), @ParameterSet(set = "Period=120")})
public class KERbelowLevel extends ConditionBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "10", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0.5", minValue = 0.05, maxValue = 1.0, step = 0.05)
   public double Level;
   @Parameter
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      KaufmanEfficiencyRatio var1 = this.Strategy.Indicators.KaufmanEfficiencyRatio(this.Chart, this.Period);
      double var2 = var1.Value.getRounded(this.Shift);
      return var2 < this.Level;
   }
}
