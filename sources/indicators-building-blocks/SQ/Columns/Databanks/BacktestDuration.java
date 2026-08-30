package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;

public class BacktestDuration extends DatabankColumn {
   public BacktestDuration() {
      super(L.tsq("Backtest Duration (s)"), "Text", (byte)2, 0.0, 0.0, 200.0);
      this.setTooltip(L.tsq("Backtest duration in seconds"));
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return Double.toString(var1.specialValues().getDouble(SpecialValues.BacktestDuration, 0.0));
   }
}
