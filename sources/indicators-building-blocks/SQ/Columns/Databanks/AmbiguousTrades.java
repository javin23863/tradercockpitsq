package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AmbiguousTrades extends DatabankColumn {
   public AmbiguousTrades() {
      super(L.tsq("Ambiguous Trades"), "Integer", (byte)2, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Ambiguous Trades - trades that start and end at the same bar"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (!var10.isPendingOrder() && var10.BarsInTrade == 0) {
            var7++;
         }
      }

      return this.round2(var7);
   }
}
