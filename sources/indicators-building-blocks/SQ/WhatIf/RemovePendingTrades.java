package SQ.WhatIf;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.WhatIf;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Remove pending trades", display = "Remove pending trades")
@Help("Remove pending trades")
public class RemovePendingTrades extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(RemovePendingTrades.class);

   public void filter(OrdersList var1) throws Exception {
      ObjectListIterator var2 = var1.listIterator();

      while (var2.hasNext()) {
         Order var3 = (Order)var2.next();
         if (var3.isPendingOrder()) {
            var2.remove();
         }
      }
   }
}
