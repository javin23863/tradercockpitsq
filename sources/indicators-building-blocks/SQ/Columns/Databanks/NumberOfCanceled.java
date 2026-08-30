package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class NumberOfCanceled extends DatabankColumn {
   public NumberOfCanceled() {
      super(L.tsq("# of canceled"), "Integer", (byte)3, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Number of canceled trades"));
      this.setWidth(70);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = 0;

      for (int var8 = 0; var8 < var3.size(); var8++) {
         Order var9 = var3.get(var8);
         if (var9.isCanceledOrder() && var9.PL == 0.0F) {
            var7++;
         }
      }

      return var7;
   }
}
