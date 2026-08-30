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

@ClassConfig(name = "Trade only in days", display = "Trade only in days")
@Help("Trade only in days")
public class ByDays extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(ByDays.class);
   @Parameter(name = "Monday", defaultValue = "true")
   public boolean Monday;
   @Parameter(name = "Tuesday", defaultValue = "true")
   public boolean Tuesday;
   @Parameter(name = "Wednesday", defaultValue = "true")
   public boolean Wednesday;
   @Parameter(name = "Thursday", defaultValue = "true")
   public boolean Thursday;
   @Parameter(name = "Friday", defaultValue = "true")
   public boolean Friday;
   @Parameter(name = "Saturday", defaultValue = "true")
   public boolean Saturday;
   @Parameter(name = "Sunday", defaultValue = "true")
   public boolean Sunday;

   public void filter(OrdersList var1) throws Exception {
      ObjectListIterator var2 = var1.listIterator();

      while (var2.hasNext()) {
         Order var3 = (Order)var2.next();
         int var4 = SQTime.getDayOfWeek(var3.OpenTime);
         if (!this.Monday && var4 == 1
            || !this.Tuesday && var4 == 2
            || !this.Wednesday && var4 == 3
            || !this.Thursday && var4 == 4
            || !this.Friday && var4 == 5
            || !this.Saturday && var4 == 6
            || !this.Sunday && var4 == 7) {
            var2.remove();
         }
      }
   }
}
