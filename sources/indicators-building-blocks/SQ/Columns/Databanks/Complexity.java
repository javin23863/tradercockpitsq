package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.results.SpecialValues;

public class Complexity extends DatabankColumn {
   public Complexity() {
      super(L.tsq("Complexity"), "Integer", (byte)2, 0.0, 0.0, 100.0);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6, Result var7, SettingsMap var8) throws Exception {
      return var8 != null && var8.containsKey(SpecialValues.Complexity) ? var8.getInt(SpecialValues.Complexity) : SQUtils.round2(0.0);
   }
}
