package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

abstract public class LeftRightComparisonBlockAbstract extends ComparisonBlock {
	@Parameter
	public IBlock Left;
	
	@Parameter
	public IBlock Right;

}
