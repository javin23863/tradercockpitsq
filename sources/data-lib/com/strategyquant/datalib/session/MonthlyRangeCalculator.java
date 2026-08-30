package com.strategyquant.datalib.session;

import com.strategyquant.lib.SQTime;
import java.util.Calendar;

public class MonthlyRangeCalculator {
   public static long getSessionStartTime(long var0, long var2, long var4) {
      int var6 = SQTime.getDayOfWeek(var2);
      int var7 = SQTime.getDayOfWeek(var4);
      long var8 = findFirstUsableDay(var0, var6, var7);
      Calendar var10 = Calendar.getInstance();
      var10.setTimeInMillis(var8);
      var10.set(11, SQTime.getHour(var2));
      var10.set(12, SQTime.getMinute(var2));
      var10.set(13, 0);
      var10.set(14, 0);
      return var10.getTimeInMillis();
   }

   public static long getSessionEndTime(long var0, long var2, long var4) {
      int var6 = SQTime.getDayOfWeek(var2);
      int var7 = SQTime.getDayOfWeek(var4);
      long var8 = findLastUsableDay(var0, var6, var7);
      Calendar var10 = Calendar.getInstance();
      var10.setTimeInMillis(var8);
      var10.set(11, SQTime.getHour(var4));
      var10.set(12, SQTime.getMinute(var4));
      var10.set(13, 0);
      var10.set(14, 0);
      return var10.getTimeInMillis();
   }

   private static long findFirstUsableDay(long var0, int var2, int var3) {
      Calendar var4 = Calendar.getInstance();
      var4.setTimeInMillis(var0);
      var4.set(5, 1);
      var4.add(5, -1);
      int var5 = 30;

      while (true) {
         var4.add(5, 1);
         int var6 = SQTime.getDayOfWeek(var4.getTimeInMillis());
         if (dayIsInSession(var6, var2, var3) || var5 <= 0) {
            return var4.getTimeInMillis();
         }

         var5--;
      }
   }

   private static long findLastUsableDay(long var0, int var2, int var3) {
      Calendar var4 = Calendar.getInstance();
      var4.setTimeInMillis(var0);
      var4.set(5, 1);
      var4.add(2, 1);
      int var5 = 30;

      while (true) {
         var4.add(5, -1);
         int var6 = SQTime.getDayOfWeek(var4.getTimeInMillis());
         if (dayIsInSession(var6, var2, var3) || var5 <= 0) {
            return var4.getTimeInMillis();
         }

         var5--;
      }
   }

   private static boolean dayIsInSession(int var0, int var1, int var2) {
      int[] var3 = new int[]{1, 2, 3, 4, 5, 6, 7, 1, 2, 3, 4, 5, 6, 7};
      if (var1 > var2) {
         var2 += 7;
      }

      for (int var4 = var1; var4 <= var2; var4++) {
         if (var3[var4 - 1] == var0) {
            return true;
         }
      }

      return false;
   }
}
