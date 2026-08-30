package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class Symbol extends AbstractItem {
   public static final String Key = "Symbol";
   public static final String Header = "Symbol";

   public Symbol() {
      super("Symbol", "Symbol", "Symbol");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return var1;
   }

   @Override
   public AbstractItem clone() {
      return new Symbol();
   }
}
