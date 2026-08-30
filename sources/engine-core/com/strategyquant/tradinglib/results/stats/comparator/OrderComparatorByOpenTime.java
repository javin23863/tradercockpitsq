package com.strategyquant.tradinglib.results.stats.comparator;

import com.strategyquant.tradinglib.Order;
import java.util.Comparator;

public class OrderComparatorByOpenTime implements Comparator<Order> {
   public int compare(Order var1, Order var2) {
      if (var1.OpenTime < var2.OpenTime) {
         return -1;
      } else {
         return var1.OpenTime > var2.OpenTime ? 1 : 0;
      }
   }
}
