package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;

public class ResultsName extends DatabankColumn {
   public ResultsName() {
      super(L.tsq("Results Name"), "Text", (byte)1, 0.0, 0.0, 0.0);
      this.setWidth(170);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return var1.getName();
   }
}
