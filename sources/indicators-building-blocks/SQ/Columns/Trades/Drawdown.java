package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class Drawdown extends TradelistColumn {
   public Drawdown() {
      super(L.tsq("Drawdown"), "Decimal2PL");
   }

   public Object getValue(Order var1) {
      return var1.isPendingOrder() ? null : var1.DD;
   }
}
