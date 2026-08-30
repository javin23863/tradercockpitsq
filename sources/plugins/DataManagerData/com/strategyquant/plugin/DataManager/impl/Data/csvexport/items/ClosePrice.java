package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class ClosePrice extends AbstractItem {
   public static final String Key = "Close";
   public static final String Header = "Close";

   public ClosePrice() {
      super("Close price", "Close", "Close");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.d(var2.close, var3);
   }

   @Override
   public AbstractItem clone() {
      return new ClosePrice();
   }
}
