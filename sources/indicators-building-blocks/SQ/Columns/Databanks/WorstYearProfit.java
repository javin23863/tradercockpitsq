package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class WorstYearProfit extends DatabankColumn {
   public WorstYearProfit() {
      super(L.tsq("Worst Year Profit"), "Decimal2PL", (byte)2, 0.0, -10000.0, 10000.0);
      this.setWidth(100);
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      Int2DoubleOpenHashMap var7 = new Int2DoubleOpenHashMap();
      var7.clear();

      for (int var9 = 0; var9 < var3.size(); var9++) {
         Order var10 = var3.get(var9);
         if (!var10.isBalanceOrder()) {
            double var11 = this.getPLByStatsType(var10, var2);
            if (var11 != 0.0) {
               int var8 = SQTime.getYear(var10.CloseTime);
               if (!var7.containsKey(var8)) {
                  var7.put(var8, 0.0);
               }

               var7.put(var8, var7.get(var8) + var11);
            }
         }
      }

      double var15 = 0.0;
      ObjectIterator var16 = var7.int2DoubleEntrySet().fastIterator();

      for (int var14 = 0; var16.hasNext(); var14++) {
         double var12 = ((Entry)var16.next()).getDoubleValue();
         if (var14 == 0) {
            var15 = var12;
         } else if (var15 > var12) {
            var15 = var12;
         }
      }

      return this.round2(var15);
   }
}
