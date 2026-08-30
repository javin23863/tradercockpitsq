package SQ.EngineCharts;

import com.strategyquant.lib.L;
import com.strategyquant.lib.MemoryInfo;
import com.strategyquant.lib.memory.OffHeapMemory;
import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.TimeSeries;
import com.strategyquant.tradinglib.TimeSeriesAreaChart;
import org.json.JSONObject;

public class OffHeapMemoryChart extends EngineChart {
   private TimeSeriesAreaChart chart = null;
   private TimeSeries series = null;
   MemoryInfo offHeapInfo = new MemoryInfo();

   public OffHeapMemoryChart() {
      super(L.tsq("Off-heap memory chart"), 10);
      this.chart = new TimeSeriesAreaChart();
      this.chart.yAxisTitle = L.t("Memory", new Object[0]);
      this.chart.xAxisTitle = L.t("Time", new Object[0]);
      this.chart.maxXTicksLimit = 4;
      this.chart.yAxisRangeMin = 0.0;
      this.chart.maxYTicksLimit = 6;
      this.chart.yAxisTicksMemory = true;
      this.series = new TimeSeries(L.t("Off-Heap Size", new Object[0]), 1000);
      this.series.color = "#FF6E66";
      this.chart.addSeries(this.series);
   }

   public JSONObject print() {
      JSONObject var1 = new JSONObject();
      var1.put("type", "chart");
      var1.put("chart", this.chart.toJSON());
      return var1;
   }

   public void addNextValue() {
      OffHeapMemory.getInfo(this.offHeapInfo);
      long var1 = this.offHeapInfo.allocatedMemory / 1048576L;
      if (var1 < 0L) {
         var1 = 0L;
      }

      try {
         this.series.addValue(System.currentTimeMillis(), var1);
      } catch (Exception var4) {
         Log.error("Exc.", var4);
      }
   }
}
