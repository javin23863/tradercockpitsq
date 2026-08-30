package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class MaxConsecWins extends DatabankColumn {
   public MaxConsecWins() {
      super(L.tsq("Max Consec. Wins"), "Integer", (byte)1, 0.0, 0.0, 20.0);
      this.setTooltip(L.tsq("Maximum Consecutive Wins"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = 0;
      int var8 = 0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (var10.isRealOrder()) {
            if (var10.PL > 0.0F) {
               var8 = Math.max(++var7, var8);
            } else {
               var7 = 0;
            }
         }
      }

      return var8;
   }
}
