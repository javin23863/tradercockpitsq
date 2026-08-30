package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

public class MaxIntradayDrawdown extends DatabankColumn {
   private static final long DAY_MILLIS = 86400000L;

   public MaxIntradayDrawdown() {
      super(L.tsq("Max Intraday Drawdown"), "Decimal2PL", (byte)2, 0.0, -10000.0, 10000.0);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6, Result var7, SettingsMap var8) throws Exception {
      double var9 = 0.0;
      if (var7 != null) {
         Long2FloatRBTreeMap var11 = var7.getWorstDailyEquity();
         if (var11 != null && !var11.isEmpty()) {
            Long2FloatRBTreeMap var12 = new Long2FloatRBTreeMap();
            long var13 = 0L;
            long var15 = 0L;

            for (int var17 = 0; var17 < var3.size(); var17++) {
               Order var18 = var3.get(var17);
               long var19 = var18.CloseTime - var18.CloseTime % 86400000L;
               var15 = Math.max(var15, var19);
               var13 = var13 == 0L ? var19 : Math.min(var13, var19);
               var12.addTo(var19, var18.PL);
            }

            double var24 = 0.0;

            for (long var25 = var13; var25 <= var15; var25 += 86400000L) {
               if (var11.containsKey(var25)) {
                  float var21 = var11.get(var25);
                  if (var21 != Float.MAX_VALUE) {
                     double var22 = var24 - var21;
                     var9 = Math.max(var9, var22);
                  }
               }

               if (var12.containsKey(var25)) {
                  var24 += var12.get(var25);
               }
            }
         }
      }

      return var9;
   }
}
