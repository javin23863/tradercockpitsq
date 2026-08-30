package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

public abstract class CountComparisonBlockAbstractPercentile extends ComparisonBlock {
   @Parameter(name = "Bars", defaultValue = "10", minValue = 2.0, builderMaxValue = 500.0, maxValue = 500.0, category = "Falling properties")
   @Help("")
   public int Bars;
   @Parameter(category = "Falling properties", builderMinValue = 1.0, builderMaxValue = 1.0)
   public int Shift;
   @Parameter
   public IBlock IndicatorLeft;
   @Parameter
   public IBlock IndicatorRight;
}
