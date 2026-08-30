package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class MaxLoss extends DatabankColumn {
   public MaxLoss() {
      super(L.tsq("Max Loss"), "Decimal2PL", (byte)2, 0.0, -10000.0, 10000.0);
      this.setDependencies(new String[]{"NumberOfProfits"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         double var11 = this.getPLByStatsType(var10, var2);
         if (var11 <= 0.0 && var11 < var7) {
            var7 = var11;
         }
      }

      return SQUtils.round2(var7);
   }
}
