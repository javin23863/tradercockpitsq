package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class DrawdownPips extends DatabankColumn {
   public DrawdownPips() {
      super(L.tsq("Max DD pips"), "Decimal2Pips", (byte)2, 0.0, 0.0, 1000.0);
      this.setTooltip(L.tsq("Max Drawdown in pips"));
      this.setDependentOnTradingPeriod(true);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;
      double var9 = 0.0;

      for (int var11 = 0; var11 < var3.size(); var11++) {
         Order var12 = var3.get(var11);
         var9 += var12.PipsPL;
         var12.PipsDD = (float)this.specialSubtraction(var7, var9);
         if (var9 > var7) {
            var7 = var9;
         }
      }

      double var15 = 9.9999999E7;

      for (int var13 = 0; var13 < var3.size(); var13++) {
         Order var14 = var3.get(var13);
         var15 = Math.min(var15, var14.PipsDD);
      }

      return var3.size() == 0 ? 0.0 : this.round2(Math.abs(var15));
   }

   private double specialSubtraction(double var1, double var3) {
      double var5 = var1 - var3;
      if (var5 < 0.0) {
         var5 = 0.0;
      }

      return -1.0 * var5;
   }
}
