package com.strategyquant.tradinglib.blocks;

import java.util.Comparator;

public class CategoryComparatorByOrder implements Comparator<BlockCategoryInfo> {
   public int compare(BlockCategoryInfo var1, BlockCategoryInfo var2) {
      if (var1.categoryOrder > var2.categoryOrder) {
         return 1;
      } else {
         return var1.categoryOrder < var2.categoryOrder ? -1 : 0;
      }
   }
}
