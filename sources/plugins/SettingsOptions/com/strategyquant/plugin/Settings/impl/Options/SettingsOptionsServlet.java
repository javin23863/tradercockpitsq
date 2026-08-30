package com.strategyquant.plugin.Settings.impl.Options;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsOptionsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SettingsOptionsServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "list":
            return this.onList(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onList(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      var2.put("parameters", TradingOptions.list());
      var2.put("success", L.t("Options listed.", new Object[0]));
      return var2.toString();
   }
}
