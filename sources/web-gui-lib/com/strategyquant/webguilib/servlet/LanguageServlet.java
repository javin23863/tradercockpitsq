package com.strategyquant.webguilib.servlet;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanguageServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(LanguageServlet.class);

   @Override
   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.getLanguages();
         case "getLanguage":
            return this.getLanguage(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command: %s", new Object[]{var1}), null);
      }
   }

   private String getLanguage(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParamValue(var1, "language");
      JSONObject var3 = new JSONObject();
      JSONArray var4 = new JSONArray();

      try {
         if (!L.isAvailable(var2)) {
            Log.info("Language '" + var2 + "' is not available.");
            var2 = "English";
         }

         L.loadLangFileToMap(var2);

         for (String var6 : L.translations.keySet()) {
            JSONArray var7 = new JSONArray();
            var7.put(var6);
            var7.put(L.translations.get(var6));
            var4.put(var7);
         }

         if (var2.contains("Chinese")) {
            JSONArray var9 = new JSONArray();
            var9.put("linfo");
            var9.put(
               new String(
                  Base64.decodeBase64(
                     "5Lqa5rSy5Zyw5Yy6IOS4reaWh+WfueiureWQiOS9nOS8meS8tCBTdGV2ZSBDaG91LiBFLW1haWw6IDx1PnN0ZXZlY2hvdUBzdHJhdGVneXF1YW50LmhrPC91Pg=="
                  ),
                  "UTF-8"
               )
            );
            var4.put(var9);
         }

         MainApp.settings().set("language", var2);
         var3.put("success", true);
         var3.put("translations", var4);
         var3.put("font", L.getFont());
         Log.debug("Selected language: " + var2);
         return var3.toString();
      } catch (Exception var8) {
         return apiErrorJSON(null, var8);
      }
   }

   private String getLanguages() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      String var3 = MainApp.settings().get("language");

      for (String var5 : L.langList) {
         JSONObject var6 = new JSONObject();
         String var7 = "";
         if (var3 != null && var5.equals(var3)) {
            var6.put("active", true);
         }

         var6.put("title", var5);
         if (L.flagsMap.containsKey(var5)) {
            var7 = (String)L.flagsMap.get(var5);
         }

         var6.put("flag", var7);
         var2.put(var6);
      }

      var1.put("success", true);
      var1.put("langs", var2);
      return var1.toString();
   }
}
