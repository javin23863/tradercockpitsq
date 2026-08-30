package com.strategyquant.datalib.metatrader4;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class Mt4SymbolProperties {
   private Map<String, String> values = new HashMap<>();

   public String getStrValue(String var1) {
      String var2 = this.values.get(var1.toLowerCase());
      if (var2 != null) {
         return var2;
      } else {
         throw new IllegalStateException("record with key:" + var1 + " was not found");
      }
   }

   public boolean exists(String var1) {
      return this.values.get(var1) != null;
   }

   public int getIntValue(String var1) {
      return Integer.parseInt(this.getStrValue(var1));
   }

   public double getDoubleValue(String var1) {
      return Double.parseDouble(this.getStrValue(var1));
   }

   public void put(String var1, String var2) {
      this.values.put(var1, var2);
   }

   public void parse(String var1) {
      String[] var2 = var1.split("\\|");

      for (String var6 : var2) {
         String[] var7 = var6.split("=");
         if (var7.length == 2) {
            this.put(var7[0], var7[1]);
         }
      }

      this.checkParams();
   }

   private void checkParams() {
      if (this.getDoubleValue("lot_step") == 0.0) {
         this.put("lot_step", "0.01");
      }
   }

   public JSONArray toJSON() {
      JSONArray var1 = new JSONArray();

      for (String var3 : this.values.keySet()) {
         String var4 = this.values.get(var3);
         var1.put(new JSONObject().put(var3, var4));
      }

      return var1;
   }

   @Override
   public String toString() {
      String var1 = "";
      int var2 = 0;

      for (String var4 : this.values.keySet()) {
         String var5 = this.values.get(var4);
         var1 = var1 + (var2 > 0 ? "|" + var4 + "=" + var5 : var4 + "=" + var5);
         var2++;
      }

      return var1;
   }
}
