package SQ.Columns.WalkForward;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import java.util.ArrayList;

public class WFPctOfProfitableRuns extends WalkForwardColumn {
   public WFPctOfProfitableRuns() {
      super("WFPctOfProfitableRuns", L.tsq("Percentage of profitable runs"), "Decimal2Pct", (byte)1);
   }

   public double compute(WalkForwardResult var1) {
      ArrayList var2 = var1.wfPeriods;
      int var3 = 0;
      int var4 = var2.size() - 1;

      for (int var5 = 0; var5 < var4; var5++) {
         WalkForwardPeriod var6 = (WalkForwardPeriod)var2.get(var5);
         if (var6.runStatData != null && var6.runStatData.getDouble("NetProfit") > 0.0) {
            var3++;
         }
      }

      float var7 = var3 / (var4 / 100.0F);
      return var7;
   }
}
