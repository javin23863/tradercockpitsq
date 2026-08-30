package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

public class AND extends ComparisonBlock {
   @Parameter
   public IBlock[] Children;

   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      boolean var1 = true;

      for (int var2 = 0; var2 < this.Children.length; var2++) {
         var1 = this.Children[var2].evaluateBlock() == 1.0;
         if (!var1) {
            return false;
         }
      }

      return true;
   }
}
