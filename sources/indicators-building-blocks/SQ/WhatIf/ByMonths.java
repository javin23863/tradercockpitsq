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

@ClassConfig(name = "Trade only in months", display = "Trade only in months")
@Help("Trade only in months")
public class ByMonths extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(ByMonths.class);
   @Parameter(name = "January", defaultValue = "true")
   public boolean January;
   @Parameter(name = "February", defaultValue = "true")
   public boolean February;
   @Parameter(name = "March", defaultValue = "true")
   public boolean March;
   @Parameter(name = "April", defaultValue = "true")
   public boolean April;
   @Parameter(name = "May", defaultValue = "true")
   public boolean May;
   @Parameter(name = "June", defaultValue = "true")
   public boolean June;
   @Parameter(name = "July", defaultValue = "true")
   public boolean July;
   @Parameter(name = "August", defaultValue = "true")
   public boolean August;
   @Parameter(name = "September", defaultValue = "true")
   public boolean September;
   @Parameter(name = "October", defaultValue = "true")
   public boolean October;
   @Parameter(name = "November", defaultValue = "true")
   public boolean November;
   @Parameter(name = "December", defaultValue = "true")
   public boolean December;

   public void filter(OrdersList var1) throws Exception {
      ObjectListIterator var2 = var1.listIterator();

      while (var2.hasNext()) {
         Order var3 = (Order)var2.next();
         int var4 = SQTime.getMonthOriginal(var3.OpenTime);
         if (!this.January && var4 == 1
            || !this.February && var4 == 2
            || !this.March && var4 == 3
            || !this.April && var4 == 4
            || !this.May && var4 == 5
            || !this.June && var4 == 6
            || !this.July && var4 == 7
            || !this.August && var4 == 8
            || !this.September && var4 == 9
            || !this.October && var4 == 10
            || !this.November && var4 == 11
            || !this.December && var4 == 12) {
            var2.remove();
         }
      }
   }
}
