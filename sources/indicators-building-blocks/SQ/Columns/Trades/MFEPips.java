package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class MFEPips extends TradelistColumn {
   public MFEPips() {
      super(L.tsq("MFE (pips)"), "Decimal2");
   }

   public Object getValue(Order var1) {
      return var1.isPendingOrder() ? null : var1.PipsMFE;
   }
}
