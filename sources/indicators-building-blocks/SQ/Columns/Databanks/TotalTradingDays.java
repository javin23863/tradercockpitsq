package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class TotalTradingDays extends DatabankColumn {
   private static final long MILISECONDS_IN_DAY = 86400000L;

   public TotalTradingDays() {
      super(L.tsq("Total Trading Days"), "Integer", (byte)2, 0.0, 10.0, 1000.0);
      this.setPLTypeRestrictions(new byte[]{10});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var4.containsKey("TotalTradingDays")
         && var4.containsKey("TotalTradingMonths")
         && var4.containsKey("TotalTradingYears")
         && var4.containsKey("ProfitableMonths")) {
         var1.set("TotalTradingMonths", var4.getDouble("TotalTradingMonths"));
         var1.set("TotalTradingYears", var4.getDouble("TotalTradingYears"));
         var1.set("ProfitableMonths", var4.getInt("ProfitableMonths"));
         return var4.getDouble("TotalTradingDays");
      }

      TotalTradingDays.TempData var7 = this.init(var3);
      if (!var3.isEmpty() && !var7.badDatesRecognized) {
         for (int var8 = 0; var8 < var3.size(); var8++) {
            Order var9 = var3.get(var8);
            long var10 = var9.OpenTime;
            long var12 = var9.CloseTime;
            int var14 = this.getDaysBetween(var7.startingDay, var10);
            int var15 = var14 / 30;
            int var16 = var14 / 365;
            int var17 = this.getDaysBetween(var7.startingDay, var12);
            int var18 = var17 / 30;
            int var19 = var17 / 365;
            if (var14 >= var7.daysInTradeArray.length) {
               var14 = var7.daysInTradeArray.length - 1;
            }

            if (var17 >= var7.daysInTradeArray.length) {
               var17 = var7.daysInTradeArray.length - 1;
            }

            if (var15 >= var7.monthsInTradeArray.length) {
               var15 = var7.monthsInTradeArray.length - 1;
            }

            if (var18 >= var7.monthsInTradeArray.length) {
               var18 = var7.monthsInTradeArray.length - 1;
            }

            if (var16 >= var7.yearsInTradeArray.length) {
               var16 = var7.yearsInTradeArray.length - 1;
            }

            if (var19 >= var7.yearsInTradeArray.length) {
               var19 = var7.yearsInTradeArray.length - 1;
            }

            for (int var20 = var14; var20 <= var17; var20++) {
               var7.daysInTradeArray[var20] = true;
            }

            for (int var21 = var15; var21 <= var18; var21++) {
               var7.monthsInTradeArray[var21] = true;
            }

            for (int var22 = var16; var22 <= var19; var22++) {
               var7.yearsInTradeArray[var22] = true;
            }
         }

         var1.set("TotalTradingMonths", this.getCount(var7.monthsInTradeArray));
         var1.set("TotalTradingYears", this.getCount(var7.yearsInTradeArray));
         var1.set("ProfitableMonths", var7.profitMonths);
         return this.getCount(var7.daysInTradeArray);
      } else {
         var1.set("TotalTradingMonths", 1);
         var1.set("TotalTradingYears", 1);
         var1.set("ProfitableMonths", 1);
         return 1.0;
      }
   }

   public TotalTradingDays.TempData init(OrdersList var1) throws Exception {
      TotalTradingDays.TempData var2 = new TotalTradingDays.TempData();
      if (!var1.isEmpty()) {
         long var3 = -1L;
         long var5 = -1L;
         int var7 = 0;
         int var8 = 0;
         double var9 = 0.0;

         for (int var11 = 0; var11 < var1.size(); var11++) {
            Order var12 = var1.get(var11);
            var8 = SQTime.getYear(var12.CloseTime) * 100000 + SQTime.getMonth(var12.CloseTime);
            if (var7 != 0 && var7 != var8) {
               if (var9 > 0.0) {
                  var2.profitMonths++;
               }

               var9 = 0.0;
            }

            var9 += var12.PL;
            var7 = var8;
            if (var5 == -1L || var12.CloseTime > var5) {
               var5 = var12.CloseTime;
            }

            if (var3 == -1L || var12.OpenTime < var3) {
               var3 = var12.OpenTime;
            }
         }

         if (var9 > 0.0) {
            var2.profitMonths++;
         }

         int var16 = SQTime.getDaysBetween(var3, var5) + 1;
         int var17 = SQTime.getMonthsBetween(var3, var5) + 1;
         int var13 = SQTime.getYearsBetween(var3, var5) + 1;
         if (var16 > 40000) {
            var2.badDatesRecognized = true;
            return var2;
         }

         var2.daysInTradeArray = new boolean[var16];
         var2.monthsInTradeArray = new boolean[var17];
         var2.yearsInTradeArray = new boolean[var13];

         for (int var14 = 0; var14 < var2.daysInTradeArray.length; var14++) {
            var2.daysInTradeArray[var14] = false;
         }

         for (int var18 = 0; var18 < var2.monthsInTradeArray.length; var18++) {
            var2.monthsInTradeArray[var18] = false;
         }

         for (int var19 = 0; var19 < var2.yearsInTradeArray.length; var19++) {
            var2.yearsInTradeArray[var19] = false;
         }

         var2.startingDay = var3;
      }

      return var2;
   }

   private int getCount(boolean[] var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < var1.length; var3++) {
         if (var1[var3]) {
            var2++;
         }
      }

      return var2;
   }

   private int getDaysBetween(long var1, long var3) {
      return (int)(Math.abs(var3 - var1) / 86400000L);
   }

   class TempData {
      public boolean[] daysInTradeArray;
      public boolean[] monthsInTradeArray;
      public boolean[] yearsInTradeArray;
      public int profitMonths;
      public long startingDay;
      public boolean badDatesRecognized = false;
   }
}
