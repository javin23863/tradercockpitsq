package SQ.Columns.Databanks;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class TSWinLossRatio extends DatabankColumn {
   public TSWinLossRatio() {
      super("TS Win/Loss ratio", "Decimal2", (byte)1, 0.0, -5.0, 5.0);
      this.setDependencies(new String[]{"AvgWin", "AvgLoss"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getInt("AvgWin");
      double var9 = var1.getInt("AvgLoss");
      double var11 = SQUtils.safeDivide(var7, var9);
      return SQUtils.round(var11, 4);
   }
}
