package SQ.Columns.Databanks;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class Efficiency extends DatabankColumn {
   public Efficiency() {
      super("Efficiency", "Decimal2Pct", (byte)1, 0.0, -100.0, 100.0);
      this.setWidth(80);
      this.setTooltip("Efficiency of trading strategy. Net profit divided by Gross profit");
      this.setDependencies(new String[]{"GrossProfit", "GrossLoss"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("GrossProfit");
      double var9 = var1.getDouble("GrossLoss");
      return SQUtils.round2(SQUtils.safeDivide(var7 - var9, var7) * 100.0);
   }
}
