package com.strategyquant.tradinglib.results.stats.comparator;

import com.strategyquant.tradinglib.Order;
import java.util.Comparator;

public class OrderComparatorByCloseTime implements Comparator<Order> {
   public int compare(Order var1, Order var2) {
      if (var1.CloseTime < var2.CloseTime) {
         return -1;
      } else {
         return var1.CloseTime > var2.CloseTime ? 1 : 0;
      }
   }
}
