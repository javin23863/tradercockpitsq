package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class TotalMFE extends DatabankColumn {
   public TotalMFE() {
      super(L.tsq("Total MFE"), "Decimal2PL", (byte)2, 0.0, 0.0, 100.0);
      this.setWidth(70);
      this.setTooltip(L.tsq("Total MFE - sum of MFE (Maximum Favorable Excursion) of all trades"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (!var10.isPendingOrder()) {
            var7 += var10.MFE;
         }
      }

      return var7;
   }
}
