package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ExitQuality extends DatabankColumn {
   public ExitQuality() {
      super(L.tsq("Exit quality"), "Decimal4", (byte)1, 0.0, 0.0, 10.0);
      this.setWidth(70);
      this.setDependencies(new String[]{"NetProfit", "TotalMFE"});
      this.setTooltip(L.tsq("ExitQuality = Total Profit / Total MFE"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("NetProfit");
      double var9 = var1.getDouble("TotalMFE");
      double var11 = SQUtils.safeDivide(var7, var9);
      return this.round4(var11);
   }
}
