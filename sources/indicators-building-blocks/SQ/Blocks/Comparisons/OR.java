package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

public class OR extends ComparisonBlock {
   @Parameter
   public IBlock[] Children;

   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      for (int var1 = 0; var1 < this.Children.length; var1++) {
         if (this.Children[var1].evaluateBlock() == 1.0) {
            return true;
         }
      }

      return false;
   }
}
