package SQ.TradeAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.AbstractChart;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.TimeSeries;
import com.strategyquant.tradinglib.TimeSeriesScatterChart;
import com.strategyquant.tradinglib.TradeAnalysisChart;
import java.util.ArrayList;

public class PLGrowthByDurationChart extends TradeAnalysisChart {
   private TimeSeriesScatterChart chart = null;
   private TimeSeries winSeries;
   private TimeSeries lossSeries;

   public PLGrowthByDurationChart() {
      this.name = L.tsq("P/L Growth by duration");
      this.chart = new TimeSeriesScatterChart();
      this.chart.xAxisTicksDurationInMS = true;
      this.winSeries = new TimeSeries("losers");
      this.winSeries.color = "#008000";
      this.chart.addSeries(this.winSeries);
      this.lossSeries = new TimeSeries("winners");
      this.lossSeries.color = "#E8383C";
      this.chart.addSeries(this.lossSeries);
   }

   public AbstractChart draw(OrdersList var1, byte var2, byte var3) {
      this.lossSeries.clear();
      this.winSeries.clear();
      if (var1 == null) {
         return this.chart;
      }

      ArrayList var4 = this.computeData(var1, var2);

      for (int var5 = 0; var5 < var4.size(); var5++) {
         double[] var6 = (double[])var4.get(var5);
         if (var6[1] < 0.0) {
            this.lossSeries.addValue((long)var6[0], var6[1]);
         } else {
            this.winSeries.addValue((long)var6[0], var6[1]);
         }
      }

      return this.chart;
   }

   private ArrayList<double[]> computeData(OrdersList var1, byte var2) {
      ArrayList var3 = new ArrayList();
      new Order();

      for (int var4 = 0; var4 < var1.size(); var4++) {
         Order var5 = var1.get(var4);
         double[] var6 = new double[]{var5.Duration, var5.getPLByType(var2)};
         var3.add(var6);
      }

      return var3;
   }
}
