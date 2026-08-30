package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class ResultName extends TradelistColumn {
   public ResultName() {
      super(L.tsq("Result name"), "Text");
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      return var1.SetupName;
   }

   public String getFormattedValue(Object var1) {
      return var1.toString();
   }
}
