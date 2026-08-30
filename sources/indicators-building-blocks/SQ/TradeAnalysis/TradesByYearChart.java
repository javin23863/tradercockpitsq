package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TradeAnalysisChart;
import java.util.TreeMap;
import java.util.Map.Entry;

public class TradesByYearChart extends TradeAnalysisChart {
   public TradesByYearChart() {
      this.name = L.tsq("Trades by year");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      if (var1 == null) {
         return var4;
      }

      TreeMap var5 = this.computeData(var1, var2, var3);

      for (Entry var7 : var5.entrySet()) {
         var4.addValue(L.tsq("Trades"), (Comparable)var7.getKey(), (Number)var7.getValue());
      }

      return var4;
   }

   private TreeMap<Integer, Integer> computeData(OrdersList var1, byte var2, byte var3) {
      TreeMap var4 = new TreeMap();

      for (int var6 = 0; var6 < var1.size(); var6++) {
         Order var7 = var1.get(var6);
         int var5 = SQTime.getFullYear(var7.getTimeByPeriodType(var3));
         if (var4.containsKey(var5)) {
            var4.put(var5, (Integer)var4.get(var5) + 1);
         } else {
            var4.put(var5, 1);
         }
      }

      return var4;
   }
}
