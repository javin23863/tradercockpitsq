package SQ.Columns.Trades;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.TradelistColumn;

public class SampleType extends TradelistColumn {
   public SampleType() {
      super(L.tsq("Sample type"), "Text");
      this.setWidth(80);
   }

   public Object getValue(Order var1) {
      return var1.SampleType;
   }

   public String getFormattedValue(Object var1) {
      byte var2 = (Byte)var1;
      return L.tnp(SampleTypes.typeToString(var2), new Object[0]);
   }
}
