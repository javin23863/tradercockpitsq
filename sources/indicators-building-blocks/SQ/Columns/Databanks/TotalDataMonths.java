package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class TotalDataMonths extends DatabankColumn {
   public TotalDataMonths() {
      super(L.tsq("Total Data Months"), "Integer", (byte)2, 0.0, 10.0, 1000.0);
      this.setDependencies(new String[]{"TotalDataDays"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      return var1.getDouble("TotalDataMonths");
   }
}
