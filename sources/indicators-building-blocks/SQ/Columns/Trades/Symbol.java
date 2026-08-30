package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class Symbol extends TradelistColumn {
   public Symbol() {
      super(L.tsq("Symbol"), "Text");
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      return var1.Symbol;
   }

   public String getFormattedValue(Object var1) {
      return var1.toString();
   }
}
