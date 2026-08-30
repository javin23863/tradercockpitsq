package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQUtils;

public class Spread extends AbstractItem {
   public static final String Key = "Spread";
   public static final String Header = "Spread";

   public Spread() {
      super("Spread", "Spread", "Spread");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      double var4 = Math.abs(var2.ask - var2.bid);
      return String.valueOf(SQUtils.round(var4, var3));
   }

   @Override
   public AbstractItem clone() {
      return new Spread();
   }
}
