package SQ.WhatIf;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.WhatIf;
import com.strategyquant.tradinglib.swap.SwapCalculator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Swap", display = "Swap (Long: #swapLongInput#,Short: #swapShortInput#)")
@Help("Simulate swap")
public class Swap extends WhatIf {
   public static final Logger Log = LoggerFactory.getLogger(Swap.class);
   @Parameter(name = "SwapDev Long", defaultValue = "10", minValue = -1000.0, maxValue = 1000.0, step = 0.01)
   public double swapLongInput;
   @Parameter(name = "SwapDev Short", defaultValue = "-10", minValue = -1000.0, maxValue = 1000.0, step = 0.01)
   public double swapShortInput;
   private SwapMethod swapMethod;

   public void filter(OrdersList var1) throws Exception {
      if (this.swapMethod == null) {
         this.swapMethod = new SwapMethod(true, "money", this.swapLongInput, this.swapShortInput, "WEDNESDAY", "23:00");
      }

      ObjectListIterator var2 = var1.listIterator();

      while (var2.hasNext()) {
         Order var3 = (Order)var2.next();
         var3.CommSwap = (float)(var3.CommSwap + SwapCalculator.calculate(var3, this.swapMethod));
      }
   }
}
