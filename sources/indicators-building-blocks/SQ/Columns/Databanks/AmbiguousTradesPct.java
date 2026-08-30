package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AmbiguousTradesPct extends DatabankColumn {
   public AmbiguousTradesPct() {
      super(L.tsq("Ambiguous Trades %"), "Decimal2Pct", (byte)2, 0.0, 0.0, 30.0);
      this.setTooltip(L.tsq("Ambiguous Trades Percentage - trades that start and end at the same bar"));
      this.setDependencies(new String[]{"AmbiguousTrades"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getInt("AmbiguousTrades");
      double var9 = var7 / var3.size() * 100.0;
      return this.round2(var9);
   }
}
