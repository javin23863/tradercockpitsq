package com.strategyquant.tradinglib.results.stats.comparator;

import com.strategyquant.tradinglib.Order;
import java.util.Comparator;

public class OrderComparatorByTicket implements Comparator<Order> {
   public int compare(Order var1, Order var2) {
      if (var1.Ticket < var2.Ticket) {
         return -1;
      } else {
         return var1.Ticket > var2.Ticket ? 1 : 0;
      }
   }
}
