package com.strategyquant.tradinglib.databank;

import org.json.JSONObject;

public class DatabankColumnTypes {
   public static final byte GENERAL = 0;
   public static final byte ORIGINAL = 10;
   public static final byte WF = 30;
   public static final byte WF_STABILITY = 31;
   public static final byte WF_SCORE = 32;
   public static final byte WF_SPECIAL = 33;
   public static final String WF_TEXT = "WF";
   public static final String WF_STABILITY_TEXT = "WF Stability of";
   public static final String WF_SCORE_TEXT = "WF Score of";
   public static final String WF_SPECIAL_TEXT = "WF Special - ";
   public static final String CATEGORY_ORIGINAL = "Original";
   public static final String CATEGORY_RT = "RT";
   public static final String CATEGORY_WF = "WF";

   public static JSONObject toJSONObject() {
      JSONObject var0 = new JSONObject();
      var0.put("general", new JSONObject().put("value", 0).put("category", "General").put("dataType", "default"));
      var0.put("original", new JSONObject().put("value", 10).put("category", "Original").put("dataType", "default"));
      var0.put("wf", new JSONObject().put("value", 30).put("category", "WF").put("dataType", "default"));
      var0.put("wfStability", new JSONObject().put("value", 31).put("category", "WF Stability").put("dataType", "Decimal2Pct"));
      var0.put("wfScore", new JSONObject().put("value", 32).put("category", "WF Score").put("dataType", "Decimal2Pct"));
      var0.put("wfSpecial", new JSONObject().put("value", 33).put("category", "WF Special").put("dataType", "default"));
      return var0;
   }

   public static String typeToString(byte var0) {
      switch (var0) {
         case 0:
            return "General";
         case 10:
            return "Original";
         case 30:
            return "WF";
         case 31:
            return "WF Stability of";
         case 32:
            return "WF Score of";
         case 33:
            return "WF Special - ";
         default:
            return "?";
      }
   }
}
