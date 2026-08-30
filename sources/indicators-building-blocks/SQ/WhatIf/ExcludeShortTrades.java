package SQ.WhatIf;

import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.WhatIf;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Exclude short trades", display = "Exclude #TradesPct#% trades which takes less than #Minutes# minutes")
@Help("Exclude short trades")
public class ExcludeShortTrades extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(ExcludeShortTrades.class);
   @Parameter(name = "% Trades", defaultValue = "5", minValue = 1.0, maxValue = 100.0, step = 1.0)
   public int TradesPct;
   @Parameter(name = "Minutes", defaultValue = "10", minValue = 1.0, maxValue = 100000.0, step = 1.0)
   public int Minutes;

   public void filter(OrdersList var1) throws Exception {
      int var2 = (int)Math.ceil(var1.size() * (this.TradesPct / 100.0));
      int var3 = 0;
      ObjectListIterator var4 = var1.listIterator();

      while (var4.hasNext()) {
         Order var5 = (Order)var4.next();
         if (!var5.isBalanceOrder()) {
            if (var2 == var3) {
               break;
            }

            int var6 = SQTime.getMinutesBetween(var5.OpenTime, var5.CloseTime);
            if (var6 < this.Minutes) {
               var4.remove();
               var3++;
            }
         }
      }
   }
}
