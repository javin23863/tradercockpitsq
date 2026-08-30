package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class CloseTime extends TradelistColumn {
   public CloseTime() {
      super(L.tsq("Close time"), "Time");
      this.setWidth(140);
   }

   public Object getValue(Order var1) {
      return var1.CloseTime;
   }

   public String getFormattedValue(Object var1) {
      long var2 = (Long)var1;
      return SQTime.toFullDateMinuteString(var2);
   }
}
