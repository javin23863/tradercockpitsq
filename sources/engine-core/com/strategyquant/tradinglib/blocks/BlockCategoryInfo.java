package com.strategyquant.tradinglib.blocks;

public class BlockCategoryInfo {
   public int categoryNameHash;
   public int categoryOrder;
   public String categoryName;

   public BlockCategoryInfo(String var1, int var2) {
      this.categoryName = var1;
      this.categoryNameHash = var1.hashCode();
      this.categoryOrder = var2;
   }
}
