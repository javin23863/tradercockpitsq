package SQ.EngineCharts;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.ProjectRunInfo;
import com.strategyquant.tradinglib.TimeSeries;
import com.strategyquant.tradinglib.TimeSeriesLineChart;
import org.json.JSONObject;

public class AcceptedStrategiesPerHour extends EngineChart {
   private TimeSeriesLineChart chart = null;
   private TimeSeries series = null;
   private ProjectRunInfo projectRunInfo = new ProjectRunInfo();
   private ProjectRunInfo oldProjectRunInfo = new ProjectRunInfo();
   private int checksSinceLastChange = 0;

   public AcceptedStrategiesPerHour() {
      super(L.tsq("Accepted strategies per hour"), 50);
      this.chart = new TimeSeriesLineChart();
      this.chart.yAxisTitle = L.t("Strategies", new Object[0]);
      this.chart.xAxisTitle = L.t("Time", new Object[0]);
      this.chart.maxXTicksLimit = 4;
      this.chart.yAxisRangeMin = 0.0;
      this.series = new TimeSeries(L.t("Accepted str. per hour", new Object[0]), 1000);
      this.series.color = "#008000";
      this.chart.addSeries(this.series);
      this.oldProjectRunInfo.totalJobsDone = -1L;
      this.checksSinceLastChange = 0;
   }

   public JSONObject print() {
      JSONObject var1 = new JSONObject();
      this.project.loadTrackingInfo(this.projectRunInfo, false);
      this.checksSinceLastChange++;
      if (this.oldProjectRunInfo.totalJobsDone != -1L
         && this.projectRunInfo.totalJobsDone == this.oldProjectRunInfo.totalJobsDone
         && this.checksSinceLastChange <= 10) {
         return null;
      }

      this.oldProjectRunInfo.totalJobsDone = this.projectRunInfo.totalJobsDone;
      this.checksSinceLastChange = 0;
      var1.put("type", "chart");
      var1.put("chart", this.chart.toJSON());
      return var1;
   }

   public void addNextValue() {
      try {
         if (!this.project.isRunning()) {
            return;
         }

         this.series.addValue(System.currentTimeMillis(), this.projectRunInfo.acceptedStrategiesPerHour);
      } catch (Exception var2) {
         Log.error("Exc.", var2);
      }
   }
}
