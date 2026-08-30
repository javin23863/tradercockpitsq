package SQ.Blocks.Functions;

import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import java.util.Arrays;

@BuildingBlock(name = "(IH) Indicator Highest Value", display = "Indicator Highest(#NthValue#, #Period#, #Indicator#)", returnType = 1)
@Help("Highest value of indicator in given period")
@SortOrder(1000)
@IgnoreInBuilder
public class IndicatorHighest extends ValueBlock {
   private static final int MAX_VALUES = 1000;
   @Parameter
   public IBlock Indicator;
   @Parameter(defaultValue = "5", minValue = 2.0, maxValue = 999.0)
   @Help("Period (bars back) in which indicator highest value is checked")
   public int Period;
   @Parameter(name = "Nth highest value", defaultValue = "0", minValue = 0.0)
   @Help("Nth highest value in given period. ) means highest value, 1 means second highest value, etc.")
   public int NthValue;

   @Override
   public double OnBlockEvaluate(int var1) throws TradingException {
      if (this.NthValue >= 0 && this.NthValue < this.Period) {
         double[] var2 = new double[1000];

         for (int var3 = 0; var3 < 1000; var3++) {
            if (var3 < this.Period) {
               var2[var3] = this.Indicator.evaluateBlock(var3 + var1);
            } else {
               var2[var3] = -2.147483647E9;
            }
         }

         Arrays.sort(var2);
         return var2[999 - this.NthValue];
      } else {
         return -1.0;
      }
   }
}
