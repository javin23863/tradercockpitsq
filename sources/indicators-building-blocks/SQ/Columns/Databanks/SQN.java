package SQ.Columns.Databanks;

import SQ.Functions.StatFunctions;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

public class SQN extends DatabankColumn {
   public SQN() {
      super(L.tsq("SQN"), "Decimal2", (byte)1, 0.0, 0.0, 20.0);
      this.setTooltip(L.tsq("Strategy Quality Number"));
      this.setDependencies(new String[]{"AvgLoss", "NumberOfTrades", "AvgTradesPerMonth", "Expectancy", "StandardDev"});
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDirectionRestrictions(new byte[]{0});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      DoubleArrayList var7 = new DoubleArrayList(var3.size() + 10);
      var7.clear();
      double var8 = var1.getDouble("AvgLoss");
      if (var8 == 0.0) {
         var8 = 1.0;
      }

      var8 = Math.abs(var8);

      for (int var10 = 0; var10 < var3.size(); var10++) {
         Order var11 = var3.get(var10);
         var7.add(var11.PL / var8);
      }

      int var18 = var1.getInt("NumberOfTrades");
      double var19 = var1.getDouble("AvgTradesPerMonth");
      double var13 = 0.0;
      double var15 = 0.0;
      if (var18 > 0) {
         if (var18 <= 100) {
            var13 = this.computeOriginalSQN(var7);
         } else {
            var13 = this.computeOriginalSQNAbove100Trades(var1, var7);
         }

         var15 = this.computeSQNScore(var7) * (var19 * 12.0) / 100.0;
      }

      var1.set("SQNScore", this.round2(var15));
      return this.round2(var13);
   }

   private double computeOriginalSQN(DoubleArrayList var1) {
      if (var1.size() < 3) {
         return 0.0;
      }

      double var2 = StatFunctions.computeAverage(var1);
      double var4 = this.correctStdDev(StatFunctions.computeStdev(var2, var1));
      return var4 == 0.0 ? 0.0 : var2 / var4 * Math.sqrt(var1.size());
   }

   private double computeOriginalSQNAbove100Trades(SQStats var1, DoubleArrayList var2) {
      double var3 = var1.getDouble("Expectancy");
      double var5 = var1.getDouble("StandardDev");
      double var7 = SQUtils.safeDivide(var3, var5);
      return var7 * 10.0;
   }

   private double computeSQNScore(DoubleArrayList var1) {
      double var2 = 0.0;
      byte var4 = 0;
      byte var5 = 100;
      int var6 = 0;

      int var7;
      do {
         var7 = var4 + var5;
         if (var7 > var1.size()) {
            var7 = var1.size();
         }

         double var8 = this.computeSQNOn100Orders(var1, var4, var7);
         double var10 = 1.0;
         if (var7 - var4 < var5) {
            var10 = (double)(var7 - var4) / var5;
         }

         var2 += var8 * var10;
         var6++;
         var4 += var5;
      } while (var4 <= var1.size() && var7 != var1.size());

      return var6 == 0 ? 0.0 : var2 / var6;
   }

   private double computeSQNOn100Orders(DoubleArrayList var1, int var2, int var3) {
      double var4 = StatFunctions.computeAverage(var1, var2, var3);
      double var6 = this.correctStdDev(StatFunctions.computeStdev(var4, var1, var2, var3));
      return var4 != 0.0 && var6 != 0.0 ? var4 / var6 * Math.sqrt(var3 - var2) : 0.0;
   }

   private double correctStdDev(double var1) {
      return Math.abs(var1) < 1.0E-6 ? 0.0 : var1;
   }
}
