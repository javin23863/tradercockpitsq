package SQ.CorrelationOf;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.tradinglib.CorrelationLib;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;

public class Loss extends CorrelationType {
   public Loss() {
      this.name = L.tsq("Loss");
      this.dataType = 5;
   }

   public void computePeriods(OrdersList var1, int var2, TimePeriods var3) throws Exception {
      for (int var6 = 0; var6 < var1.size(); var6++) {
         Order var7 = var1.get(var6);
         if (!this.isCanceledOrder(var7)) {
            long var4 = CorrelationLib.getCorrectPeriod(var7.CloseTime, var2);
            if (!var3.containsKey(var4)) {
               throw new Exception("Period '" + SQTime.toDateMinuteString(var4) + "' not found!");
            }

            TimePeriod var10000 = (TimePeriod)var3.get(var4);
            var10000.value = var10000.value + var7.PL;
         }
      }
   }

   public boolean shouldSkipPeriod(double var1, double var3) {
      return var1 >= 0.0 && var3 >= 0.0;
   }
}
