package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;

public class TimeFrame extends DatabankColumn {
   public TimeFrame() {
      super(L.tsq("TimeFrame"), "Text", (byte)1, 0.0, 0.0, 100.0);
      this.setTooltip(L.tsq("Timeframe on which the test was made"));
      this.setWidth(100);
      this.printsSpecialValue(true);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return var1.subResult(var2).containsKey(SpecialValues.Timeframe)
         ? var1.subResult(var2).getString(SpecialValues.Timeframe)
         : var1.specialValues().getString(SpecialValues.Timeframe, "N/A");
   }
}
