package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class Comment extends TradelistColumn {
   public Comment() {
      super(L.tsq("Comment"), "Text");
      this.setWidth(100);
   }

   public Object getValue(Order var1) {
      return var1.Comment;
   }

   public String getFormattedValue(Object var1) {
      String var2 = (String)var1;
      return var2 == null ? "" : var2;
   }
}
