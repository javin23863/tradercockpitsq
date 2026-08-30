package com.strategyquant.tradinglib.optimization;

import com.strategyquant.tradinglib.ResultsGroup;
import java.util.Comparator;

public class OrdersIndexComparator implements Comparator<ResultsGroup> {
   public int compare(ResultsGroup var1, ResultsGroup var2) {
      double var3 = var1.specialValues().getDouble("OptIndexFitness", 0.0);
      double var5 = var2.specialValues().getDouble("OptIndexFitness", 0.0);
      if (var3 < var5) {
         return -1;
      } else {
         return var3 > var5 ? 1 : 0;
      }
   }
}
