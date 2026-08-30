package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class TotalTradingMonths extends DatabankColumn {
   public TotalTradingMonths() {
      super(L.tsq("Total Trading Months"), "Integer", (byte)2, 0.0, 10.0, 1000.0);
      this.setDependencies(new String[]{"TotalTradingDays"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      return var1.getDouble("TotalTradingMonths");
   }
}
