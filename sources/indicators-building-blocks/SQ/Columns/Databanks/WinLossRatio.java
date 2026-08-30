package SQ.Columns.Databanks;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class WinLossRatio extends DatabankColumn {
   public WinLossRatio() {
      super("Win/Loss ratio", "Decimal2", (byte)1, 0.0, -5.0, 5.0);
      this.setDependencies(new String[]{"NumberOfProfits", "NumberOfLosses"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      int var7 = var1.getInt("NumberOfProfits");
      int var8 = var1.getInt("NumberOfLosses");
      double var9 = 0.0;
      if (var7 > 0 && var8 == 0) {
         var9 = 99999.0;
      } else {
         var9 = SQUtils.safeDivide(var7, var8);
      }

      return SQUtils.round2(var9);
   }
}
