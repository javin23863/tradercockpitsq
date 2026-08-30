package SQ.CorrelationOf;

import com.strategyquant.lib.L;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;

public class NumberOfOpenTrades extends CorrelationType {
   public NumberOfOpenTrades() {
      this.name = L.tsq("Number of open trades");
      this.dataType = 10;
   }

   public void computePeriods(OrdersList var1, int var2, TimePeriods var3) throws Exception {
      for (int var4 = 0; var4 < var1.size(); var4++) {
         Order var5 = var1.get(var4);
         if (!this.isCanceledOrder(var5)) {
            ObjectBidirectionalIterator var6 = var3.long2ObjectEntrySet().iterator();

            while (var6.hasNext()) {
               Entry var7 = (Entry)var6.next();
               TimePeriod var8 = (TimePeriod)var3.get(var7.getLongKey());
               if (var8.from < var5.CloseTime && var8.to >= var5.OpenTime) {
                  var8.value++;
               }
            }
         }
      }
   }
}
