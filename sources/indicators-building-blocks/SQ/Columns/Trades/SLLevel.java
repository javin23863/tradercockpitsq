package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class SLLevel extends TradelistColumn {
   public SLLevel() {
      super(L.tsq("Stop Loss price level"), "Decimal5");
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      return var1.StopLoss;
   }
}
