package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class StagnationPct extends DatabankColumn {
   public StagnationPct() {
      super(L.tsq("% Stagnation"), "Decimal2Pct", (byte)2, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Stagnation in % from total days"));
      this.setDependencies(new String[]{"Stagnation"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      return var1.getDouble("StagnationPct");
   }
}
