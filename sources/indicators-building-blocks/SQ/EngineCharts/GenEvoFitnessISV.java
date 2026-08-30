package SQ.EngineCharts;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.GeneticInfo;
import com.strategyquant.tradinglib.TimeSeries;
import com.strategyquant.tradinglib.TimeSeriesLineChart;
import com.strategyquant.tradinglib.gp.FitnessCollectionData;
import com.strategyquant.tradinglib.gp.GPFitnessEvolutionData;
import org.json.JSONObject;

public class GenEvoFitnessISV extends EngineChart {
   private TimeSeriesLineChart chart = null;
   private TimeSeries bestSeries = null;
   private TimeSeries best5AvgSeries = null;
   private TimeSeries avgSeries = null;

   public GenEvoFitnessISV() {
      super(L.tsq("Island #1 - Fitness IS Validation"), 50);
      this.chart = new TimeSeriesLineChart();
      this.chart.yAxisRangeMin = 0.0;
      this.chart.yAxisRangeMax = 1.0;
      this.chart.maxXTicksLimit = 4;
      this.chart.showLegend = true;
      this.bestSeries = new TimeSeries(L.tsq("Top Strategy"), 1000);
      this.bestSeries.color = "#008000";
      this.chart.addSeries(this.bestSeries);
      this.best5AvgSeries = new TimeSeries(L.tsq("Top Ten Avg"), 1000);
      this.best5AvgSeries.color = "#383CE8";
      this.chart.addSeries(this.best5AvgSeries);
      this.avgSeries = new TimeSeries(L.tsq("All Avg"), 1000);
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

         this.addValueFromGenEvo();
      } catch (Exception var2) {
         Log.error("Exc.", var2);
      }
   }

   private void addValueFromGenEvo() {
      GeneticInfo var1 = this.project.getGeneticInfo();
      double var2 = 0.0;
      double var4 = 0.0;
      double var6 = 0.0;
      if (var1.isGeneticBuild()) {
         GPFitnessEvolutionData[] var8 = var1.getIslandsEvoData();
         if (var8 != null && var8.length > 0) {
            GPFitnessEvolutionData var9 = var8[0];
            if (var9.generationData.length > var9.generation && var9.generationData[var9.generation] != null) {
               FitnessCollectionData var10 = var9.generationData[var9.generation];
               if (var10 != null) {
                  var2 = var10.bestFitnessISV;
                  var4 = var10.topAvgFitnessISV;
                  var6 = var10.avgFitnessISV;
               }
            }
         }
      }

      this.bestSeries.addValue(System.currentTimeMillis(), var2);
      this.best5AvgSeries.addValue(System.currentTimeMillis(), var4);
      this.avgSeries.addValue(System.currentTimeMillis(), var6);
   }

   private void reset() {
      this.bestSeries.clear();
      this.best5AvgSeries.clear();
      this.avgSeries.clear();
   }
}
