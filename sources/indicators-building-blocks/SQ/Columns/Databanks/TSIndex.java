package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSIndex extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("TSIndex");

   public TSIndex() {
      super(L.tsq("TS Index"), "Decimal2", (byte)1, 0.0, 0.0, 1.0);
      this.setTooltip(L.tsq("TS Index - Tradestation index"));
      this.setPLTypeRestrictions(new byte[]{10});
      this.setDependencies(new String[]{"NetProfit", "NumberOfProfits", "MaxIntradayDrawdown"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3 != null && !var3.isEmpty()) {
         double var7 = var1.getDouble("NetProfit");
         double var9 = var1.getDouble("NumberOfProfits");
         double var11 = var1.getDouble("MaxIntradayDrawdown");
         return this.round2(var7 * var9 / Math.abs(var11));
      } else {
         return 0.0;
      }
   }
}
