package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ReturnOpenDDRatio extends DatabankColumn {
   public ReturnOpenDDRatio() {
      super(L.tsq("Ret/OpenDD Ratio"), "Decimal2", (byte)1, 0.0, -20.0, 20.0);
      this.setDependencies(new String[]{"NetProfit", "OpenDrawdown", "NumberOfTrades"});
      this.setTooltip(L.tsq("Return / Open Drawdown Ratio"));
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("NetProfit");
      double var9 = Math.abs(var1.getDouble("OpenDrawdown"));
      int var11 = var1.getInt("NumberOfTrades");
      if (var11 == 0) {
         return 0.0;
      } else if (var9 == 0.0) {
         return var7 == 0.0 ? 0.0 : 10.0;
      } else {
         return this.round2(this.safeDivide(var7, var9));
      }
   }
}
