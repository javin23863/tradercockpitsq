package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgWin extends DatabankColumn {
   public AvgWin() {
      super(L.tsq("Avg. Win"), "Decimal2PL", (byte)1, 0.0, 0.0, 200.0);
      this.setDependencies(new String[]{"NumberOfProfits", "GrossProfit"});
      this.setTooltip(L.tsq("Average Winning Trade"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("GrossProfit");
      int var9 = var1.getInt("NumberOfProfits");
      return this.round2(this.safeDivide(var7, var9));
   }
}
