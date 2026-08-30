package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TradeAnalysisChart;

public class WinLossPLByDayChart extends TradeAnalysisChart {
   public WinLossPLByDayChart() {
      this.name = L.tsq("Wins/Losses Profit by day");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      var4.setCategoryColor("winners", "#008000");
      var4.setCategoryColor("losers", "#E8383C");
      if (var1 == null) {
         return var4;
      }

      double[][] var5 = this.computeData(var1, var2, var3);

      for (int var6 = 0; var6 < var5.length; var6++) {
         var4.addValue("winners", var6 + 1, var5[var6][0]);
         var4.addValue("losers", var6 + 1, var5[var6][1]);
      }

      return var4;
   }

   private double[][] computeData(OrdersList var1, byte var2, byte var3) {
      double[][] var4 = new double[31][2];

      for (int var5 = 0; var5 < var4.length; var5++) {
         var4[var5][0] = 0.0;
         var4[var5][1] = 0.0;
      }

      for (int var8 = 0; var8 < var1.size(); var8++) {
         Order var9 = var1.get(var8);
         int var10 = SQTime.getDay(var9.getTimeByPeriodType(var3));
         double var6 = var9.getPLByType(var2);
         if (var6 > 0.0) {
            var4[var10 - 1][0] = var4[var10 - 1][0] + var6;
         } else {
            var4[var10 - 1][1] = var4[var10 - 1][1] + Math.abs(var6);
         }
      }

      return var4;
   }
}
