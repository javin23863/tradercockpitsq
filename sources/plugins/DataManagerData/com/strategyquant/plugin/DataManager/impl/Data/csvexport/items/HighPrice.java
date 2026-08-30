package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class HighPrice extends AbstractItem {
   public static final String Key = "High";
   public static final String Header = "High";

   public HighPrice() {
      super("High price", "High", "High");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.d(var2.high, var3);
   }

   @Override
   public AbstractItem clone() {
      return new HighPrice();
   }
}
