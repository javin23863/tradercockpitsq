package SQ.Columns.Databanks;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ActualDrawdownPct extends DatabankColumn {
   public ActualDrawdownPct() {
      super("Actual Drawdown / Max DD", "Decimal2Pct", (byte)2, 0.0, 0.0, 100.0);
      this.setWidth(80);
      this.setTooltip("Actual Drawdown / Max DD");
      this.setDependencies(new String[]{"Drawdown"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("Drawdown");
      if (var3.size() > 0) {
         Order var9 = var3.get(var3.size() - 1);
         double var10 = this.safeDivide(var9.DD, -var7) * 100.0;
         return this.round2(var10);
      } else {
         return 0.0;
      }
   }
}
