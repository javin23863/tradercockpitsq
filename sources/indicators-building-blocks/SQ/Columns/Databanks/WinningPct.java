package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class WinningPct extends DatabankColumn {
   public WinningPct() {
      super(L.tsq("Winning Percent"), "Decimal2Pct", (byte)1, 0.0, -5.0, 5.0);
      this.setDependencies(new String[]{"NumberOfProfits", "NumberOfLosses"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("NumberOfProfits");
      int var9 = var1.getInt("NumberOfLosses");
      return SQUtils.round2(SQUtils.safeDivide(var7, var7 + var9) * 100.0);
   }
}
