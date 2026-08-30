package com.strategyquant.plugin.Results.impl.OptimizationProfile.charts;

import com.strategyquant.tradinglib.BarChart;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.optimization.OptimizationProfile;
import com.strategyquant.tradinglib.optimization.OptimizationTestResult;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Optimization2DChart {
   private static final Logger Log = LoggerFactory.getLogger(Optimization2DChart.class);

   public JSONObject print(ResultsGroup var1, String var2, String var3) throws Exception {
      OptimizationProfile var4 = var1.getOptimizationProfile();
      ObjectArrayList var5 = var4.getOptimizations().clone();
      DatabankColumn var6 = (DatabankColumn)DatabankColumns.get().findClassByName(var3);
      DoubleArrayList var7 = new DoubleArrayList();

      for (int var12 = 0; var12 < var5.size(); var12++) {
         OptimizationTestResult var13 = (OptimizationTestResult)var5.get(var12);
         Int2DoubleOpenHashMap var14 = var13.parseParams();
         if (var14.containsKey(var2.hashCode())) {
            double var8 = var14.get(var2.hashCode());
            if (!var7.contains(var8)) {
               var7.add(var8);
            }
         }
      }

      Collections.sort(var7);
      BarChart var25 = new BarChart();
      var25.maxXTicksLimit = 10;
      var25.yAxisTitle = var6.getName();
      var25.xAxisTitle = var2;
      var25.tooltips = false;
      var25.disableEvents = true;
      boolean var26 = ((OptimizationTestResult)var5.get(0)).parseParams().size() == 1;
      var25.setCategoryColor("best", "#008000");
      if (!var26) {
         var25.setCategoryColor("worst", "#E8383C");
      }

      for (int var27 = 0; var27 < var7.size(); var27++) {
         double var24 = var7.get(var27);
         double var15 = Double.MAX_VALUE;
         double var17 = Double.MAX_VALUE;

         for (int var21 = 0; var21 < var5.size(); var21++) {
            OptimizationTestResult var22 = (OptimizationTestResult)var5.get(var21);
            Int2DoubleOpenHashMap var23 = var22.parseParams();
            if (var23.containsKey(var2.hashCode()) && var23.get(var2.hashCode()) == var24) {
               double var19 = var6.getNumericValue(var22.stats);
               if (var17 == Double.MAX_VALUE || var17 > var19) {
                  var17 = var19;
               }

               if (var15 == Double.MAX_VALUE || var15 < var19) {
                  var15 = var19;
               }
            }
         }

         if (var6.getValueType() == 1) {
            var25.addValue("best", var24, var15);
            if (!var26) {
               var25.addValue("worst", var24, var17);
            }
         } else {
            var25.addValue("best", var24, var17);
            if (!var26) {
               var25.addValue("worst", var24, var15);
            }
         }
      }

      return var25.toJSON();
   }
}
