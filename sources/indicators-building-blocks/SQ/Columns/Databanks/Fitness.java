package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;

public class Fitness extends DatabankColumn {
   public Fitness() {
      super(L.tsq("Fitness"), "Decimal2", (byte)1, 0.0, 0.0, 1.0);
      this.setTooltip(L.tsq("Fitness rank"));
      this.setWidth(45);
   }

   public double getNumericValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return var1.getFitness(var5);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      double var6 = this.getNumericValue(var1, var2, var3, var4, var5);
      return Double.toString(SQUtils.round(var6, 2));
   }
}
