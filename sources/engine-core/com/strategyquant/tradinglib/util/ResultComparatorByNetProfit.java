package com.strategyquant.tradinglib.util;

import com.strategyquant.tradinglib.ResultsGroup;
import java.util.Comparator;

public class ResultComparatorByNetProfit implements Comparator<ResultsGroup> {
   public int compare(ResultsGroup var1, ResultsGroup var2) {
      try {
         double var3 = var1.portfolio().stats((byte)0, (byte)10, (byte)10).getDouble("NetProfit");
         double var5 = var2.portfolio().stats((byte)0, (byte)10, (byte)10).getDouble("NetProfit");
         if (var3 < var5) {
            return 1;
         }

         if (var3 > var5) {
            return -1;
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }

      return 0;
   }
}
