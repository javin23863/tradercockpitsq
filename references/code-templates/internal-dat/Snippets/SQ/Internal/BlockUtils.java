package SQ.Internal;

import SQ.Blocks.Comparisons.CountComparisonBlockAbstract;
import SQ.Blocks.Comparisons.IsOneComparisonBlockAbstract;
import com.strategyquant.tradinglib.IBlock;

import SQ.Blocks.Comparisons.LeftRightComparisonBlockAbstract;


/**
 * The Class BlockUtils.
 */
public class BlockUtils {

	/**
	 * Checks if is left right block.
	 *
	 * @param block the block
	 * @return true, if is left right block
	 */
	public static boolean isLeftRightBlock(IBlock block) {
		return (block instanceof LeftRightComparisonBlockAbstract);
	}

	//------------------------------------------------------------------------

	public static boolean isOneIndyComparison(IBlock block) {
		return (block instanceof IsOneComparisonBlockAbstract);
	}

	//------------------------------------------------------------------------
	public static boolean isCountComparison(IBlock block) {
		return (block instanceof CountComparisonBlockAbstract);
	}
}
