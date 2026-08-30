package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EquityAngle extends DatabankColumn {
   public static final Logger Log = LoggerFactory.getLogger("EquitySlope");

   public EquityAngle() {
      super(L.tsq("EquityAngle"), "Decimal2", (byte)1, 0.0, -90.0, 90.0);
      this.setTooltip(L.tsq("Angle of the equity"));
      this.setDependencies(new String[]{"RSquared", "NetProfit"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var3 != null && !var3.isEmpty()) {
         double var7 = var1.getDouble("DataLength");
         double var9 = var1.getDouble("NetProfit");
         double var11 = Math.atan2(var9, var7);
         double var13 = Math.toDegrees(var11);
         return this.round2(var13);
      } else {
         return 0.0;
      }
   }
}
