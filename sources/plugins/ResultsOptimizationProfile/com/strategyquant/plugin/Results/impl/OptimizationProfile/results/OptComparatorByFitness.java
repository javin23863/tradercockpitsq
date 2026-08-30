package com.strategyquant.plugin.Results.impl.OptimizationProfile.results;

import com.strategyquant.tradinglib.optimization.OptimizationTestResult;
import java.util.Comparator;

public class OptComparatorByFitness implements Comparator<OptimizationTestResult> {
   public int compare(OptimizationTestResult var1, OptimizationTestResult var2) {
      if (var1.stats.getDouble("Fitness") < var2.stats.getDouble("Fitness")) {
         return 1;
      } else {
         return var1.stats.getDouble("Fitness") > var2.stats.getDouble("Fitness") ? -1 : 0;
      }
   }
}
