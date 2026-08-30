package SQ.Columns.WalkForward;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import java.util.ArrayList;

public class WFMaxProfitByRunInPct extends WalkForwardColumn {
   public WFMaxProfitByRunInPct() {
      super("WFMaxProfitByRunInPct", L.tsq("Max profit in one run as % of total"), "Decimal2Pct", (byte)1);
   }

   public double compute(WalkForwardResult var1) {
      ArrayList var2 = var1.wfPeriods;
      if (!var2.isEmpty() && ((WalkForwardPeriod)var2.get(0)).runStatData != null) {
         double var3 = ((WalkForwardPeriod)var2.get(0)).runStatData.getDouble("NetProfit");
         double var5 = 0.0;

         for (int var7 = 0; var7 < var2.size(); var7++) {
            WalkForwardPeriod var8 = (WalkForwardPeriod)var2.get(var7);
            if (var8.runStatData != null) {
               double var9 = var8.runStatData.getDouble("NetProfit");
               var5 += var9;
               if (var9 > var3) {
                  var3 = var8.runStatData.getDouble("NetProfit");
               }
            }
         }

         double var11 = 0.0;
         if (var3 > 0.0 && var5 < 0.0) {
            var11 = 100.0;
         } else {
            var11 = var3 / (var5 / 100.0);
         }

         return var11;
      } else {
         return 0.0;
      }
   }
}
