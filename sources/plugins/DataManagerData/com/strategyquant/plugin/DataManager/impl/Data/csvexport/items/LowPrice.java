package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class LowPrice extends AbstractItem {
   public static final String Key = "Low";
   public static final String Header = "Low";

   public LowPrice() {
      super("Low price", "Low", "Low");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.d(var2.low, var3);
   }

   @Override
   public AbstractItem clone() {
      return new LowPrice();
   }
}
