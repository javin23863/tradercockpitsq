package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AvgTrade extends DatabankColumn {
   public AvgTrade() {
      super(L.tsq("Avg. Trade"), "Decimal2PL", (byte)1, 0.0, 0.0, 200.0);
      this.setDependencies(new String[]{"NumberOfTrades", "NetProfit"});
      this.setTooltip(L.tsq("Average Trade"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("NetProfit");
      int var9 = var1.getInt("NumberOfTrades");
      return SQUtils.round(this.safeDivide(var7, var9), 4);
   }
}
