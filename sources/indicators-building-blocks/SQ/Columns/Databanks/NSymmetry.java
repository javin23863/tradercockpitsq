package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class NSymmetry extends DatabankColumn {
   public NSymmetry() {
      super(L.tsq("NSymmetry"), "Decimal2Pct", (byte)1, 0.0, 0.0, 100.0);
      this.setTooltip(
         L.tsq(
            "How symmetrical is profit between Long and Short side? The difference of NSymmetry from Symmetry is that if one side is in profit and other in loss it returns -1 instead of 0."
         )
      );
      this.setDependencies(new String[]{"NetProfit"});
      this.setDirectionRestrictions(new byte[]{0});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      if (var2.getDirection() == 0 && var5 != null && var6 != null) {
         double var7 = var5.getDouble("NetProfit");
         double var9 = var6.getDouble("NetProfit");
         double var11 = this.computeSymmetry(var7, var9);
         return this.round2(var11);
      } else {
         return 0.0;
      }
   }

   private double computeSymmetry(double var1, double var3) {
      if (var1 == 0.0 || var3 == 0.0) {
         return 0.0;
      }

      if (var1 == var3) {
         return 1.0;
      }

      double var5 = 0.0;
      double var7 = 0.0;
      if ((!(var1 < 0.0) || !(var3 < 0.0)) && (!(var1 > 0.0) || !(var3 > 0.0))) {
         return -1.0;
      }

      var5 = Math.max(Math.abs(var1), Math.abs(var3));
      var7 = Math.min(Math.abs(var1), Math.abs(var3));
      return var7 / (var5 / 100.0);
   }
}
