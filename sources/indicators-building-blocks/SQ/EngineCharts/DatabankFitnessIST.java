package SQ.EngineCharts;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.TimeSeries;
import com.strategyquant.tradinglib.TimeSeriesLineChart;
import com.strategyquant.tradinglib.gp.FitnessCollectionData;
import org.json.JSONObject;

public class DatabankFitnessIST extends EngineChart {
   private TimeSeriesLineChart chart = null;
   private TimeSeries bestSeries = null;
   private TimeSeries topAvgSeries = null;
   private TimeSeries avgSeries = null;

   public DatabankFitnessIST() {
      super(L.tsq("Databank Fitness - IS Training"), 50);
      this.chart = new TimeSeriesLineChart();
      this.chart.yAxisRangeMin = 0.0;
      this.chart.yAxisRangeMax = 1.0;
      this.chart.maxXTicksLimit = 4;
      this.chart.showLegend = true;
      this.bestSeries = new TimeSeries(L.t("Top Strategy", new Object[0]), 1000);
      this.bestSeries.color = "#008000";
      this.chart.addSeries(this.bestSeries);
      this.topAvgSeries = new TimeSeries(L.t("Top 10 Avg", new Object[0]), 1000);
      this.topAvgSeries.color = "#383CE8";
      this.chart.addSeries(this.topAvgSeries);
      this.avgSeries = new TimeSeries(L.t("All Avg", new Object[0]), 1000);
      this.avgSeries.color = "#E8383C";
      this.chart.addSeries(this.avgSeries);
   }

   public JSONObject print() {
      JSONObject var1 = new JSONObject();
      var1.put("type", "chart");
      var1.put("chart", this.chart.toJSON());
      return var1;
   }

   public void addNextValue() {
      try {
         Databank var1 = this.project.getResultsDatabank();
         if (var1 == null) {
            return;
         }

         if (!this.project.isRunning()) {
            return;
         }

         this.addValueFromDatabank(var1);
      } catch (Exception var2) {
         Log.error("Exc.", var2);
      }
   }

   private void addValueFromDatabank(Databank var1) {
      FitnessCollectionData var2 = var1.getFitnessData();
      long var3 = System.currentTimeMillis();
      this.bestSeries.addValue(var3, var2.bestFitnessIST);
      this.topAvgSeries.addValue(var3, var2.topAvgFitnessIST);
      this.avgSeries.addValue(var3, var2.avgFitnessIST);
   }

   private void reset() {
      this.bestSeries.clear();
      this.topAvgSeries.clear();
      this.avgSeries.clear();
   }
}
