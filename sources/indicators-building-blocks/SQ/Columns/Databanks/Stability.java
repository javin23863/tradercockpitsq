package SQ.Columns.Databanks;

import SQ.Functions.DailyEquityComputer;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stability extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("Stability");

   public Stability() {
      super(L.tsq("Stability"), "Decimal2", (byte)1, 0.0, 0.0, 1.0);
      this.setTooltip(L.tsq("Stability - how straight and steep is the equity curve"));
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDirectionRestrictions(new byte[]{0});
      this.setDependencies(new String[]{"NetProfit"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3 != null && !var3.isEmpty()) {
         double[] var7 = DailyEquityComputer.computeDailyEquity(var3, (byte)10);
         double[] var8 = this.getLineValues(var7);
         if (var8 != null && var8.length != 0) {
            double var9 = CorrelationComputer.calculateSimilarity(var7, var8);
            double var11 = Math.pow(var9, 2.0);
            double var13 = var1.getDouble("NetProfit");
            if (var13 < 0.0) {
               var11 *= -1.0;
            }

            return this.round2(var11);
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
