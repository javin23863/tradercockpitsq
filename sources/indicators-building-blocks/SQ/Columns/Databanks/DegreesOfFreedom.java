package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class DegreesOfFreedom extends DatabankColumn {
   public DegreesOfFreedom() {
      super(L.tsq("Degrees of freedom"), "Integer", (byte)2, 0.0, 0.0, 100.0);
      this.setDependencies(new String[]{"NumberOfTrades", "Complexity"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = var1.getInt("Complexity");
      int var8 = var1.getInt("NumberOfTrades");
      int var9 = var8 - var7;
      if (var9 < 0) {
         var9 = 0;
      }

      return var9;
   }
}
