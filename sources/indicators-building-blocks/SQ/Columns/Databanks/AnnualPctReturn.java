package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AnnualPctReturn extends DatabankColumn {
   public AnnualPctReturn() {
      super(L.tsq("Annual % Return"), "Decimal2Pct", (byte)1, 0.0, 0.0, 50.0);
      this.setTooltip(L.tsq("Annual Percentage Return"));
      this.setDependencies(new String[]{"TotalDataYears"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = var1.getInt("TotalDataYears");
      double var8 = 0.0;

      for (int var10 = 0; var10 < var3.size(); var10++) {
         Order var11 = var3.get(var10);
         var8 += var11.PctPL;
      }

      return this.round2(this.safeDivide(var8, var7));
   }
}
