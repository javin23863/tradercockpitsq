package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PieChart;
import com.strategyquant.tradinglib.TradeAnalysisChart;

public class LongShortTradesChart extends TradeAnalysisChart {
   public LongShortTradesChart() {
      this.name = L.tsq("Long vs Short trades");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      PieChart var4 = new PieChart();
      if (var1 == null) {
         return var4;
      }

      int[] var5 = this.computeData(var1, var2);
      var4.addValue(L.tsq("Long trades"), var5[0], "#383CE8");
      var4.addValue(L.tsq("Short trades"), var5[1], "#E8383C");
      return var4;
   }

   private int[] computeData(OrdersList var1, byte var2) {
      int[] var3 = new int[2];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = 0;
      }

      for (int var6 = 0; var6 < var1.size(); var6++) {
         Order var5 = var1.get(var6);
         if (var5.isLong()) {
            var3[0]++;
         } else {
            var3[1]++;
         }
      }

      return var3;
   }
}
