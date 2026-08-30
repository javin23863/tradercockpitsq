package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;

public class Parameters extends DatabankColumn {
   public Parameters() {
      super(L.tsq("Parameters"), "Text", (byte)1, 0.0, 0.0, 1.0);
      this.setWidth(200);
   }

   public double getNumericValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return 0.0;
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      try {
         WalkForwardMatrixResult var6 = (WalkForwardMatrixResult)var1.mainResult().get("WalkForwardResult");
         WalkForwardResult var7 = var6.getWFResult(var1.getBestWFResultKey(), true);
         return var7.testParams;
      } catch (Exception var8) {
         return var1.getParams();
      }
   }
}
