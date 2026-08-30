package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class Tab extends AbstractItem {
   public static final String Key = "Tab";
   public static final String Header = "[Tab]";

   public Tab() {
      super("Tab", "Tab", "[Tab]");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return "\t";
   }

   @Override
   public AbstractItem clone() {
      return new Tab();
   }
}
