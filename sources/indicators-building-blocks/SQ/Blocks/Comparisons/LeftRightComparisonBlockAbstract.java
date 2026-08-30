package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

public abstract class LeftRightComparisonBlockAbstract extends ComparisonBlock {
   @Parameter
   public IBlock Left;
   @Parameter
   public IBlock Right;
}
