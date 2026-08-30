package SQ.WhatIf;

import SQ.Functions.ComparatorByProfit;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.WhatIf;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Exclude trades with lowest profit", display = "Exclude #Trades# trades with lowest profit")
@Help("Exclude trades with lowest profit")
public class ExcludeTradesWithLowestPl extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(ExcludeTradesWithLowestPl.class);
   @Parameter(name = "Trades", defaultValue = "2", minValue = 1.0, maxValue = 10000.0, step = 1.0)
   public int Trades;

   public void filter(OrdersList var1) throws Exception {
      var1.sort(new ComparatorByProfit((byte)10));
      int var2 = 0;
      ObjectListIterator var3 = var1.listIterator(var1.size());

      while (var3.hasPrevious()) {
         Order var4 = (Order)var3.previous();
         if (!var4.isBalanceOrder()) {
            if (var2 == this.Trades) {
               break;
            }

            var3.remove();
            var2++;
         }
      }
   }
}
