package com.strategyquant.tradinglib.generator;

import com.strategyquant.tradinglib.Negater;
import com.strategyquant.tradinglib.SortOrder;
import java.util.Comparator;

public class NegaterComparatorByOrder implements Comparator<Negater> {
   public int compare(Negater var1, Negater var2) {
      int var3 = this.getOrder(var1);
      int var4 = this.getOrder(var2);
      if (var3 > var4) {
         return 1;
      } else {
         return var3 < var4 ? -1 : 0;
      }
   }

   private int getOrder(Negater var1) {
      Class var2 = var1.getClass();
      SortOrder var3 = var2.getAnnotation(SortOrder.class);
      return var3 == null ? 100 : var3.value();
   }
}
