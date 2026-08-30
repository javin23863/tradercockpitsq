package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

abstract public class IsOneComparisonBlockAbstract extends ComparisonBlock {
	@Parameter
	public IBlock Indicator;

	@Parameter(category="Properties", builderMinValue=1, builderMaxValue=1)
	public int Shift;

}
