package SQ.Columns.Trades;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.TradelistColumn;

public class ClosePrice extends TradelistColumn {
   public ClosePrice() {
      super(L.tsq("Close price"), "Text");
      this.setWidth(80);
   }

   public Object getValue(Order var1) throws Exception {
      if (var1.isPendingOrder()) {
         return null;
      }

      DataInfo var2 = DataManager.getDataInfo("History", var1.Symbol);
      return var2 == null ? var1.ClosePrice + "" : SQUtils.d2String(var1.ClosePrice, var2.symbolInfo.decimals);
   }
}
