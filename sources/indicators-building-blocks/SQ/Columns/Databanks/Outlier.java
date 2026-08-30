package SQ.Columns.Databanks;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class Outlier extends DatabankColumn {
   public Outlier() {
      super("Outlier1", "Decimal2", (byte)1, 0.0, 0.0, 100.0);
      this.setWidth(80);
      this.setTooltip("Outlier Without filtering Trade with Same Profit Loss");
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;
      double var9 = 0.0;
      double var11 = 0.0;
      int var13 = 0;
      double var14 = 0.0;

      for (int var16 = 0; var16 < var3.size(); var16++) {
         Order var17 = var3.get(var16);
         if (!var17.isBalanceOrder()) {
            double var18 = this.getPLByStatsType(var17, var2);
            if (var18 > var7) {
               var11 = var9;
               var9 = var7;
               var7 = var18;
            } else if (var18 > var9) {
               var11 = var9;
               var9 = var18;
            } else if (var18 > var11) {
               var11 = var18;
            }

            var13++;
         }
      }

      if (var13 > 2 && var9 + var11 != 0.0) {
         var14 = var7 / (var9 + var11);
      }

      return this.round2(var14);
   }
}
