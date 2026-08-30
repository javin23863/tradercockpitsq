package SQ.Blocks.Comparisons;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;

@BuildingBlock(name = "(I ↘ MA) Indicator Crosses Below MA", display = "#Indicator# crosses below its #MAType# MA(#Period#)", returnType = 3)
@OppositeBlock("IndicatorCrossesAboveMA")
public class IndicatorCrossesBelowMA extends IndicatorMAComparisonBlockAbstract {
   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      double var1 = SQUtils.round(this.Indicator.evaluateBlock(this.Shift), 6);
      double var3 = SQUtils.round(this.Indicator.evaluateBlock(this.Shift + 1), 6);
      double var5 = this.calculateMA(this.Period, this.MAType, this.Shift);
      double var7 = this.calculateMA(this.Period, this.MAType, this.Shift + 1);
      return var3 > var7 && var1 < var5;
   }
}
