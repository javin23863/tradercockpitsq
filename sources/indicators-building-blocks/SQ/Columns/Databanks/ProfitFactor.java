package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ProfitFactor extends DatabankColumn {
   public ProfitFactor() {
      super(L.tsq("Profit factor"), "Decimal2", (byte)1, 0.0, 0.0, 10.0);
      this.setDependencies(new String[]{"NetProfit", "GrossProfit", "GrossLoss", "NumberOfTrades"});
      this.setTooltip(L.tsq("Profit Factor - Ratio of gross profit to gross loss."));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("NetProfit");
      double var9 = var1.getDouble("GrossProfit");
      double var11 = var1.getDouble("GrossLoss");
      int var13 = var1.getInt("NumberOfTrades");
      if (var13 == 0) {
         return 0.0;
      }

      double var14 = 0.0;
      if (var11 == 0.0) {
         if (var7 == 0.0) {
            return 0.0;
         }

         var14 = 5.0;
      } else {
         var14 = var9 / var11;
      }

      return this.round2(var14);
   }
}
