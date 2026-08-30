package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgBarsWin extends DatabankColumn {
   public AvgBarsWin() {
      super(L.tsq("Avg. Bars Win"), "Decimal2", (byte)1, 0.0, 0.0, 40.0);
      this.setDependencies(new String[]{"NumberOfProfits"});
      this.setTooltip(L.tsq("Average Bars In Trade for Winner"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      short var7 = 0;

      for (int var8 = 0; var8 < var3.size(); var8++) {
         Order var9 = var3.get(var8);
         if (var9.PL > 0.0F) {
            var7 += var9.BarsInTrade;
         }
      }

      int var10 = var1.getInt("NumberOfProfits");
      return SQUtils.round2(SQUtils.safeDivide(var7, var10));
   }
}
