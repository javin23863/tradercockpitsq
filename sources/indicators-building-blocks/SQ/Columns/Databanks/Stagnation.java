package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.strategy.OutOfSample;

public class Stagnation extends DatabankColumn {
   public Stagnation() {
      super(L.tsq("Stagnation"), "Integer", (byte)2, 0.0, 0.0, 10000.0);
      this.setTooltip(L.tsq("Stagnation in Days"));
      this.setDependentOnTradingPeriod(true);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      Object var7 = null;
      Order var8 = null;
      Order var9 = null;
      Order var10 = null;
      long var11 = 0L;
      long var13 = 0L;
      double var15 = 0.0;
      double var17 = 0.0;
      Order var19 = var3.size() > 0 ? var3.get(0) : null;
      boolean var20 = false;
      OutOfSample var21 = (OutOfSample)var4.get("ChartOOS");

      for (int var22 = 0; var22 < var3.size(); var22++) {
         Order var23 = var3.get(var22);
         double var24 = this.getPLByStatsType(var23, var2);
         var15 += var24;
         long var26 = var20 && var19 != null ? this.correctStagnationEndTime(var19.CloseTime, var23.CloseTime, var21, var2.getSampleType()) : var23.CloseTime;
         if (!(var15 > var17) && var26 == var23.CloseTime) {
            var20 = true;
         } else {
            if (var20 && var19 != null) {
               var13 = var26 - var19.CloseTime;
               if (var13 > var11) {
                  var9 = var19;
                  var10 = var23;
                  var11 = var13;
               }
            }

            var19 = var23;
            var17 = var15;
            var20 = false;
         }

         var8 = var23;
      }

      int var34;
      if (var3.isEmpty()) {
         var34 = 0;
      } else {
         var34 = SQTime.getDaysBetween(var3.get(0).OpenTime, var8.CloseTime);
      }

      long var35 = 0L;
      long var25 = 0L;
      if (var20 && var8 != null && var19 != null) {
         long var27 = var19.CloseTime;
         long var29 = this.correctStagnationEndTime(var27, var8.CloseTime, var21, var2.getSampleType());
         var13 = var29 - var27;
         if (var13 > var11) {
            var9 = var19;
            var10 = var8;
         }
      }

      if (var9 == null) {
         var9 = (Order)var7;
      }

      if (var10 == null) {
         var10 = var8;
      }

      if (var9 != null) {
         var35 = var9.CloseTime;
      }

      if (var10 != null) {
         var25 = this.correctStagnationEndTime(var35, var10.CloseTime, var21, var2.getSampleType());
      } else {
         var25 = 0L;
      }

      if (var35 != 0L && var25 != 0L) {
         var13 = SQTime.getDaysBetween(var35, var25);
      } else {
         var13 = 0L;
      }

      var1.set("StagnationFrom", var35);
      var1.set("StagnationTo", var25);
      double var37 = SQUtils.safeDivide(var13, var34) * 100.0;
      var1.set("StagnationPct", this.round2(var37));
      return (int)var13;
   }

   private long correctStagnationEndTime(long var1, long var3, OutOfSample var5, byte var6) {
      if (var5 == null) {
         return var3;
      }

      int var7 = var5.getRangesCount();

      for (int var8 = 0; var8 < var7; var8++) {
         long var9 = var5.getDateFrom(var8);
         long var11 = var5.getDateTo(var8);
         if (var6 == 10 && var1 < var11) {
            if (var3 > var9) {
               var3 = var9;
            }
            break;
         }

         if (var6 == 20 && var1 >= var9 && var1 <= var11) {
            if (var3 > var11) {
               var3 = var11;
            }
            break;
         }

         if (var6 == 20 && var1 < var9 && var8 > 0) {
            var3 = var5.getDateTo(var8 - 1);
            break;
         }
      }

      return var3;
   }
}
