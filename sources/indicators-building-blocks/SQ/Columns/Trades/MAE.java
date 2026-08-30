package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class MAE extends TradelistColumn {
   public MAE() {
      super(L.tsq("MAE ($)"), "Decimal2PL");
   }

   public Object getValue(Order var1) {
      if (var1.isPendingOrder()) {
         return null;
      } else {
         return var1.MAE == 0.0F ? var1.MAE : -1.0F * var1.MAE;
      }
   }
}
