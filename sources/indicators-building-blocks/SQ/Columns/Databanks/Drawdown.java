package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class Drawdown extends DatabankColumn {
   public Drawdown() {
      super(L.tsq("Drawdown"), "Decimal2PL", (byte)2, 0.0, 0.0, 10000.0);
      this.setDependentOnTradingPeriod(true);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3.size() == 0) {
         var1.set("AvgDrawdown", 0);
         return 0.0;
      }

      double var7 = var4.getDouble("MoneyManagement.InitialCapital", 10000.0);
      double var9 = var7;
      double var11 = var7;

      for (int var13 = 0; var13 < var3.size(); var13++) {
         Order var14 = var3.get(var13);
         var11 += var14.PL;
         var14.DD = (float)specialSubtraction(var9, var11);
         if (var11 > var9) {
            var9 = var11;
         }
      }

      double var19 = 9.9999999E7;
      double var15 = 0.0;

      for (int var17 = 0; var17 < var3.size(); var17++) {
         Order var18 = var3.get(var17);
         var15 += var18.DD;
         if (var18.DD < var19) {
            var19 = var18.DD;
         }
      }

      var1.set("AvgDrawdown", this.round2(Math.abs(this.safeDivide(var15, var3.size()))));
      return this.round2(Math.abs(var19));
   }

   private static double specialSubtraction(double var0, double var2) {
      double var4 = var0 - var2;
      if (var4 < 0.0) {
         var4 = 0.0;
      }

      return -1.0 * var4;
   }
}
