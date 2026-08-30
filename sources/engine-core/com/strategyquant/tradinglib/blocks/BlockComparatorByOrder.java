package com.strategyquant.tradinglib.blocks;

import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.SortOrder;
import java.util.Comparator;

public class BlockComparatorByOrder implements Comparator<IBlock> {
   public int compare(IBlock var1, IBlock var2) {
      int var3 = this.getOrder(var1);
      int var4 = this.getOrder(var2);
      if (var3 > var4) {
         return 1;
      } else {
         return var3 < var4 ? -1 : 0;
      }
   }

   private int getOrder(IBlock var1) {
      Class var2 = var1.getClass();
      SortOrder var3 = var2.getAnnotation(SortOrder.class);
      return var3 == null ? 100 : var3.value();
   }
}
