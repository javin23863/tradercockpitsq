package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class TimeInTrade extends TradelistColumn {
   public TimeInTrade() {
      super(L.tsq("Time in trade"), "Text");
      this.setWidth(90);
   }

   public Object getValue(Order var1) {
      return var1.Duration;
   }

   public String getFormattedValue(Object var1) {
      int var2 = (Integer)var1;
      return SQTime.formatDateTime(var2);
   }
}
