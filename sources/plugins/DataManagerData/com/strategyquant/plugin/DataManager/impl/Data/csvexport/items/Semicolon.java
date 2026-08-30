package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class Semicolon extends AbstractItem {
   public static final String Key = ";";
   public static final String Header = ";";

   public Semicolon() {
      super("Semicolon", ";", ";");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return ";";
   }

   @Override
   public AbstractItem clone() {
      return new Semicolon();
   }
}
