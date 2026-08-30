package com.strategyquant.tradinglib.util;

import com.strategyquant.tradinglib.ResultsGroup;
import java.util.Comparator;

public class ResultComparatorByName implements Comparator<ResultsGroup> {
   public int compare(ResultsGroup var1, ResultsGroup var2) {
      return var1.getName().compareTo(var2.getName());
   }
}
