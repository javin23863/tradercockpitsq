package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;

public class Symbol extends DatabankColumn {
   public Symbol() {
      super("Symbol", "Text", (byte)2, 0.0, 0.0, 10000.0);
      this.setTooltip(L.tsq("Symbol on which the test was made"));
      this.setWidth(150);
      this.printsSpecialValue(true);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return var1.subResult(var2).containsKey(SpecialValues.Symbol)
         ? var1.subResult(var2).getString(SpecialValues.Symbol)
         : var1.specialValues().getString(SpecialValues.Symbol, "N/A");
   }
}
