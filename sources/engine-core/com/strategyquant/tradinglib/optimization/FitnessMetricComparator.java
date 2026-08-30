package com.strategyquant.tradinglib.optimization;

import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import java.util.Comparator;

public class FitnessMetricComparator implements Comparator<ResultsGroup> {
   private final MetricForFitness metric;
   private final IFitnessFunction fitnessFunction;

   public FitnessMetricComparator(MetricForFitness var1, IFitnessFunction var2) {
      this.metric = var1;
      this.fitnessFunction = var2;
   }

   public int compare(ResultsGroup var1, ResultsGroup var2) {
      try {
         double var3 = this.fitnessFunction.getMetricValue(var1, (byte)0, (byte)10, this.metric.metric);
         double var5 = this.fitnessFunction.getMetricValue(var2, (byte)0, (byte)10, this.metric.metric);
         if (this.metric.valueType == 1) {
            if (var3 < var5) {
               return 1;
            }

            if (var3 > var5) {
               return -1;
            }
         } else if (this.metric.valueType == 2) {
            if (var3 < var5) {
               return -1;
            }

            if (var3 > var5) {
               return 1;
            }
         } else {
            var3 = Math.abs(var3 - this.metric.target);
            var5 = Math.abs(var5 - this.metric.target);
            if (var3 < var5) {
               return -1;
            }

            if (var3 > var5) {
               return 1;
            }
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }

      return 0;
   }
}
