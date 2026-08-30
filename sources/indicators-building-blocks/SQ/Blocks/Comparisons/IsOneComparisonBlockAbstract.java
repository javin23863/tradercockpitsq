package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

public abstract class IsOneComparisonBlockAbstract extends ComparisonBlock {
   @Parameter
   public IBlock Indicator;
   @Parameter(category = "Properties", builderMinValue = 1.0, builderMaxValue = 1.0)
   public int Shift;
}
