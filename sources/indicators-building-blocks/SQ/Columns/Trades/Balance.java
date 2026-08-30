package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class Balance extends TradelistColumn {
   public Balance() {
      super(L.tsq("Balance"), "Decimal2PL");
      this.setWidth(100);
   }

   public Object getValue(Order var1) throws Exception {
      return var1.AccountBalance;
   }
}
