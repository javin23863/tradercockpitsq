package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquitySlope extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("EquitySlope");

   public EquitySlope() {
      super(L.tsq("EquitySlope"), "Decimal2", (byte)1, 0.0, -90.0, 90.0);
      this.setTooltip(L.tsq("EquitySlope - how straight is the equity curve times how big is its angle"));
      this.setDependencies(new String[]{"RSquared", "EquityAngle"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3 != null && !var3.isEmpty()) {
         double var7 = var1.getDouble("EquityAngle");
         double var9 = var1.getDouble("RSquared");
         double var11 = var7 * var9;
         return this.round2(var11);
      } else {
         return 0.0;
      }
   }
}
