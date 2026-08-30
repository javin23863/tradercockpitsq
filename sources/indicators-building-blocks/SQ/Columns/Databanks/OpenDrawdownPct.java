package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class OpenDrawdownPct extends DatabankColumn {
   public OpenDrawdownPct() {
      super(L.tsq("Open Drawdown %"), "Decimal2Pct", (byte)2, 0.0, 0.0, 10000.0);
      this.setDependentOnTradingPeriod(true);
      this.setTooltip(L.tsq("Max % Drawdown computed from MAE (Maximum Adverse Excursion)"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3.size() == 0) {
         return 0.0;
      }

      double var7 = var4.getDouble("MoneyManagement.InitialCapital", 10000.0);
      double var9 = 0.0;
      double var11 = var7;
      double var13 = var7;

      for (int var17 = 0; var17 < var3.size(); var17++) {
         Order var18 = var3.get(var17);
         var9 = var11 - var18.MAE;
         var11 += var18.PL;
         double var15 = specialSubtraction(var13, var9);
         var18.Extra1 = (float)this.getPercentageDD(var15, var13);
         if (var11 > var13) {
            var13 = var11;
         }
      }

      double var22 = 9.9999999E7;

      for (int var19 = 0; var19 < var3.size(); var19++) {
         Order var20 = var3.get(var19);
         if (var20.Extra1 < var22) {
            var22 = var20.Extra1;
         }
      }

      return this.round2(Math.abs(var22));
   }

   private double getPercentageDD(double var1, double var3) {
      return !(var3 <= 0.0) && !(var1 > var3) ? var1 / (var3 / 100.0) : -1.0;
   }

   private static double specialSubtraction(double var0, double var2) {
      double var4 = var0 - var2;
      if (var4 < 0.0) {
         var4 = 0.0;
      }

      return -1.0 * var4;
   }
}
