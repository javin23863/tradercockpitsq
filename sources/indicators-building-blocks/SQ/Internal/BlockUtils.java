package SQ.Internal;

import SQ.Blocks.Comparisons.CountComparisonBlockAbstract;
import SQ.Blocks.Comparisons.IsOneComparisonBlockAbstract;
import SQ.Blocks.Comparisons.LeftRightComparisonBlockAbstract;
import com.strategyquant.tradinglib.IBlock;

public class BlockUtils {
   public static boolean isLeftRightBlock(IBlock var0) {
      return var0 instanceof LeftRightComparisonBlockAbstract;
   }

   public static boolean isOneIndyComparison(IBlock var0) {
      return var0 instanceof IsOneComparisonBlockAbstract;
   }

   public static boolean isCountComparison(IBlock var0) {
      return var0 instanceof CountComparisonBlockAbstract;
   }
}
