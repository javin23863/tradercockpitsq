package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class Ticket extends TradelistColumn {
   public Ticket() {
      super(L.tsq("Ticket"), "Integer");
   }

   public Object getValue(Order var1) {
      return var1.Ticket;
   }
}
