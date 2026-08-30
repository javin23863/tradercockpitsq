package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgTradesPerMonth extends DatabankColumn {
   public AvgTradesPerMonth() {
      super(L.tsq("Avg. Trades Per Month"), "Decimal2", (byte)2, 0.0, 0.0, 1000.0);
      this.setDependencies(new String[]{"NumberOfTrades", "TotalDataMonths"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = var1.getInt("NumberOfTrades");
      int var8 = var1.getInt("TotalDataMonths");
      return this.round2(this.safeDivide(var7, var8));
   }
}
