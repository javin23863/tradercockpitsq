package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TradeAnalysisChart;

public class WinLossCountByHourChart extends TradeAnalysisChart {
   public WinLossCountByHourChart() {
      this.name = L.tsq("Wins/Losses by hour");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      var4.setCategoryColor("winners", "#008000");
      var4.setCategoryColor("losers", "#E8383C");
      if (var1 == null) {
         return var4;
      }

      int[][] var5 = this.computeData(var1, var2, var3);

      for (int var6 = 0; var6 < var5.length; var6++) {
         var4.addValue("winners", var6, var5[var6][0]);
         var4.addValue("losers", var6, var5[var6][1]);
      }

      return var4;
   }

   private int[][] computeData(OrdersList var1, byte var2, byte var3) {
      int[][] var4 = new int[24][2];

      for (int var5 = 0; var5 < var4.length; var5++) {
         var4[var5][0] = 0;
         var4[var5][1] = 0;
      }

      for (int var6 = 0; var6 < var1.size(); var6++) {
         Order var7 = var1.get(var6);
         int var8 = SQTime.getHour(var7.getTimeByPeriodType(var3));
         if (var7.getPLByType(var2) > 0.0F) {
            var4[var8][0]++;
         } else {
            var4[var8][1]++;
         }
      }

      return var4;
   }
}
