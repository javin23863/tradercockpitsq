package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import org.joda.time.format.DateTimeFormat;

public class Date extends AbstractItem {
   public static final String Key = "Date";
   public static final String Header = "Date";

   public Date() {
      super("Date", "Date", "Date");
      this.formatter = DateTimeFormat.forPattern("yyyyMMdd");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.formatter.print(var2.time);
   }

   @Override
   public AbstractItem clone() {
      return new Date();
   }

   @Override
   public void setFormat(String var1) throws Exception {
      try {
         this.formatter = DateTimeFormat.forPattern(var1);
      } catch (Exception var3) {
         throw new Exception(String.format(L.t("Invalid Date format '%s'", new Object[0]), var1));
      }
   }
}
