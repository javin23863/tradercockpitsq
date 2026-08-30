package SQ.Blocks.Comparisons;

import SQ.Internal.ComparisonBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.Parameter;

public abstract class IndicatorMAComparisonBlockAbstract extends ComparisonBlock {
   @Parameter
   public IBlock Indicator;
   @Parameter(name = "Period", defaultValue = "14", minValue = 2.0, builderMaxValue = 250.0, maxValue = 250.0)
   @Help("Moving Average Period")
   public int Period;
   @Parameter(name = "MA Type", defaultValue = "1")
   @Editor(type = 40, values = "Simple=1,Exponential=2,Weighted=3,Hull=4")
   @Help("Simple=1,Exponential=2,Weighted=3,Hull=4")
   public int MAType;
   @Parameter(category = "Properties", builderMinValue = 1.0, builderMaxValue = 1.0)
   public int Shift;

   protected double calculateWMA(int var1, int var2) throws TradingException {
      double var3 = 0.0;
      double var5 = 0.0;

      for (int var7 = 0; var7 < var1; var7++) {
         double var8 = var1 - var7;
         var3 += SQUtils.round(this.Indicator.evaluateBlock(var2 + var7), 6) * var8;
         var5 += var8;
      }

      return var3 / var5;
   }

   protected double calculateMA(int var1, int var2, int var3) throws TradingException {
      double var4 = 0.0;
      switch (var2) {
         case 1:
            double var6 = 0.0;

            for (int var18 = 0; var18 < var1; var18++) {
               var6 += SQUtils.round(this.Indicator.evaluateBlock(var3 + var18), 6);
            }

            var4 = var6 / var1;
            break;
         case 2:
            double var8 = 2.0 / (var1 + 1.0);
            double var10 = SQUtils.round(this.Indicator.evaluateBlock(var3 + var1 - 1), 6);

            for (int var19 = var1 - 2; var19 >= 0; var19--) {
               var10 = SQUtils.round(this.Indicator.evaluateBlock(var3 + var19), 6) * var8 + var10 * (1.0 - var8);
            }

            var4 = var10;
            break;
         case 3:
            var4 = this.calculateWMA(var1, var3);
            break;
         case 4:
            int var12 = var1 / 2;
            int var13 = (int)Math.sqrt(var1);
            double var14 = this.calculateWMA(var12, var3);
            double var16 = this.calculateWMA(var1, var3);
            var4 = 2.0 * var14 - var16;
      }

      return var4;
   }
}
