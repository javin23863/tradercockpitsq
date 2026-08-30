package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.SQUtils;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

public abstract class AbstractItem {
   public static final String FormatSeparator = ":";
   public String name = null;
   public String key = null;
   public String format = null;
   public String header = null;
   public String value = null;
   public DateTimeFormatter formatter;
   public int count = 1;

   public AbstractItem(String var1, String var2, String var3) {
      this.name = var1;
      this.key = var2;
      this.header = var3;
   }

   public JSONObject toJson() {
      return new JSONObject().put("name", this.name).put("key", this.key).put("format", this.format).put("header", this.header);
   }

   public static String format(String var0, String var1) {
      return var0 + ":" + var1;
   }

   public abstract String printValue(String var1, VersatileData var2, int var3);

   protected String d(double var1, int var3) {
      return SQUtils.d2String(var1, var3);
   }

   public abstract AbstractItem clone();

   public void setFormat(String var1) throws Exception {
      this.format = var1;
   }
}
