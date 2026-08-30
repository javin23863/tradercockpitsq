package SQ.Columns.WalkForward;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import java.util.ArrayList;

public class WFMinTradesInRun extends WalkForwardColumn {
   public WFMinTradesInRun() {
      super("WFMinTradesInRun", L.tsq("Min trades in one run"), "Integer", (byte)1);
   }

   public double compute(WalkForwardResult var1) {
      ArrayList var2 = var1.wfPeriods;
      if (!var2.isEmpty() && ((WalkForwardPeriod)var2.get(0)).runStatData != null) {
         double var3 = ((WalkForwardPeriod)var2.get(0)).runStatData.getDouble("NumberOfTrades");

         for (int var5 = 0; var5 < var2.size(); var5++) {
            WalkForwardPeriod var6 = (WalkForwardPeriod)var2.get(var5);
            if (var6.runStatData != null && var6.runStatData.getDouble("NumberOfTrades") < var3) {
               var3 = var6.runStatData.getDouble("NumberOfTrades");
            }
         }

         return var3;
      } else {
         return 0.0;
      }
   }
}
