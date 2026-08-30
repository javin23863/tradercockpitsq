package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class Commission extends DatabankColumn {
   public Commission() {
      super(L.tsq("Commission/Swap ($)"), "Decimal2PL", (byte)2, 0.0, 0.0, 5.0);
      this.setDependentOnTradingPeriod(true);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         var7 += var10.CommSwap;
      }

      return SQUtils.round2(var7);
   }
}
