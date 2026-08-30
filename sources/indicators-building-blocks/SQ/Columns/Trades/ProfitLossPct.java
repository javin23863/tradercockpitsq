package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class ProfitLossPct extends TradelistColumn {
   public ProfitLossPct() {
      super(L.tsq("Profit/Loss Pct"), "Decimal2Pct", true);
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      return var1.isPendingOrder() ? null : var1.PctPL;
   }
}
