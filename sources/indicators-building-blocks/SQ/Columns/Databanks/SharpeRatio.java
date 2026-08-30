package SQ.Columns.Databanks;

import SQ.Functions.StatFunctions;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharpeRatio extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("SharpeRatio");
   private final long DAY_DURATION = 86400000L;

   public SharpeRatio() {
      super(L.tsq("Sharpe Ratio"), "Decimal2", (byte)1, 0.0, -1.0, 1.0);
      this.setTooltip(L.tsq("Sharpe Ratio (annualized)"));
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDirectionRestrictions(new byte[]{0});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      DoubleArrayList var7 = this.computeDailyReturn(var3);
      double var8 = StatFunctions.computeAverage(var7);
      double var10 = StatFunctions.computeStdev(var8, var7);
      double var12 = Math.sqrt(252.0) * SQUtils.safeDivide(var8, var10);
      return this.round2(var12);
   }

   private DoubleArrayList computeDailyReturn(OrdersList var1) {
      if (var1.size() == 0) {
         return null;
      }

      double var2 = 1.984126984126984E-4;
      long var4 = Long.MAX_VALUE;
      long var6 = -1L;

      for (int var8 = 0; var8 < var1.size(); var8++) {
         Order var9 = var1.get(var8);
         if (var9.CloseTime < var4) {
            var4 = var9.CloseTime;
         }

         if (var9.CloseTime > var6) {
            var6 = var9.CloseTime;
         }
      }

      var4 = SQTime.correctDayStart(var4);
      int var22 = SQTime.getDayOfWeek(var4);
      DoubleArrayList var23 = new DoubleArrayList(100);
      int var10 = 1;
      long var11 = var4;

      do {
         int var13 = var10 + var22 - 1;
         if (var13 > 7) {
            int var14 = var13 / 7;
            var13 -= var14 * 7;
         }

         if (var13 % 6 != 0 && var13 % 7 != 0) {
            var23.add(-var2);
         }

         var10++;
         var11 += 86400000L;
      } while (var11 <= var6);

      for (int var26 = 0; var26 < var1.size(); var26++) {
         Order var27 = var1.get(var26);
         int var15 = SQTime.getDayOfWeek(var27.CloseTime);
         if (var15 != 6 && var15 != 7) {
            long var16 = SQTime.correctDayStart(var27.CloseTime);
            var10 = (int)((var16 - var4) / 86400000L);
            if (var10 > var22) {
               var10 -= 2;
            }

            int var18 = var10 / 7;
            var10 -= var18 * 2;
            if (var10 >= 0 && var10 < var23.size()) {
               double var19 = var23.getDouble(var10);
               var23.set(var10, var19 + var27.PctPL);
            }
         }
      }

      return var23;
   }
}
