package SQ.Blocks.Comparisons;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(<) Is lower", display = "#Left# < #Right#", returnType = 3)
@OppositeBlock("IsGreater")
@SortOrder(200)
public class IsLower extends LeftRightComparisonBlockAbstract {
   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      double var1 = SQUtils.round(this.Left.evaluateBlock(), 6);
      double var3 = SQUtils.round(this.Right.evaluateBlock(), 6);
      return var1 < var3;
   }
}
