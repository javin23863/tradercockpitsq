package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ProfitableMonths extends DatabankColumn {
   public ProfitableMonths() {
      super(L.tsq("Profitable Months"), "Integer", (byte)1, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Profitable Months"));
      this.setDependencies(new String[]{"TotalTradingDays"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      return var1.getInt("ProfitableMonths");
   }
}
