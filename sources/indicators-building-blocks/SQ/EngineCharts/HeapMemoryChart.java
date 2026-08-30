package SQ.EngineCharts;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.TimeSeries;
import com.strategyquant.tradinglib.TimeSeriesAreaChart;
import java.lang.management.MemoryUsage;
import org.json.JSONObject;

public class HeapMemoryChart extends EngineChart {
   private TimeSeriesAreaChart chart = null;
   private TimeSeries memoryUsageSeries = null;
   private TimeSeries heapSizeSeries = null;
   private long checks = 0L;

   public HeapMemoryChart() {
      super(L.t("Heap memory chart", new Object[0]), 10);
      this.chart = new TimeSeriesAreaChart();
      this.chart.yAxisTitle = L.t("Memory", new Object[0]);
      this.chart.xAxisTitle = L.t("Time", new Object[0]);
      this.chart.yAxisRangeMin = 0.0;
      this.chart.yAxisRangeMax = new Float(this.maxHeapMemory * 1.1).longValue();
      this.chart.maxXTicksLimit = 4;
      this.chart.maxYTicksLimit = 6;
      this.chart.yAxisTicksMemory = true;
      this.memoryUsageSeries = new TimeSeries(L.t("Memory Usage", new Object[0]), 1000);
      this.memoryUsageSeries.color = "#FF6E66";
      this.heapSizeSeries = new TimeSeries(L.t("Heap Size", new Object[0]), 1000);
      this.heapSizeSeries.color = "#BFDFBF";
      this.chart.addSeries(this.memoryUsageSeries);
      this.chart.addSeries(this.heapSizeSeries);
   }

   public JSONObject print() {
      JSONObject var1 = new JSONObject();
      var1.put("type", "chart");
      var1.put("chart", this.chart.toJSON());
      this.checks++;
      if (this.checks % 5000L == 1L) {
         System.gc();
      }

      return var1;
   }

   public void addNextValue() {
      MemoryUsage var1 = this.memoryBean.getHeapMemoryUsage();
      long var2 = var1.getUsed() / 1048576L;
      long var4 = var1.getCommitted() / 1048576L;

      try {
         this.memoryUsageSeries.addValue(System.currentTimeMillis(), var2);
         this.heapSizeSeries.addValue(System.currentTimeMillis(), var4);
      } catch (Exception var7) {
         Log.error("Exc.", var7);
      }
   }
}
