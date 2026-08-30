package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class UlcerIndex extends DatabankColumn {
   public UlcerIndex() {
      super(L.tsq("Ulcer Index %"), "Decimal2Pct", (byte)2, 0.0, 0.0, 100.0);
      this.setDependencies(new String[]{"NumberOfTrades"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (!var10.isBalanceOrder()) {
            double var11 = var10.PctDD;
            var7 += Math.pow(var11, 2.0);
         }
      }

      int var13 = var1.getInt("NumberOfTrades");
      return this.round2(this.safeDivide(Math.sqrt(var7), var13));
   }
}
