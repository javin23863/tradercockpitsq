package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import org.json.JSONObject;

public class ValueTypes {
   public static final byte Maximize = 1;
   public static final byte Minimize = 2;
   public static final byte Aproximate = 3;

   public static String typeToString(byte var0) {
      switch (var0) {
         case 1:
            return L.t("Maximize", new Object[0]);
         case 2:
            return L.t("Minimize", new Object[0]);
         case 3:
            return L.t("Aproximate", new Object[0]);
         default:
            return "?";
      }
   }

   public static JSONObject getTypes() {
      JSONObject var0 = new JSONObject();
      var0.put("1", typeToString((byte)1));
      var0.put("2", typeToString((byte)2));
      var0.put("3", typeToString((byte)3));
      return var0;
   }
}
