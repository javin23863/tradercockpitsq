package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

public abstract class IsOneComparisonBlockAbstractPercentil extends ComparisonBlock {
   @Parameter
   public IBlock Indicator;
   @Parameter(name = "Bars", defaultValue = "10", minValue = 2.0, builderMaxValue = 1000.0, maxValue = 1000.0, category = "Properties")
   @Help("Number of bars for calculating the Percentile")
   public int Bars;
   @Parameter(category = "Properties", builderMinValue = 1.0, builderMaxValue = 1.0)
   public int Shift;
   @Parameter(name = "Percentile", defaultValue = "50", minValue = 0.1, builderMaxValue = 99.9, maxValue = 99.9, category = "Properties", step = 0.1)
   @Help("Percentile Value")
   public double Percentile;
}
