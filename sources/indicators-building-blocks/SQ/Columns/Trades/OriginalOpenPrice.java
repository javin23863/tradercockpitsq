package SQ.Columns.Trades;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class OriginalOpenPrice extends TradelistColumn {
   public OriginalOpenPrice() {
      super(L.tsq("Orig. Open price"), "Text");
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      DataInfo var2 = DataManager.getDataInfo("History", var1.Symbol);
      return var2 == null ? var1.OriginalPrice + "" : SQUtils.d2String(var1.OriginalPrice, var2.symbolInfo.decimals);
   }
}
