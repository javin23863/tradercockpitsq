package SQ.WhatIf;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.WhatIf;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Take every second trade", display = "Take every second trade")
@Help("Take every second trade")
public class TakeEverySecondTrade extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(TakeEverySecondTrade.class);

   public void filter(OrdersList var1) throws Exception {
      boolean var2 = false;
      var1.sort(new OrderComparatorByOpenTime());

      for (ObjectListIterator var3 = var1.listIterator(); var3.hasNext(); var2 = !var2) {
         Order var4 = (Order)var3.next();
         if (var2) {
            var3.remove();
         }
      }
   }
}
