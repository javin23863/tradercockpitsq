package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;

public class InitialDeposit extends DatabankColumn {
   public InitialDeposit() {
      super(L.tsq("Initial deposit"), "Decimal2", (byte)1, 0.0, 0.0, 1.0);
      this.setWidth(100);
   }

   public double getNumericValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      Result var6 = var1.subResult("Portfolio");
      return var6.getSettings().getDouble("MoneyManagement.InitialCapital", 0.0);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      Result var6 = var1.subResult("Portfolio");
      return Double.toString(var6.getSettings().getDouble("MoneyManagement.InitialCapital", 0.0));
   }
}
