package com.strategyquant.tradinglib.databank;

import org.json.JSONArray;
import org.json.JSONObject;

public class DatabankChange {
   public static final String ADD = "add";
   public static final String REMOVE = "remove";
   public static final String UPDATE = "update";
   public static final String RESET = "reset";
   public static final String CLEARALL = "clearAll";
   public static final String CONST_REFRESH_GRID = "refreshGrid";
   String type = "";
   String id;
   Object[] data = new Object[500];
   String extraValue;
   long time;

   public void update(String var1, String var2, Object[] var3) {
      this.type = var1;
      this.id = var2;
      this.time = System.currentTimeMillis();

      for (int var4 = 0; var4 < this.data.length; var4++) {
         if (var3 != null && var4 < var3.length) {
            this.data[var4] = var3[var4];
         } else {
            this.data[var4] = null;
         }
      }
   }

   public DatabankChange setExtraValue(String var1) {
      this.extraValue = var1;
      return this;
   }

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("type", this.type);
      var1.put("id", this.id);
      var1.put("time", this.time);
      var1.put("extraValue", this.extraValue);
      JSONArray var2 = new JSONArray();
      if (this.data != null) {
         for (int var3 = 0; var3 < this.data.length; var3++) {
            Object var4 = this.data[var3];
            if (var4 == null) {
               break;
            }

            try {
               var2.put(var4.toString());
            } catch (Exception var6) {
               var2.put("N/A");
            }
         }
      }

      var1.put("data", var2);
      return var1;
   }
}
