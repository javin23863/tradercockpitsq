package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.miniequitychart.MiniEquityChartComputer;

public class MiniEquityChart extends DatabankColumn {
   public MiniEquityChart() {
      super(L.tsq("Mini equity chart"), "Text", (byte)2, 0.0, 0.0, 0.0);
      this.setWidth(150);
      this.setEditable(true);
   }

   public String getValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return MiniEquityChartComputer.getValue(var1, var2, var5);
   }

   public String exportValue(ResultsGroup var1, String var2, byte var3, byte var4, byte var5) throws Exception {
      return "";
   }
}
