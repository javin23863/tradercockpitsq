package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class AnnualPctReturnDDRatio extends DatabankColumn {
   public AnnualPctReturnDDRatio() {
      super(L.tsq("CAGR/Max DD %"), "Decimal2", (byte)1, 0.0, 0.0, 5.0);
      this.setTooltip(L.tsq("CAGR vs Drawdown Ratio"));
      this.setDependencies(new String[]{"CAGR", "DrawdownPct"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("DrawdownPct");
      double var9 = var1.getDouble("CAGR");
      return this.round2(this.safeDivide(var9, var7));
   }
}
