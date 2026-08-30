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

@ClassConfig(name = "Exclude overlapping trades", display = "Exclude overlapping trades")
@Help("Exclude overlapping trades")
public class ExludeOverlappingTrades extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(ExludeOverlappingTrades.class);

   public void filter(OrdersList var1) throws Exception {
      long var2 = -1L;
      var1.sort(new OrderComparatorByOpenTime());
      ObjectListIterator var4 = var1.listIterator();

      while (var4.hasNext()) {
         Order var5 = (Order)var4.next();
         if (var2 == -1L) {
            var2 = var5.CloseTime;
         } else if (var5.OpenTime > var2) {
            var2 = var5.CloseTime;
         } else {
            var4.remove();
         }
      }
   }
}
