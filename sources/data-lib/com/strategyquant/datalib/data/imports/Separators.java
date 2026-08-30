package com.strategyquant.datalib.data.imports;

import org.json.JSONArray;
import org.json.JSONObject;

public class Separators {
   public static final String TYPE_COMMA = "COMMA";
   public static final String VALUE_COMMA = ",";
   public static final String TYPE_SEMICOLON = "SEMICOLON";
   public static final String VALUE_SEMICOLON = ";";
   public static final String TYPE_TAB = "TAB";
   public static final String VALUE_TAB = "\t";
   public static final String TYPE_SPACE = "SPACE";
   public static final String VALUE_SPACE = " ";
   private static final String[] types = new String[]{"COMMA", "SEMICOLON", "TAB", "SPACE"};
   private static final String[] values = new String[]{",", ";", "\t", " "};

   public static String[] listTypes() {
      return types;
   }

   public static String[] listValues() {
      return values;
   }

   public static String getValue(String var0) {
      for (int var1 = 0; var1 < types.length; var1++) {
         if (types[var1].equals(var0)) {
            return values[var1];
         }
      }

      return var0;
   }

   public static String getType(String var0) {
      for (int var1 = 0; var1 < values.length; var1++) {
         if (values[var1].equals(var0)) {
            return types[var1];
         }
      }

      return var0;
   }

   public static JSONArray toJSON() {
      JSONArray var0 = new JSONArray();

      for (int var1 = 0; var1 < types.length; var1++) {
         var0.put(new JSONObject().put("type", types[var1]).put("value", values[var1]));
      }

      return var0;
   }
}
