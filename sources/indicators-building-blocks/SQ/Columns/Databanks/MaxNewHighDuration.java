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

public class MaxNewHighDuration extends DatabankColumn {
   public MaxNewHighDuration() {
      super(L.tsq("Max Drawdown Duration"), "Integer", (byte)2, 0.0, 0.0, 10000.0);
      this.setTooltip(L.tsq("Max Drawdown in Days"));
      this.setDependentOnTradingPeriod(true);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3.size() == 0) {
         return 0.0;
      }

      Order var7 = var3.get(0);
      Order var8 = var3.get(var3.size() - 1);
      Order var9 = null;
      Order var10 = null;
      long var11 = 0L;
      long var13 = 0L;
      long var15 = 0L;
      double var17 = 0.0;
      OutOfSample var19 = (OutOfSample)var4.get("ChartOOS");

      for (int var20 = 0; var20 < var3.size() && var3.size() - var20 >= var13; var20++) {
         Order var21 = var3.get(var20);
         var17 += this.getPLByStatsType(var21, var2);
         double var22 = var17;
         Order var24 = null;
         int var25 = -1;

         for (int var26 = var20 + 1; var26 < var3.size(); var26++) {
            Order var27 = var3.get(var26);
            var22 += this.getPLByStatsType(var27, var2);
            if (var22 <= var17) {
               var24 = null;
               var25 = -1;
            } else if (var22 > var17 && var24 == null) {
               var24 = var27;
               var25 = var26;
            }
         }

         if (var24 == null) {
            var24 = var8;
            var25 = var3.size() - 1;
         }

         var15 = var24.CloseTime - var21.CloseTime;
         if (var15 > var11) {
            var9 = var21;
            var10 = var24;
            var11 = var15;
            var13 = var25 - var20;
         }
      }

      var9 = var9 == null ? var7 : var9;
      var10 = var10 == null ? var8 : var10;
      int var32 = SQTime.getDaysBetween(var7.OpenTime, var8.CloseTime);
      if (var9 != null && var10 != null) {
         long var33 = var9.CloseTime;
         long var23 = this.correctMaxDrawdownDurationEndTime(var33, var10.CloseTime, var19, var2.getSampleType());
         var15 = SQTime.getDaysBetween(var33, var23);
         var1.set("MaxNewHighDurationFrom", var33);
         var1.set("MaxNewHighDurationTo", var23);
         double var34 = SQUtils.safeDivide(var15, var32) * 100.0;
         var1.set("MaxNewHighDurationPct", this.round2(var34));
         return (int)var15;
      } else {
         return 0.0;
      }
   }

   private long correctMaxDrawdownDurationEndTime(long var1, long var3, OutOfSample var5, byte var6) {
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
