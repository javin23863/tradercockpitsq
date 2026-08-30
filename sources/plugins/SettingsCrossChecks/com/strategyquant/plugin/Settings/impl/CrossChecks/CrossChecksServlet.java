package com.strategyquant.plugin.Settings.impl.CrossChecks;

import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossChecksServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(CrossChecksServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      String var4 = var1;
      byte var5 = -1;
      var4.hashCode();
      switch (var5) {
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   public JSONArray list() throws Exception {
      JSONArray var1 = new JSONArray();

      for (ICrossCheck var3 : SQPluginManager.getPlugins(ICrossCheck.class)) {
         JSONObject var4 = new JSONObject();
         var4.put("use", true);
         var4.put("type", var3.getType());
         var4.put("title", var3.getName());
         var4.put("desc", var3.getDescription());
         var4.put("name", var3.getSettingName());
         var4.put("position", var3.getPreferredPosition());
         var4.put("forEngine", var3.forEngine());
         var4.put("disabledForSpecialTrial", var3.disabledForSpecialTrial());
         var1.put(var4);
      }

      return var1;
   }
}
