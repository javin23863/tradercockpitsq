package SQ.WhatIf;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.WhatIf;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Use fixed lots", display = "Use #Size# fixed lots")
@Help("Use fixed lots")
public class UseFixedLots extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(UseFixedLots.class);
   @Parameter(name = "Size", defaultValue = "0.1", minValue = 0.01, maxValue = 100.0, step = 0.1)
   public double Size;

   public void filter(OrdersList var1) throws Exception {
      ObjectListIterator var2 = var1.listIterator();

      while (var2.hasNext()) {
         Order var3 = (Order)var2.next();
         var3.PL = var3.PL / var3.Size * (float)this.Size;
         var3.Size = (float)this.Size;
      }
   }
}
