package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class NumberOfTrade extends TradelistColumn {
   public NumberOfTrade() {
      super(L.tsq("Line Number"), "Integer");
   }

   public Object getValue(Order var1) {
      return var1.Order;
   }
}
