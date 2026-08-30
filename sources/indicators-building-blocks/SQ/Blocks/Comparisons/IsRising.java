package SQ.Blocks.Comparisons;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "Is rising", display = "#Indicator# is rising", returnType = 3)
@OppositeBlock("IsFalling")
@SortOrder(900)
public class IsRising extends IsOneComparisonBlockAbstract {
   @Parameter(name = "Bars rising", defaultValue = "2", minValue = 2.0, builderMaxValue = 50.0, maxValue = 100.0, category = "Properties")
   @Help("Number of bars the value has to be rising")
   public int Bars;
   @Parameter(name = "Allow same values", defaultValue = "false", category = "Properties")
   @Help("If set to true, then indicator doesn't have to be rising all the time, it can have some values that are equal (but it cannot be falling)")
   public boolean NotStrict;

   @Override
   public boolean OnEvaluateComparison() throws TradingException {
      boolean var1 = false;
      double var2 = SQUtils.round(this.Indicator.evaluateBlock(this.Bars + this.Shift - 1), 6);

      for (int var4 = 1; var4 < this.Bars; var4++) {
         double var5 = SQUtils.round(this.Indicator.evaluateBlock(this.Bars + this.Shift - 1 - var4), 6);
         if (var5 < var2) {
            return false;
         }

         if (var5 == var2 && !this.NotStrict) {
            return false;
         }

         if (var5 > var2) {
            var1 = true;
         }

         var2 = var5;
      }

      return var1;
   }
}
