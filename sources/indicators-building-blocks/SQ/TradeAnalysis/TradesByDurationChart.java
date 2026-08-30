package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TimeDuration;
import com.strategyquant.tradinglib.TradeAnalysisChart;
import java.util.ArrayList;

public class TradesByDurationChart extends TradeAnalysisChart {
   private ArrayList<TimeDuration> timeDurationList;

   public TradesByDurationChart() {
      this.name = L.tsq("Trades by duration");
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      BarChart var4 = new BarChart();
      if (var1 == null) {
         return var4;
      }

      int[] var5 = this.computeData(var1, var2);

      for (int var6 = 0; var6 < var5.length; var6++) {
         var4.addValue(L.tsq("Trades"), this.timeDurationList.get(var6).toString(), var5[var6]);
      }

      return var4;
   }

   private int[] computeData(OrdersList var1, byte var2) {
      this.timeDurationList = this.calculateTimeDurationScale(var1);
      int[] var3 = new int[this.timeDurationList.size()];

      for (int var4 = 0; var4 < var3.length; var4++) {
         var3[var4] = 0;
      }

      for (int var7 = 0; var7 < var1.size(); var7++) {
         Order var5 = var1.get(var7);

         for (int var6 = 0; var6 < this.timeDurationList.size(); var6++) {
            if (var5.Duration < this.timeDurationList.get(var6).seconds) {
               var3[var6]++;
               break;
            }
         }
      }

      return var3;
   }
}
