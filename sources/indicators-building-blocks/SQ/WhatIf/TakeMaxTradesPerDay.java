package SQ.WhatIf;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.WhatIf;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByOpenTime;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Take maximum trades per day", display = "Take maximum #Trades# trades per day")
@Help("Take maximum trades per day")
public class TakeMaxTradesPerDay extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(TakeMaxTradesPerDay.class);
   @Parameter(name = "Trades", defaultValue = "2", minValue = 1.0, maxValue = 10000.0, step = 1.0)
   public int Trades;

   public void filter(OrdersList var1) throws Exception {
      long var2 = -1L;
      long var4 = -1L;
      int var6 = 1;
      var1.sort(new OrderComparatorByOpenTime());
      ObjectListIterator var7 = var1.listIterator();

      while (var7.hasNext()) {
         Order var8 = (Order)var7.next();
         if (!var8.isBalanceOrder()) {
            var4 = SQTime.getDateInMs(var8.OpenTime);
            if (var4 == var2) {
               if (var6 == this.Trades) {
                  var7.remove();
                  continue;
               }

               var6++;
            } else {
               var6 = 1;
            }

            var2 = var4;
         }
      }
   }
}
