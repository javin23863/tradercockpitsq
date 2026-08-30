package SQ.Blocks.Comparisons;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(
   name = "(< % Rank) Is Lower or Equal Percent Rank",
   display = "#Indicator# is lower or equal than #Percentile# % of the values over #Bars# bars in the past",
   returnType = 3
)
@OppositeBlock("IsGreaterPercentil")
@SortOrder(900)
@ForEngine("*,-SP,-SA")
public class IsLowerPercentil extends IsOneComparisonBlockAbstractPercentil {
   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      int var1 = 1;
      double var2 = 0.0;

      for (int var4 = 0; var4 < this.Bars; var4++) {
         double var5 = SQUtils.round(this.Indicator.evaluateBlock(this.Shift), 5);
         double var7 = SQUtils.round(this.Indicator.evaluateBlock(this.Shift + var4), 5);
         if (var5 < var7) {
            var1++;
         }
      }

      var2 = (double)var1 / this.Bars * 100.0;
      double var10 = SQUtils.round(this.Percentile, 5);
      return var2 > var10;
   }
}
