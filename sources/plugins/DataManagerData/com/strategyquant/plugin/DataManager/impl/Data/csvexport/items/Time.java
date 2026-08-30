package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;
import org.joda.time.format.DateTimeFormat;

public class Time extends AbstractItem {
   public static final String Key = "Time";
   public static final String Header = "Time";

   public Time() {
      super("Time", "Time", "Time");
      this.formatter = DateTimeFormat.forPattern("HH:mm:ss");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.formatter.print(var2.time);
   }

   @Override
   public AbstractItem clone() {
      return new Time();
   }

   @Override
   public void setFormat(String var1) {
      this.formatter = DateTimeFormat.forPattern(var1);
   }
}
