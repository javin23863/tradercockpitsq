package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class LongestTrade extends DatabankColumn {
   public LongestTrade() {
      super(L.tsq("Longest trade (days)"), "Integer", (byte)2, 0.0, 0.0, 100.0);
      this.setWidth(70);
      this.setTooltip(L.tsq("Duration of longest trade in days"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      long var7 = -1L;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (var10.isFilledOrder() && var10.isRealOrder() && var10.Duration > var7) {
            var7 = var10.Duration;
         }
      }

      return var7 / 86400L;
   }
}
