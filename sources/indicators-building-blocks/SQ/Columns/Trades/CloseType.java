package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.ATMExit;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.TradelistColumn;

public class CloseType extends TradelistColumn {
   public CloseType() {
      super(L.tsq("Close type"), "Text");
   }

   public Object getValue(Order var1) {
      return var1.ExitIndex >= 0 ? ATMExit.toString(var1.CloseType, var1.ExitIndex) : L.tnp(OrderCloseTypes.toString(var1.CloseType), new Object[0]);
   }
}
