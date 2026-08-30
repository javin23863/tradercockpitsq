package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class RExpectancy extends DatabankColumn {
   public RExpectancy() {
      super(L.tsq("R Expectancy"), "Decimal2", (byte)1, 0.0, -5.0, 5.0);
      this.setDependencies(new String[]{"NetProfit", "NumberOfTrades", "AvgLoss"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("AvgLoss");
      double var9 = var1.getDouble("NetProfit");
      double var11 = var1.getDouble("NumberOfTrades");
      double var13 = 0.0;
      if (var11 != 0.0) {
         if (Math.abs(var7) == 0.0) {
            var13 = 99999.0;
         } else {
            var13 = var9 / (var11 * Math.abs(var7));
         }
      }

      return this.round4(var13);
   }
}
