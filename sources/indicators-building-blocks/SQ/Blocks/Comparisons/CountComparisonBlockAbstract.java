package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

public abstract class CountComparisonBlockAbstract extends ComparisonBlock {
   @Parameter(name = "Bars", defaultValue = "2", minValue = 2.0, builderMaxValue = 10.0, maxValue = 500.0, category = "Falling properties")
   @Help("")
   public int Bars;
   @Parameter(name = "Allow same values", defaultValue = "false", category = "Falling properties")
   @Help("If set to true, then condition doesn't have to be true all the time, it can have some values that are equal (but it cannot be falling)")
   public boolean NotStrict;
   @Parameter(category = "Falling properties", builderMinValue = 1.0, builderMaxValue = 1.0)
   public int Shift;
   @Parameter
   public IBlock IndicatorLeft;
   @Parameter
   public IBlock IndicatorRight;
}
