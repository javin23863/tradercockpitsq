package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.L;
import org.joda.time.format.DateTimeFormat;

public class DateTime extends AbstractItem {
   public static final String Key = "DateTime";
   public static final String Header = "DateTime";

   public DateTime() {
      super("Date time", "DateTime", "DateTime");
      this.formatter = DateTimeFormat.forPattern("yyyyMMdd HH:mm:ss");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.formatter.print(var2.time);
   }

   @Override
   public AbstractItem clone() {
      return new DateTime();
   }

   @Override
   public void setFormat(String var1) throws Exception {
      try {
         this.formatter = DateTimeFormat.forPattern(var1);
      } catch (Exception var3) {
         throw new Exception(String.format(L.t("Invalid DateTime format '%s'", new Object[0]), var1));
      }
   }
}
