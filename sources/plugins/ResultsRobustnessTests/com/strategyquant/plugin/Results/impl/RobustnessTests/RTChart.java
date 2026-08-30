package com.strategyquant.plugin.Results.impl.RobustnessTests;

import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.equitychart.EquityChart;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.equitychart.MainChartDataset;
import com.strategyquant.tradinglib.robustnesstests.RobustnessOrdersValues;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTChart {
   private static final Logger Log = LoggerFactory.getLogger(RTChart.class);

   public JSONObject print(Result var1, String var2, int var3, int var4) {
      EquityChart var5 = new EquityChart();
      MainChartDataset var6 = null;
      RobustnessResults var7 = (RobustnessResults)var1.get("RobustnessOriginalResults");
      if (var7 != null) {
         var6 = this.createSeries("Original", var7.getOrdersValues(), 0);
         var6.filled = true;
         var6.fillColor = "#DAF5FF";
         var6.fillNegativeColor = "#F49C9C";
         var6.thickness = 3;
         var5.addMainChartDataset(var6);
      }

      int var8 = var1.getInt(var2 + "_NumberOfSimulations");

      for (int var9 = 0; var9 < var8; var9++) {
         var7 = var1.getMCSimulation(var2, var9);
         if (var7 != null) {
            var6 = this.createSeries("Simulation " + var9, var7.getOrdersValues(), var9 + 1);
            if (var6 != null) {
               var5.addMainChartDataset(var6);
            }
         }
      }

      return var5.toJSON();
   }

   private MainChartDataset createSeries(String var1, RobustnessOrdersValues var2, int var3) {
      double var4 = 0.0;
      int[] var6 = var2.arr();
      if (var6 == null) {
         return null;
      }

      MainChartDataset var7 = new MainChartDataset("RobustnessTests", var6.length);
      var7.name = var1;
      var7.title = var1;
      var7.color = EquityChartUtils.getColor(var3, false);
      var7.colorDark = EquityChartUtils.getColor(var3, true);

      for (int var8 = 0; var8 < var6.length; var8++) {
         var4 += var6[var8] / 100.0;
         var7.addValue(var8, var4);
      }

      return var7;
   }
}
