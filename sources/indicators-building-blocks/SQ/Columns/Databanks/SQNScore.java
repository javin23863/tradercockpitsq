package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class SQNScore extends DatabankColumn {
   public SQNScore() {
      super(L.tsq("SQN Score"), "Decimal2", (byte)1, 0.0, -1.0, 1.0);
      this.setTooltip(L.tsq("Strategy Quality Number Score"));
      this.setDependencies(new String[]{"SQN"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      return var1.getDouble("SQNScore");
   }
}
