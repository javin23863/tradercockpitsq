package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ZScore extends DatabankColumn {
   public ZScore() {
      super(L.tsq("ZScore"), "Decimal2", (byte)1, 0.0, -10.0, 10.0);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = this.computeZIndex(var3);
      return this.round2(var7);
   }

   private double computeZIndex(OrdersList var1) {
      if (var1.size() <= 0) {
         return 0.0;
      }

      int var2 = 0;
      int var3 = 0;
      int var4 = 0;
      int var5 = 0;

      for (int var12 = 0; var12 < var1.size(); var12++) {
         Order var13 = var1.get(var12);
         if (var13.isRealOrder() && var13.isFilledOrder()) {
            double var8 = var1.get(var12).PL;
            double var10;
            if (var5 == 0) {
               var10 = 0.0;
            } else {
               var10 = var1.get(var5 - 1).PL;
            }

            if (var5 == 0) {
               var4 = 1;
            } else if (this.sign(var8) * this.sign(var10) < 0.0) {
               var4++;
            }

            if (var8 >= 0.0) {
               var2++;
            }

            if (var8 < 0.0) {
               var3++;
            }

            var5++;
         }
      }

      double var6;
      if (var3 > 0 && var2 > 0) {
         double var14 = 2.0F * var2 * var3;
         var6 = (var5 * (var4 - 0.5F) - var14) / Math.sqrt(var14 * (var14 - var5) / (var5 - 1.0F));
      } else if (var3 == 0) {
         var6 = 100000.0;
      } else {
         var6 = -100000.0;
      }

      return var6;
   }

   private double sign(double var1) {
      return var1 < 0.0 ? -1.0 : 1.0;
   }
}
