package SQ.Columns.Databanks;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgTrStddevRatio extends DatabankColumn {
   public AvgTrStddevRatio() {
      super("Avg. trade / StdDev ratio", "Decimal2", (byte)1, 0.0, -100.0, 100.0);
      this.setWidth(80);
      this.setTooltip("Avg. trade / StdDev ratio");
      this.setDependencies(new String[]{"AvgTrade", "StandardDev"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("AvgTrade") / 100.0;
      double var9 = var1.getDouble("StandardDev") / 100.0;
      return SQUtils.round2(SQUtils.safeDivide(var7, var9));
   }
}
