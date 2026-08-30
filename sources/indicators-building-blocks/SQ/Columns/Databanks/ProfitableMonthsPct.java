package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ProfitableMonthsPct extends DatabankColumn {
   public ProfitableMonthsPct() {
      super(L.tsq("% Profitable Months"), "Decimal2Pct", (byte)1, 0.0, 0.0, 100.0);
      this.setDependencies(new String[]{"ProfitableMonths", "TotalTradingMonths"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = var1.getInt("ProfitableMonths");
      int var8 = var1.getInt("TotalTradingMonths");
      return SQUtils.round2(SQUtils.safeDivide(var7, var8) * 100.0);
   }
}
