package SQ.Columns.Databanks;

import SQ.Functions.DailyEquityComputer;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSquared extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("RSquared");

   public RSquared() {
      super(L.tsq("RSquared"), "Decimal2", (byte)1, 0.0, 0.0, 1.0);
      this.setTooltip(L.tsq("RSquared - how straight is the equity curve"));
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDirectionRestrictions(new byte[]{0});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3 != null && !var3.isEmpty()) {
         double[] var7 = DailyEquityComputer.computeDailyEquity(var3, (byte)10);
         double[] var8 = this.getLineValues(var7);
         if (var8 != null && var8.length != 0) {
            double var9 = 0.0;
            double var11 = 0.0;
            double var13 = 0.0;

            for (int var15 = 0; var15 < var7.length; var15++) {
               var9 += var7[var15];
               var13 += var7[var15] * var7[var15];
               var11 += var8[var15];
            }

            double var34 = var9 / var7.length;
            double var17 = var11 / var7.length;
            double var19 = 0.0;
            double var21 = 0.0;
            double var23 = 0.0;

            for (int var25 = 0; var25 < var7.length; var25++) {
               var19 += (var7[var25] - var34) * (var7[var25] - var34);
               var21 += (var8[var25] - var17) * (var8[var25] - var17);
               var23 += (var7[var25] - var34) * (var8[var25] - var17);
            }

            double var35 = var23 / var19;
            double var27 = var17 - var35 * var34;
            double var29 = 0.0;

            for (int var31 = 0; var31 < var7.length; var31++) {
               double var32 = var35 * var7[var31] + var27;
               var29 += (var32 - var17) * (var32 - var17);
            }

            double var36 = var29 / var21;
            if (var36 < 0.0) {
               var36 = 0.0;
            }

            if (var36 > 1.0) {
               var36 = 1.0;
            }

            var1.set("DataLength", var8.length);
            return this.round2(var36);
         } else {
            return 0.0;
         }
      } else {
         return 0.0;
      }
   }

   private double[] getLineValues(double[] var1) {
      if (var1 != null && var1.length != 0) {
         double[] var2 = new double[var1.length];
         double var3 = var1[0];
         double var5 = var1[var1.length - 1];

         for (int var7 = 0; var7 < var1.length; var7++) {
            var2[var7] = getLineValue(var7, 0.0, var3, var1.length, var5);
         }

         return var2;
      } else {
         return new double[0];
      }
   }

   private static double getLineValue(double var0, double var2, double var4, double var6, double var8) {
      return var4 + (var8 - var4) / (var6 - var2) * (var0 - var2);
   }
}
