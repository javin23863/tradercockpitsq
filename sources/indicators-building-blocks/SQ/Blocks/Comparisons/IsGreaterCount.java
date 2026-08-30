package SQ.Blocks.Comparisons;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "(> X) Is greater for X bars", display = "#IndicatorLeft# > #IndicatorRight# is true #Bars# bars", returnType = 3)
@OppositeBlock("IsLowerCount")
@SortOrder(100)
public class IsGreaterCount extends CountComparisonBlockAbstract {
   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      boolean var1 = false;

      for (int var2 = 0; var2 < this.Bars; var2++) {
         int var3 = this.Shift + var2;
         double var4 = SQUtils.round(this.IndicatorLeft.evaluateBlock(var3), 5);
         double var6 = SQUtils.round(this.IndicatorRight.evaluateBlock(var3), 5);
         if (var4 < var6) {
            return false;
         }

         if (var4 == var6 && !this.NotStrict) {
            return false;
         }

         if (var4 > var6) {
            var1 = true;
         }
      }

      return var1;
   }
}
