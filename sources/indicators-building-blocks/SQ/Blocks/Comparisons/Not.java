package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;

@BuildingBlock(name = "Not", display = "Not #Value#", returnType = 3)
@OppositeBlock("Not")
public class Not extends ComparisonBlock {
   @Parameter
   public IBlock Value;

   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      return this.Value.evaluateBlock() == 0.0;
   }
}
