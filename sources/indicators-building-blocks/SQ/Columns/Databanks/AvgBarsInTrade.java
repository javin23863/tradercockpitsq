package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgBarsInTrade extends DatabankColumn {
   public AvgBarsInTrade() {
      super(L.tsq("Avg. Bars in Trade"), "Decimal2", (byte)2, 0.0, 0.0, 40.0);
      this.setTooltip(L.tsq("Average Bars In Trade"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      short var7 = 0;
      int var8 = 0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (var10.isFilledOrder()) {
            var8++;
            var7 += var10.BarsInTrade;
         }
      }

      return this.round2(SQUtils.safeDivide(var7, var8));
   }
}
