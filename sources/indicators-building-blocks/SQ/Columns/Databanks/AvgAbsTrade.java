package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgAbsTrade extends DatabankColumn {
   public AvgAbsTrade() {
      super(L.tsq("Avg. Abs Trade"), "Decimal2PL", (byte)1, 0.0, 0.0, 200.0);
      this.setDependencies(new String[]{"NumberOfTrades"});
      this.setTooltip(L.tsq("Average Absolute Trade (including losing trades)"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = 0;

      for (int var8 = 0; var8 < var3.size(); var8++) {
         Order var9 = var3.get(var8);
         if (!var9.isBalanceOrder()) {
            double var10 = this.getPLByStatsType(var9, var2);
            var7 = (int)(var7 + Math.abs(var10));
         }
      }

      int var12 = var1.getInt("NumberOfTrades");
      return SQUtils.round(this.safeDivide(var7, var12), 4);
   }
}
