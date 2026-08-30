package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class PTLevel extends TradelistColumn {
   public PTLevel() {
      super(L.tsq("Profit Target price level"), "Decimal5");
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      return var1.TakeProfit;
   }
}
