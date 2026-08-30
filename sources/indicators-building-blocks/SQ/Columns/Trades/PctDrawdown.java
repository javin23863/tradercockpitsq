package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class PctDrawdown extends TradelistColumn {
   public PctDrawdown() {
      super(L.tsq("% Drawdown"), "Decimal2Pct");
   }

   public Object getValue(Order var1) {
      return var1.isPendingOrder() ? null : var1.PctDD;
   }
}
