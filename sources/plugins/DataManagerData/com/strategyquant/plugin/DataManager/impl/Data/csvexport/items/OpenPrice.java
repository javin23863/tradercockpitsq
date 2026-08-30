package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class OpenPrice extends AbstractItem {
   public static final String Key = "Open";
   public static final String Header = "Open";

   public OpenPrice() {
      super("Open price", "Open", "Open");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.d(var2.open, var3);
   }

   @Override
   public AbstractItem clone() {
      return new OpenPrice();
   }
}
