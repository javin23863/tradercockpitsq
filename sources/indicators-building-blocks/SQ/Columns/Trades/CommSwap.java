package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class CommSwap extends TradelistColumn {
   public CommSwap() {
      super(L.tsq("Comm/Swap"), "Decimal2PL");
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      return var1.isPendingOrder() ? null : var1.CommSwap;
   }
}
