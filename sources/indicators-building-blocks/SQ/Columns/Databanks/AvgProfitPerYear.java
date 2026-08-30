package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgProfitPerYear extends DatabankColumn {
   public AvgProfitPerYear() {
      super(L.tsq("Avg. Profit Per Year"), "Decimal2PL", (byte)1, 0.0, 0.0, 200.0);
      this.setDependencies(new String[]{"TotalDataYears", "NetProfit"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = var1.getInt("NetProfit");
      int var8 = var1.getInt("TotalDataYears");
      return this.round2(this.safeDivide(var7, var8));
   }
}
