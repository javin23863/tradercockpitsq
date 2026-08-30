package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TradeAnalysisChart;

public class PLbyMonthChart extends TradeAnalysisChart {
   public PLbyMonthChart() {
      this.name = L.tsq("P/L by month");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      var4.invertIfNegative(true);
      if (var1 == null) {
         return var4;
      }

      double[] var5 = this.computeData(var1, var2, var3);

      for (int var6 = 0; var6 < var5.length; var6++) {
         var4.addValue(L.tsq("P/L"), this.monthToString(var6), var5[var6]);
      }

      return var4;
   }

   private double[] computeData(OrdersList var1, byte var2, byte var3) {
      double[] var4 = new double[12];

      for (int var5 = 0; var5 < var4.length; var5++) {
         var4[var5] = 0.0;
      }

      for (int var6 = 0; var6 < var1.size(); var6++) {
         Order var7 = var1.get(var6);
         int var8 = SQTime.getMonth(var7.getTimeByPeriodType(var3));
         var4[var8] += var7.getPLByType(var2);
      }

      return var4;
   }
}
