package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class RecoveryFactor extends DatabankColumn {
   public RecoveryFactor() {
      super(L.tsq("RecoveryFactor"), "Decimal2", (byte)1, 0.0, 0.0, 200.0);
      this.setDependencies(new String[]{"NetProfitInPct", "DrawdownPct"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("NetProfitInPct");
      double var9 = var1.getDouble("DrawdownPct");
      return this.round2(this.safeDivide(var7, var9));
   }
}
