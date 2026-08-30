package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class StandardDev extends DatabankColumn {
   public StandardDev() {
      super("StandardDev", "Decimal2", (byte)2, 0.0, -1.0, 1.0);
      this.setTooltip(L.tsq("Standard Deviation of Trades"));
      this.setDependencies(new String[]{"AvgTrade"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("AvgTrade");
      double var9 = this.computeStdev(var7, var3, var2);
      return this.round2(var9);
   }

   public double computeStdev(double var1, OrdersList var3, StatsTypeCombination var4) {
      if (var3.size() <= 0) {
         return 0.0;
      }

      double var5 = 0.0;
      byte var11 = 0;
      int var12 = var3.size();

      for (int var13 = var11; var13 < var12; var13++) {
         Order var14 = var3.get(var13);
         double var9 = this.getPLByStatsType(var14, var4);
         var5 += Math.pow(1.0 * var9 - var1, 2.0);
      }

      return Math.sqrt(var5 / (var12 - var11));
   }
}
