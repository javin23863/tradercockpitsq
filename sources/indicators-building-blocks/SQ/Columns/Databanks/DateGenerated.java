package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;

public class DateGenerated extends DatabankColumn {
   public DateGenerated() {
      super("Date generated", "Time", (byte)2, 0.0, 0.0, 10000.0);
      this.setTooltip(L.tsq("Date generated"));
      this.setWidth(150);
      this.printsSpecialValue(true);
   }

   public double getNumericValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return var1.specialValues().getLong(SpecialValues.DateGenerated, -1L);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return (long)this.getNumericValue(var1, var2, var3, var4, var5) + "";
   }

   public String exportValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      long var6 = (long)this.getNumericValue(var1, var2, var3, var4, var5);
      return var6 > 0L ? SQTime.toFullDateMinuteString(var6) : "N/A";
   }
}
