package SQ.Blocks.Comparisons;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Crosses Above", display = "#Left# crosses above #Right#", returnType = 3)
@OppositeBlock("CrossesBelow")
@SortOrder(700)
public class CrossesAbove extends LeftRightComparisonBlockAbstract {
   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      double var1 = SQUtils.round(this.Left.evaluateBlock(1), 6);
      double var3 = SQUtils.round(this.Right.evaluateBlock(1), 6);
      double var5 = SQUtils.round(this.Left.evaluateBlock(0), 6);
      double var7 = SQUtils.round(this.Right.evaluateBlock(0), 6);
      return var1 < var3 && var5 > var7;
   }
}
