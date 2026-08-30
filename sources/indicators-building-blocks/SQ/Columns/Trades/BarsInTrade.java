package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class BarsInTrade extends TradelistColumn {
   public BarsInTrade() {
      super(L.tsq("BarsInTrade"), "Integer");
      this.setWidth(100);
   }

   public Object getValue(Order var1) throws Exception {
      return var1.BarsInTrade;
   }
}
