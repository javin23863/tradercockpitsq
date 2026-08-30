package SQ.Columns.Databanks;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ActualDD extends DatabankColumn {
   public ActualDD() {
      super("Actual Drawdown", "Decimal2PL", (byte)2, 0.0, 0.0, 100.0);
      this.setWidth(80);
      this.setTooltip("Actual Drawdown - drawdown during very last trade");
      this.setDependencies(new String[]{"Drawdown"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3.size() > 0) {
         Order var7 = var3.get(var3.size() - 1);
         double var8 = var7.DD;
         return this.round2(Math.abs(var8));
      } else {
         return 0.0;
      }
   }
}
