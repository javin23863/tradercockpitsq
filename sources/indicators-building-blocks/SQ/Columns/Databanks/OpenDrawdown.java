package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class OpenDrawdown extends DatabankColumn {
   public OpenDrawdown() {
      super(L.tsq("Open Drawdown"), "Decimal2PL", (byte)2, 0.0, 0.0, 10000.0);
      this.setDependentOnTradingPeriod(true);
      this.setTooltip(L.tsq("Drawdown computed from open balance + MAE"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3.size() == 0) {
         return 0.0;
      }

      double var7 = var4.getDouble("MoneyManagement.InitialCapital", 10000.0);
      double var9 = 0.0;
      double var11 = var7;
      double var13 = var7;

      for (int var15 = 0; var15 < var3.size(); var15++) {
         Order var16 = var3.get(var15);
         var9 = var11 - var16.MAE;
         var11 += var16.PL;
         var16.Extra1 = (float)specialSubtraction(var13, var9);
         if (var11 > var13) {
            var13 = var11;
         }
      }

      double var20 = 9.9999999E7;

      for (int var17 = 0; var17 < var3.size(); var17++) {
         Order var18 = var3.get(var17);
         if (var18.Extra1 < var20) {
            var20 = var18.Extra1;
         }
      }

      return this.round2(Math.abs(var20));
   }

   private static double specialSubtraction(double var0, double var2) {
      double var4 = var0 - var2;
      if (var4 < 0.0) {
         var4 = 0.0;
      }

      return -1.0 * var4;
   }
}
