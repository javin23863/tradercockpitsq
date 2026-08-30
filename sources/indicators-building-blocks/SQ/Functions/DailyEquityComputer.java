package SQ.Functions;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DailyEquityComputer {
   public static final Logger Log = LoggerFactory.getLogger(DailyEquityComputer.class);

   public static double[] computeDailyEquity(OrdersList var0, byte var1) throws Exception {
      if (var0.isEmpty()) {
         return new double[0];
      }

      long var3 = Long.MIN_VALUE;
      long var5 = Long.MAX_VALUE;

      for (int var7 = 0; var7 < var0.size(); var7++) {
         Order var8 = var0.get(var7);
         if (var8.isFilledOrder()) {
            if (var8.CloseTime > var3) {
               var3 = var8.CloseTime;
            }

            if (var8.CloseTime < var5) {
               var5 = var8.CloseTime;
            }
         }
      }

      int var19 = 3 + (int)((var3 - var5) / 86400000L);
      double[] var20 = new double[var19];
      long var9 = SQTime.getDateInMs(var5);
      double var11 = 0.0;
      int var15 = 0;

      for (int var16 = 0; var16 < var0.size(); var16++) {
         Order var2 = var0.get(var16);
         if (var2.isFilledOrder()) {
            var11 += var2.getPLByType(var1);
            long var13 = SQTime.getDateInMs(var2.CloseTime);
            if (var13 == var9) {
               var20[var15] = var11;
            } else {
               if (var13 < var9) {
                  return new double[0];
               }

               double var17 = var20[var15];

               while (var9 < var13) {
                  if (++var15 >= var20.length) {
                     Log.error("Unable to compute daily equity - index out of array {}, {},  {}, {}", new Object[]{var15, var20.length, var0.size(), var16});
                     return new double[0];
                  }

                  var20[var15] = var17;
                  var9 = SQTime.addDays(var9, 1);
               }

               var20[var15] = var11;
            }
         }
      }

      if (var15 < var20.length) {
         double var21 = var20[var15];

         for (int var18 = var15 + 1; var18 < var20.length; var18++) {
            var20[var18] = var21;
         }
      }

      return var20;
   }
}
