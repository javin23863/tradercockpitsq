package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class Size extends TradelistColumn {
   public Size() {
      super(L.tsq("Size"), "Text");
   }

   public Object getValue(Order var1) {
      return var1.Size;
   }
}
