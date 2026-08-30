package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class OriginalOpenTime extends TradelistColumn {
   public OriginalOpenTime() {
      super(L.tsq("Orig. Open time"), "Time");
      this.setWidth(140);
   }

   public Object getValue(Order var1) {
      return var1.OriginalType != 3
            && var1.OriginalType != 5
            && var1.OriginalType != 7
            && var1.OriginalType != 4
            && var1.OriginalType != 6
            && var1.OriginalType != 8
         ? null
         : var1.OriginalOpenTime;
   }

   public String getFormattedValue(Object var1) {
      if (var1 == null) {
         return "";
      }

      long var2 = (Long)var1;
      return SQTime.toFullDateMinuteString(var2);
   }
}
