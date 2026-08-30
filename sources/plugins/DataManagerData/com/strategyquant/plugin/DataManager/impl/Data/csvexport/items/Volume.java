package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQUtils;

public class Volume extends AbstractItem {
   public static final String Key = "Volume";
   public static final String Header = "Volume";

   public Volume() {
      super("Volume", "Volume", "Volume");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return var2.volume > 1.0 ? SQUtils.roundLong(this.count > 1 ? var2.volume / this.count : var2.volume) + "" : "1";
   }

   @Override
   public AbstractItem clone() {
      return new Volume();
   }
}
