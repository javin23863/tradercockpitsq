package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class ProfitLossPips extends TradelistColumn {
   public ProfitLossPips() {
      super(L.tsq("Profit/Loss Pips"), "Decimal2Pips", true);
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      return var1.isPendingOrder() ? null : var1.PipsPL;
   }
}
