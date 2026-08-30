package SQ.Utils;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ChartData;

public class TimeDiffUtil {
   private static final long DAY_MILLIS = 86400000L;

   public static int getDiffInDays(ChartData var0, int var1, boolean var2) throws TradingException {
      if (var1 == 0) {
         return 0;
      }

      long var3 = var0.Time(0);
      int var5 = SQTime.getDiffInDays(var3, var0.Time(var1));
      if (var5 == 0) {
         return 0;
      }

      if (var0.TimeD(0) != 0L) {
         long var12 = var0.Time(var1);

         for (int var8 = 0; var8 <= var1; var8++) {
            long var13 = SQTime.correctDayStart(var0.TimeD(var8));
            if (var13 <= var12) {
               return var8;
            }
         }

         return 0;
      } else {
         int var6 = 0;
         long var7 = var3 - var3 % 86400000L;

         for (int var9 = 1; var9 <= var1; var9++) {
            long var10 = var0.Time(var9);
            if (var10 <= var7) {
               var6++;
               var7 = var10 - var10 % 86400000L;
            }
         }

         return var6;
      }
   }

   public static int getDiffInWeeks(ChartData var0, int var1) throws TradingException {
      if (var1 == 0) {
         return 0;
      }

      long var2 = var0.Time(0);
      long var4 = var0.Time(var1);
      int var6 = SQTime.getDiffInDays(var2, var4);
      if (var6 == 0) {
         return 0;
      }

      int var7 = var6 / 7;
      var6 %= 7;
      if (var6 > 0) {
         int var8 = SQTime.getDayOfWeekOriginal(var2);
         int var9 = SQTime.getDayOfWeekOriginal(var4);
         if (var8 < var9) {
            var7++;
         }
      }

      return var7;
   }

   public static int getDiffInMonths(ChartData var0, int var1) throws TradingException {
      if (var1 == 0) {
         return 0;
      }

      long var2 = var0.Time(0);
      long var4 = var0.Time(var1);
      int var6 = SQTime.getDiffInDays(var2, var4);
      if (var6 == 0) {
         return 0;
      }

      int var7 = SQTime.getYear(var2);
      int var8 = SQTime.getMonthOriginal(var2);
      int var9 = SQTime.getYear(var4);
      int var10 = SQTime.getMonthOriginal(var4);
      if (var7 == var9) {
         return var8 - var10;
      }

      int var11 = var8;
      var7--;
      var11 += (var7 - var9) * 12;
      return var11 + (12 - var10);
   }
}
