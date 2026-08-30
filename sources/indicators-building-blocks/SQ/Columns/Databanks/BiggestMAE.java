package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class BiggestMAE extends DatabankColumn {
   public BiggestMAE() {
      super(L.tsq("Biggest MAE"), "Decimal2PL", (byte)1, 0.0, 35000.0, 0.0);
      this.setWidth(70);
      this.setTooltip(L.tsq("Biggest MAE - is the worst Maximum Adverse Excursion of all trades"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = -Double.MAX_VALUE;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (!var10.isPendingOrder()) {
            double var11 = this.getMAEByStatsType(var10, var2);
            if (var11 > var7) {
               var7 = var11;
            }
         }
      }

      return -1.0 * var7;
   }

   private double getMAEByStatsType(Order var1, StatsTypeCombination var2) {
      if (var2.getPLType() == 10) {
         return var1.MAE;
      } else if (var2.getPLType() == 20) {
         return SQUtils.safeDivide(var1.MAE, var1.AccountBalance - var1.PL) * 100.0;
      } else {
         return var2.getPLType() == 30 ? var1.PipsMAE : 0.0;
      }
   }
}
