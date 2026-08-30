package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class MaxTSIntradayDrawdown extends DatabankColumn {
   private static final long DAY_MILLIS = 86400000L;

   public MaxTSIntradayDrawdown() {
      super(L.tsq("Max TS Intraday Drawdown"), "Decimal2PL", (byte)1, 0.0, -10000.0, 10000.0);
      this.setDependencies(new String[]{"MaxIntradayDrawdown"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6, Result var7, SettingsMap var8) throws Exception {
      if (var3.size() == 0) {
         return 0.0;
      }

      double var9 = Double.MAX_VALUE;
      double var11 = 0.0;

      for (int var13 = 0; var13 < var3.size(); var13++) {
         Order var14 = var3.get(var13);
         double var15 = var11 - var14.MAE;
         if (var15 < var9) {
            var9 = var15;
         }

         var11 = var14.DD;
      }

      int var17 = var1.getInt("MaxIntradayDrawdown");
      return -Math.max(Math.abs(var9), var17);
   }
}
