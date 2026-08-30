package SQ.Columns.Databanks;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class KellyFormula extends DatabankColumn {
   public KellyFormula() {
      super("Kelly formula", "Decimal2Pct", (byte)1, 0.0, -100.0, 100.0);
      this.setWidth(80);
      this.setTooltip("Kelly formula");
      this.setDependencies(new String[]{"WinningPct", "Efficiency"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("WinningPct") / 100.0;
      double var9 = var1.getDouble("Efficiency") / 100.0;
      return SQUtils.round2(var7 * var9 * 100.0);
   }
}
