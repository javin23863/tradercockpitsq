package com.strategyquant.tradinglib.task.settings.buildmode;

import java.lang.reflect.Field;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JSONAble {
   private static final Logger Log = LoggerFactory.getLogger(JSONAble.class);

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();

      for (Field var5 : this.getClass().getDeclaredFields()) {
         try {
            var1.put(this.getCamelName(var5.getName()), var5.get(this));
         } catch (Exception var7) {
            Log.error("Cannot get value of field '" + var5.getName() + "' of class '" + this.getClass().getName() + "'");
         }
      }

      return var1;
   }

   private String getCamelName(String var1) {
      String[] var2 = var1.split("_");
      String var3 = "";

      for (int var4 = 0; var4 < var2.length; var4++) {
         String var5 = var2[var4];
         if (var4 == 0) {
            var3 = var3 + var5.toLowerCase();
         } else {
            var3 = var3 + var5.toUpperCase().substring(0, 1);
            var3 = var3 + var5.toLowerCase().substring(1);
         }
      }

      return var3;
   }
}
