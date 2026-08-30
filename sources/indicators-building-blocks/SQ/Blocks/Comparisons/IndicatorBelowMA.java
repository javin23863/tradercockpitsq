package SQ.Blocks.Comparisons;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;

@BuildingBlock(name = "(I < MA) Indicator Below MA", display = "#Indicator# is below its #MAType# MA(#Period#)", returnType = 3)
@OppositeBlock("IndicatorAboveMA")
public class IndicatorBelowMA extends IndicatorMAComparisonBlockAbstract {
   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      double var1 = SQUtils.round(this.Indicator.evaluateBlock(this.Shift), 6);
      double var3 = this.calculateMA(this.Period, this.MAType, this.Shift);
      return var1 < var3;
   }
}
