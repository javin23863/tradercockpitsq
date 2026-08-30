package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class EdgeRatioInPips extends DatabankColumn {
   public EdgeRatioInPips() {
      super(L.t("Edge Ratio", new Object[0]), "Decimal2", (byte)1, 0.0, 0.0, 50.0);
      this.setDependencies(new String[]{"NumberOfTrades"});
      this.setTooltip(L.t("Edge Ratio", new Object[0]));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = 0.0;
      double var9 = 0.0;

      for (int var11 = 0; var11 < var3.size(); var11++) {
         Order var12 = var3.get(var11);
         if (!var12.isBalanceOrder() && var12.isRealOrder()) {
            var7 += var12.PipsMAE / var12.ATROnOpen;
            var9 += var12.PipsMFE / var12.ATROnOpen;
         }
      }

      int var16 = var1.getInt("NumberOfTrades");
      double var17 = var7 / var16;
      double var14 = var9 / var16;
      return this.round2(this.safeDivide(var14, var17));
   }
}
