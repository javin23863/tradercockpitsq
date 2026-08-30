package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.lib.TimePeriods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationLib {
   public static final Logger Log = LoggerFactory.getLogger("CorrelationLib");

   public static TimePeriod getPeriod(OrdersList var0, OrdersList var1) {
      TimePeriod var6 = getPeriod(var0);
      TimePeriod var7 = getPeriod(var1);
      long var2;
      if (var6.from < var7.from) {
         var2 = var6.from;
      } else {
         var2 = var7.from;
      }

      long var4;
      if (var6.to > var7.to) {
         var4 = var6.to;
      } else {
         var4 = var7.to;
      }

      Object var8 = null;
      Object var9 = null;
      return new TimePeriod(var2, var4);
   }

   public static TimePeriod getPeriod(OrdersList var0) {
      long var1 = -1L;
      long var3 = -1L;

      for (int var6 = 0; var6 < var0.size(); var6++) {
         Order var5 = var0.get(var6);
         if (var1 == -1L || var1 > var5.OpenTime) {
            var1 = var5.OpenTime;
         }

         if (var3 < var5.CloseTime) {
            var3 = var5.CloseTime;
         }
      }

      return new TimePeriod(var1, var3);
   }

   public static TimePeriods generatePeriods(int var0, long var1, long var3) {
      TimePeriods var5 = new TimePeriods();
      var1 = getCorrectPeriod(var1, var0);
      long var6 = var1;

      do {
         long var8 = var6;
         var8 = increasePeriod(var8, var0);
         TimePeriod var10 = new TimePeriod(var6, var8, printPeriod(var6, var0));
         var5.put(var6, var10);
         var6 = var8;
      } while (var6 <= var3);

      return var5;
   }

   private static long increasePeriod(long var0, int var2) {
      switch (var2) {
         case 5:
            return SQTime.addHours(var0, 1);
         case 10:
            return SQTime.addDays(var0, 1);
         case 20:
            return SQTime.addWeeks(var0, 1);
         case 30:
            return SQTime.addMonths(var0, 1);
         case 40:
            return SQTime.addYears(var0, 1);
         default:
            return var0;
      }
   }

   public static String printPeriod(long var0, int var2) {
      if (var2 == 5) {
         return SQTime.toDateString(var0) + " " + SQTime.getHour(var0) + ":00";
      } else if (var2 == 40) {
         return SQTime.getFullYear(var0) + " year " + SQTime.getYear(var0);
      } else if (var2 == 30) {
         return SQTime.getFullYear(var0) + " month " + SQTime.getMonthOriginal(var0);
      } else {
         return var2 == 20 ? SQTime.getFullYear(var0) + " week " + (SQTime.getWeekOfYear(var0) + 1) : SQTime.toDateString(var0);
      }
   }

   public static long getCorrectPeriod(long var0, int var2) {
      long var3 = var0;
      switch (var2) {
         case 5:
            var3 = SQTime.setMinute(var3, 0);
            var3 = SQTime.setSecond(var3, 0);
            var3 = SQTime.setMiliSeconds(var3, 0);
            break;
         case 10:
            var3 = SQTime.setHour(var3, 0);
            var3 = SQTime.setMinute(var3, 0);
            var3 = SQTime.setSecond(var3, 0);
            var3 = SQTime.setMiliSeconds(var3, 0);
            break;
         case 20:
            var3 = SQTime.setDayOfWeek(var3, 1);
            var3 = SQTime.setHour(var3, 0);
            var3 = SQTime.setMinute(var3, 0);
            var3 = SQTime.setSecond(var3, 0);
            var3 = SQTime.setMiliSeconds(var3, 0);
            break;
         case 30:
            var3 = SQTime.setDayOfMonth(var3, 1);
            var3 = SQTime.setHour(var3, 0);
            var3 = SQTime.setMinute(var3, 0);
            var3 = SQTime.setSecond(var3, 0);
            var3 = SQTime.setMiliSeconds(var3, 0);
            break;
         case 40:
            var3 = SQTime.setMonthOfYear(var3, 1);
            var3 = SQTime.setDayOfMonth(var3, 1);
            var3 = SQTime.setHour(var3, 0);
            var3 = SQTime.setMinute(var3, 0);
            var3 = SQTime.setSecond(var3, 0);
            var3 = SQTime.setMiliSeconds(var3, 0);
      }

      return var3;
   }
}
