package com.strategyquant.plugin.Results.impl.OptimizationProfile.charts;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.ScatterChart;
import com.strategyquant.tradinglib.charts.linechart.series.XYSeries;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.OptimizationTestResult;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationScatterChart {
   private static final Logger Log = LoggerFactory.getLogger(OptimizationScatterChart.class);

   public JSONObject print(ResultsGroup var1, String var2) throws Exception {
      OptimizationProfile var3 = var1.getOptimizationProfile();
      ObjectArrayList var4 = var3.getOptimizations().clone();
      DatabankColumn var5 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      ScatterChart var8 = new ScatterChart();
      XYSeries var9 = new XYSeries("Optimizations");
      var9.color = "#383CE8";
      var8.addSeries(var9);
      var8.yAxisTitle = var5.getName();
      var8.xAxisTitle = L.t("Optimizations", new Object[0]);
      var8.pointRadius = 3;
      var8.xAxisRangeMin = 0.0;
      var8.xAxisRangeMax = var4.size();

      for (int var10 = 0; var10 < var4.size(); var10++) {
         OptimizationTestResult var11 = (OptimizationTestResult)var4.get(var10);
         double var6 = var5.getNumericValue(var11.stats);
         var9.addValue(var10, var6);
      }

      return var8.toJSON();
   }
}
