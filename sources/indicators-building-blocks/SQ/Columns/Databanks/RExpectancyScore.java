package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class RExpectancyScore extends DatabankColumn {
   public RExpectancyScore() {
      super(L.tsq("R Expectancy Score"), "Decimal2", (byte)1, 0.0, -5.0, 5.0);
      this.setDependencies(new String[]{"NumberOfTrades", "RExpectancy", "AvgTradesPerMonth", "AvgLoss"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("RExpectancy");
      double var9 = var1.getDouble("AvgTradesPerMonth");
      double var11 = var1.getDouble("AvgLoss");
      double var13 = var1.getDouble("NumberOfTrades");
      double var15 = 0.0;
      if (var13 != 0.0 && Math.abs(var11) != 0.0) {
         var15 = var7 * var9 * 12.0;
      }

      return this.round2(var15);
   }
}
