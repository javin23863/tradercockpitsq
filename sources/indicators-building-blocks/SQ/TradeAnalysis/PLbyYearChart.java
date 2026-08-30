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

public class PLbyYearChart extends TradeAnalysisChart {
   public PLbyYearChart() {
      this.name = L.tsq("P/L by year");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      var4.xAxisTitle = L.tsq("PL by Year");
      var4.invertIfNegative(true);
      if (var1 == null) {
         return var4;
      }

      if (var2 == 10) {
         var4.xAxisTitle = L.tsq("PL in money by Year");
      } else if (var2 == 30) {
         var4.xAxisTitle = L.tsq("PL in pips by Year");
      } else {
         var4.xAxisTitle = L.tsq("PL in % by Year");
      }

      TreeMap var5 = this.computeData(var1, var2, var3);

      for (Entry var7 : var5.entrySet()) {
         var4.addValue(L.tsq("P/L"), (Comparable)var7.getKey(), (Number)var7.getValue());
      }

      return var4;
   }

   private TreeMap<Integer, Double> computeData(OrdersList var1, byte var2, byte var3) {
      TreeMap var4 = new TreeMap();

      for (int var8 = 0; var8 < var1.size(); var8++) {
         Order var9 = var1.get(var8);
         int var7 = SQTime.getFullYear(var9.getTimeByPeriodType(var3));
         double var5 = var9.getPLByType(var2);
         if (var4.containsKey(var7)) {
            var4.put(var7, (Double)var4.get(var7) + var5);
         } else {
            var4.put(var7, var5);
         }
      }

      return var4;
   }
}
