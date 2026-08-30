package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TimeDuration;
import com.strategyquant.tradinglib.TradeAnalysisChart;
import java.util.ArrayList;

public class PLbyTradeDurationChart extends TradeAnalysisChart {
   private ArrayList<TimeDuration> timeDurationList;

   public PLbyTradeDurationChart() {
      this.name = L.tsq("P/L by trade duration");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      var4.invertIfNegative(true);
      if (var1 == null) {
         return var4;
      }

      double[] var5 = this.computeData(var1, var2, var3);

      for (int var6 = 0; var6 < var5.length; var6++) {
         var4.addValue(L.tsq("P/L"), this.timeDurationList.get(var6).toString(), var5[var6]);
      }

      return var4;
   }

   private double[] computeData(OrdersList var1, byte var2, byte var3) {
      this.timeDurationList = this.calculateTimeDurationScale(var1);
      double[] var4 = new double[this.timeDurationList.size()];

      for (int var5 = 0; var5 < var4.length; var5++) {
         var4[var5] = 0.0;
      }

      for (int var8 = 0; var8 < var1.size(); var8++) {
         Order var6 = var1.get(var8);

         for (int var7 = 0; var7 < this.timeDurationList.size(); var7++) {
            if (var6.Duration < this.timeDurationList.get(var7).seconds) {
               var4[var7] += var6.getPLByType(var2);
               break;
            }
         }
      }

      return var4;
   }
}
