package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgTradesPerYear extends DatabankColumn {
   public AvgTradesPerYear() {
      super(L.tsq("Avg. Trades Per Year"), "Decimal2", (byte)2, 0.0, 0.0, 2000.0);
      this.setDependencies(new String[]{"NumberOfTrades", "TotalDataYears"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = var1.getInt("NumberOfTrades");
      int var8 = var1.getInt("TotalDataYears");
      return this.round2(this.safeDivide(var7, var8));
   }
}
