package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class CAGR extends DatabankColumn {
   public CAGR() {
      super(L.tsq("CAGR"), "Decimal2Pct", (byte)1, 0.0, 0.0, 50.0);
      this.setDependencies(new String[]{"TotalDataYears"});
      this.setTooltip(L.tsq("Compound Annual Growth Rate"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (!var10.isBalanceOrder()) {
            var7 += var10.PL;
         }
      }

      int var18 = var1.getInt("TotalDataYears");
      double var19 = var4.getDouble("MoneyManagement.InitialCapital");
      if (var18 > 0 && !(var19 <= 0.0)) {
         double var12 = (var19 + var7) / var19;
         double var14 = this.safeDivide(1.0, var18);
         double var16 = (Math.pow(var12, var14) - 1.0) * 100.0;
         return this.round2(var16);
      } else {
         return 0.0;
      }
   }
}
