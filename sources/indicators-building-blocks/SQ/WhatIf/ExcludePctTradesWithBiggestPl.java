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

@ClassConfig(name = "Exclude % of trades with biggest profit", display = "Exclude #TradesPct#% trades with biggest profit")
@Help("Exclude % of trades with biggest profit")
public class ExcludePctTradesWithBiggestPl extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(ExcludePctTradesWithBiggestPl.class);
   @Parameter(name = "% Trades", defaultValue = "5", minValue = 1.0, maxValue = 100.0, step = 1.0)
   public int TradesPct;

   public void filter(OrdersList var1) throws Exception {
      var1.sort(new ComparatorByProfit((byte)10));
      int var2 = (int)Math.ceil(var1.size() * (this.TradesPct / 100.0));
      int var3 = 0;
      ObjectListIterator var4 = var1.listIterator();

      while (var4.hasNext()) {
         Order var5 = (Order)var4.next();
         if (!var5.isBalanceOrder()) {
            if (var2 == var3) {
               break;
            }

            var4.remove();
            var3++;
         }
      }
   }
}
