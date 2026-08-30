package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgConsecWins extends DatabankColumn {
   public AvgConsecWins() {
      super(L.tsq("Avg Consec. Wins"), "Decimal2", (byte)1, 0.0, 0.0, 20.0);
      this.setTooltip(L.tsq("Average Consecutive Wins"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = 0;
      int var8 = 0;
      int var9 = 0;
      byte var10 = 0;

      for (int var11 = 0; var11 < var3.size(); var11++) {
         Order var12 = var3.get(var11);
         if (var12.PL > 0.0F) {
            var9++;
            var10 = 1;
         } else {
            if (var10 > 0) {
               var7 += var9;
               var8++;
               var9 = 0;
            }

            var10 = -1;
         }
      }

      if (var9 > 0) {
         var7 += var9;
         var8++;
      }

      return this.round2(this.safeDivide(var7, var8));
   }
}
