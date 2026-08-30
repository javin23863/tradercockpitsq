package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.GeneticInfo;
import com.strategyquant.tradinglib.TimeSeries;
import com.strategyquant.tradinglib.TimeSeriesLineChart;
import com.strategyquant.tradinglib.XYLineChart;
import com.strategyquant.tradinglib.charts.linechart.series.XYSeries;
import com.strategyquant.tradinglib.gp.FitnessCollectionData;
import com.strategyquant.tradinglib.gp.GPFitnessEvolutionData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitnessEvolution {
   public static final Logger Log = LoggerFactory.getLogger(FitnessEvolution.class);
   private SQProject project;
   private FitnessEvolution.DatabankChart[] databankCharts = new FitnessEvolution.DatabankChart[3];
   private FitnessEvolution.IslandCharts[] islandCharts = null;
   private boolean geneticBuild;

   public FitnessEvolution(SQProject var1) {
      this.project = var1;
      this.databankCharts[0] = new FitnessEvolution.DatabankChart(L.tsq("In Sample Training"));
      this.databankCharts[1] = new FitnessEvolution.DatabankChart(L.tsq("In Sample Validation"));
      this.databankCharts[2] = new FitnessEvolution.DatabankChart(L.tsq("Out of sample"));
   }

   public boolean isGeneticBuild() {
      return this.geneticBuild;
   }

   public JSONObject print() {
      JSONObject var1 = new JSONObject();
      var1.put("geneticBuild", this.geneticBuild);
      if (this.isGeneticBuild()) {
         GeneticInfo var2 = this.project.getGeneticInfo();
         GPFitnessEvolutionData[] var3 = var2.getIslandsEvoData();
         JSONArray var4 = new JSONArray();

         for (int var5 = 0; var5 < var3.length && var5 != 30; var5++) {
            if (var3[var5] != null) {
               JSONObject var6 = new JSONObject().put("generation", var3[var5].generation).put("population", var3[var5].populationSize);
               if (var5 < this.islandCharts.length) {
                  var6.put(
                     "chart",
                     new JSONArray()
                        .put(this.islandCharts[var5].fitness[0].toJSON())
                        .put(this.islandCharts[var5].fitness[1].toJSON())
                        .put(this.islandCharts[var5].fitness[2].toJSON())
                  );
               }

               var4.put(var6);
            }
         }

         var1.put("islands", var4);
      }

      var1.put("databank", new JSONArray().put(this.databankCharts[0].toJSON()).put(this.databankCharts[1].toJSON()).put(this.databankCharts[2].toJSON()));
      return var1;
   }

   public synchronized void addNextIslansEvodData(GPFitnessEvolutionData[] var1, boolean var2) {
      for (int var3 = 0; var3 < var1.length; var3++) {
         GPFitnessEvolutionData var4 = var1[var3];
         if (var4 == null || var3 >= this.islandCharts.length) {
            break;
         }

         FitnessCollectionData[] var5 = var4.generationData;
         FitnessEvolution.IslandCharts var6 = this.islandCharts[var4.islandIndex];
         int var7 = 0;
         if (var6.lastGenerationIndex != var1[var3].generation) {
            if (var6.lastGenerationIndex < var1[var3].generation) {
               var7 = var6.lastGenerationIndex;
            }

            if (var7 < 0) {
               var7 = 0;
            }

            for (int var9 = var7; var9 < var1[var3].generation + 1; var9++) {
               String var8 = var9 + "";
               if (var5[var9] == null) {
                  break;
               }

               if (var6.fitness[0].bestSeries.values.size() <= 0
                  || !var6.fitness[0].bestSeries.values.get(var6.fitness[0].bestSeries.values.size() - 1).xLabel.equals(var8)) {
                  var6.fitness[0].bestSeries.addValue(var9, var5[var9].bestFitnessIST, var8);
                  var6.fitness[0].topAvgSeries.addValue(var9, var5[var9].topAvgFitnessIST, var8);
                  var6.fitness[0].avgSeries.addValue(var9, var5[var9].avgFitnessIST, var8);
                  var6.fitness[1].bestSeries.addValue(var9, var5[var9].bestFitnessISV, var8);
                  var6.fitness[1].topAvgSeries.addValue(var9, var5[var9].topAvgFitnessISV, var8);
                  var6.fitness[1].avgSeries.addValue(var9, var5[var9].avgFitnessISV, var8);
                  var6.fitness[2].bestSeries.addValue(var9, var5[var9].bestFitnessOOS, var8);
                  var6.fitness[2].topAvgSeries.addValue(var9, var5[var9].topAvgFitnessOOS, var8);
                  var6.fitness[2].avgSeries.addValue(var9, var5[var9].avgFitnessOOS, var8);
               }
            }

            var6.lastGenerationIndex = var1[var3].generation;
         }
      }
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
      if (var2.bestFitnessIST != 0.0 || var2.topAvgFitnessIST != 0.0 || var2.avgFitnessIST != 0.0) {
         this.databankCharts[0].bestSeries.addValue(var3, var2.bestFitnessIST);
         this.databankCharts[0].topAvgSeries.addValue(var3, var2.topAvgFitnessIST);
         this.databankCharts[0].avgSeries.addValue(var3, var2.avgFitnessIST);
      } else if (var2.bestFitnessISV == 0.0) {
         this.databankCharts[0].bestSeries.addValue(var3, var2.bestFitnessIS);
         this.databankCharts[0].topAvgSeries.addValue(var3, var2.topAvgFitnessIS);
         this.databankCharts[0].avgSeries.addValue(var3, var2.avgFitnessIS);
      } else {
         this.databankCharts[0].bestSeries.addValue(var3, 0.0);
         this.databankCharts[0].topAvgSeries.addValue(var3, 0.0);
         this.databankCharts[0].avgSeries.addValue(var3, 0.0);
      }

      this.databankCharts[1].bestSeries.addValue(var3, var2.bestFitnessISV);
      this.databankCharts[1].topAvgSeries.addValue(var3, var2.topAvgFitnessISV);
      this.databankCharts[1].avgSeries.addValue(var3, var2.avgFitnessISV);
      this.databankCharts[2].bestSeries.addValue(var3, var2.bestFitnessOOS);
      this.databankCharts[2].topAvgSeries.addValue(var3, var2.topAvgFitnessOOS);
      this.databankCharts[2].avgSeries.addValue(var3, var2.avgFitnessOOS);
   }

   public void reset(boolean var1, int var2) {
      this.geneticBuild = var1;
      if (var2 > 20) {
         var2 = 20;
      }

      if (var1) {
         this.islandCharts = new FitnessEvolution.IslandCharts[var2];

         for (int var3 = 0; var3 < var2; var3++) {
            this.islandCharts[var3] = new FitnessEvolution.IslandCharts();
            this.islandCharts[var3].fitness[0] = new FitnessEvolution.IslandChart("In Sample Training");
            this.islandCharts[var3].fitness[1] = new FitnessEvolution.IslandChart("In Sample Validation");
            this.islandCharts[var3].fitness[2] = new FitnessEvolution.IslandChart("Out of sample");
         }
      } else {
         this.islandCharts = null;
      }
   }

   private class DatabankChart {
      public TimeSeriesLineChart chart = null;
      public TimeSeries bestSeries = null;
      public TimeSeries topAvgSeries = null;
      public TimeSeries avgSeries = null;

      public DatabankChart(String nullx) {
         this.chart = new TimeSeriesLineChart();
         this.chart.title = nullx;
         this.chart.yAxisRangeMin = 0.0;
         this.chart.yAxisRangeMax = 1.0;
         this.chart.maxXTicksLimit = 4;
         this.chart.showLegend = true;
         this.bestSeries = new TimeSeries(L.t("Top Strategy", new Object[0]), 1000);
         this.bestSeries.color = "#008000";
         this.chart.addSeries(this.bestSeries);
         this.topAvgSeries = new TimeSeries(L.t("Top Ten Avg", new Object[0]), 1000);
         this.topAvgSeries.color = "#383CE8";
         this.chart.addSeries(this.topAvgSeries);
         this.avgSeries = new TimeSeries(L.t("All Avg", new Object[0]), 1000);
         this.avgSeries.color = "#E8383C";
         this.chart.addSeries(this.avgSeries);
      }

      public JSONObject toJSON() {
         return this.chart.toJSON();
      }
   }

   private class IslandChart {
      public XYLineChart chart = null;
      public XYSeries bestSeries = null;
      public XYSeries topAvgSeries = null;
      public XYSeries avgSeries = null;

      public IslandChart(String nullx) {
         this.chart = new XYLineChart();
         this.chart.title = nullx;
         this.chart.yAxisRangeMin = 0.0;
         this.chart.yAxisRangeMax = 1.0;
         this.chart.pointRadius = 3;
         this.chart.showLegend = true;
         this.chart.categoryAxis = true;
         this.bestSeries = new XYSeries(L.tsq("Top Strategy"), 30);
         this.bestSeries.color = "#008000";
         this.chart.addSeries(this.bestSeries);
         this.topAvgSeries = new XYSeries(L.tsq("Top 10 Avg"), 30);
         this.topAvgSeries.color = "#383CE8";
         this.chart.addSeries(this.topAvgSeries);
         this.avgSeries = new XYSeries(L.tsq("All Avg"), 30);
         this.avgSeries.color = "#E8383C";
         this.chart.addSeries(this.avgSeries);
      }

      public JSONObject toJSON() {
         JSONObject var1 = this.chart.toJSON();
         JSONArray var2 = new JSONArray();

         for (int var3 = 0; var3 < this.chart.getSeries(0).values.size(); var3++) {
            if (this.chart.getSeries(0).values.get(var3).xLabel.equals("0")) {
               var2.put(var3);
            }
         }

         if (var2.length() > 0) {
            var1.put("lineAtIndex", var2);
         }

         return var1;
      }
   }

   private class IslandCharts {
      public int lastGenerationIndex = -1;
      public FitnessEvolution.IslandChart[] fitness = new FitnessEvolution.IslandChart[3];

      private IslandCharts() {
      }
   }
}
