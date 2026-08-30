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

@ClassConfig(name = "Trade only in hours", display = "Trade only in hours")
@Help("Trade only in hours")
public class ByHours extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(ByHours.class);
   @Parameter(name = "Hour 0", defaultValue = "true")
   public boolean H0;
   @Parameter(name = "Hour 1", defaultValue = "true")
   public boolean H1;
   @Parameter(name = "Hour 2", defaultValue = "true")
   public boolean H2;
   @Parameter(name = "Hour 3", defaultValue = "true")
   public boolean H3;
   @Parameter(name = "Hour 4", defaultValue = "true")
   public boolean H4;
   @Parameter(name = "Hour 5", defaultValue = "true")
   public boolean H5;
   @Parameter(name = "Hour 6", defaultValue = "true")
   public boolean H6;
   @Parameter(name = "Hour 7", defaultValue = "true")
   public boolean H7;
   @Parameter(name = "Hour 8", defaultValue = "true")
   public boolean H8;
   @Parameter(name = "Hour 9", defaultValue = "true")
   public boolean H9;
   @Parameter(name = "Hour 10", defaultValue = "true")
   public boolean H10;
   @Parameter(name = "Hour 11", defaultValue = "true")
   public boolean H11;
   @Parameter(name = "Hour 12", defaultValue = "true")
   public boolean H12;
   @Parameter(name = "Hour 13", defaultValue = "true")
   public boolean H13;
   @Parameter(name = "Hour 14", defaultValue = "true")
   public boolean H14;
   @Parameter(name = "Hour 15", defaultValue = "true")
   public boolean H15;
   @Parameter(name = "Hour 16", defaultValue = "true")
   public boolean H16;
   @Parameter(name = "Hour 17", defaultValue = "true")
   public boolean H17;
   @Parameter(name = "Hour 18", defaultValue = "true")
   public boolean H18;
   @Parameter(name = "Hour 19", defaultValue = "true")
   public boolean H19;
   @Parameter(name = "Hour 20", defaultValue = "true")
   public boolean H20;
   @Parameter(name = "Hour 21", defaultValue = "true")
   public boolean H21;
   @Parameter(name = "Hour 22", defaultValue = "true")
   public boolean H22;
   @Parameter(name = "Hour 23", defaultValue = "true")
   public boolean H23;

   public void filter(OrdersList var1) throws Exception {
      ObjectListIterator var2 = var1.listIterator();

      while (var2.hasNext()) {
         Order var3 = (Order)var2.next();
         int var4 = SQTime.getHour(var3.OpenTime);
         if (!this.H0 && var4 == 0
            || !this.H1 && var4 == 1
            || !this.H2 && var4 == 2
            || !this.H3 && var4 == 3
            || !this.H4 && var4 == 4
            || !this.H5 && var4 == 5
            || !this.H6 && var4 == 6
            || !this.H7 && var4 == 7
            || !this.H8 && var4 == 8
            || !this.H9 && var4 == 9
            || !this.H10 && var4 == 10
            || !this.H11 && var4 == 11
            || !this.H12 && var4 == 12
            || !this.H13 && var4 == 13
            || !this.H14 && var4 == 14
            || !this.H15 && var4 == 15
            || !this.H16 && var4 == 16
            || !this.H17 && var4 == 17
            || !this.H18 && var4 == 18
            || !this.H19 && var4 == 19
            || !this.H20 && var4 == 20
            || !this.H21 && var4 == 21
            || !this.H22 && var4 == 22
            || !this.H23 && var4 == 23) {
            var2.remove();
         }
      }
   }
}
