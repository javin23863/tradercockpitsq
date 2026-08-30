package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class AskPrice extends AbstractItem {
   public static final String Key = "Ask";
   public static final String Header = "Ask";

   public AskPrice() {
      super("Ask price", "Ask", "Ask");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.d(var2.ask, var3);
   }

   @Override
   public AbstractItem clone() {
      return new AskPrice();
   }
}
