package SQ.Columns.WalkForward;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import java.util.ArrayList;

public class WFMaxPctDDbyRun extends WalkForwardColumn {
   public WFMaxPctDDbyRun() {
      super("WFMaxPctDDbyRun", L.tsq("Max % Drawdown in one run"), "Decimal2Pct", (byte)2);
   }

   public double compute(WalkForwardResult var1) {
      ArrayList var2 = var1.wfPeriods;
      if (!var2.isEmpty() && ((WalkForwardPeriod)var2.get(0)).runStatData != null) {
         double var3 = ((WalkForwardPeriod)var2.get(0)).runStatData.getDouble("DrawdownPct");

         for (int var5 = 0; var5 < var2.size(); var5++) {
            WalkForwardPeriod var6 = (WalkForwardPeriod)var2.get(var5);
            if (var6.runStatData != null && var6.runStatData.getDouble("DrawdownPct") > var3) {
               var3 = var6.runStatData.getDouble("DrawdownPct");
            }
         }

         return var3;
      } else {
         return 0.0;
      }
   }
}
