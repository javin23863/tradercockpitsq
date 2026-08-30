package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TradeAnalysisChart;

public class LongShortProfitLossChart extends TradeAnalysisChart {
   public LongShortProfitLossChart() {
      this.name = L.tsq("Long vs Short P/L");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      if (var1 == null) {
         return var4;
      }

      double[] var5 = this.computeData(var1, var2);
      var4.addValue("PL", L.tsq("Long P/L"), var5[0]);
      var4.addValue("PL", L.tsq("Short P/L"), var5[1]);
      var4.invertIfNegative(true);
      return var4;
   }

   private double[] computeData(OrdersList var1, byte var2) {
      double[] var3 = new double[2];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = 0.0;
      }

      for (int var6 = 0; var6 < var1.size(); var6++) {
         Order var5 = var1.get(var6);
         if (OrderTypes.isLongOrder(var5.Type)) {
            var3[0] += var5.getPLByType(var2);
         } else {
            var3[1] += var5.getPLByType(var2);
         }
      }

      return var3;
   }
}
