package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class GrossLoss extends DatabankColumn {
   public GrossLoss() {
      super(L.tsq("Gross loss"), "Decimal2PL", (byte)2, 0.0, -10000.0, 10000.0);
      this.setWidth(100);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (!var10.isBalanceOrder()) {
            double var11 = this.getPLByStatsType(var10, var2);
            if (var10.PL < 0.0F) {
               var7 += Math.abs(var11);
            }
         }
      }

      return this.round2(var7);
   }
}
