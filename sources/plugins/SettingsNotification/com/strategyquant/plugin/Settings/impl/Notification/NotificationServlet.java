package com.strategyquant.plugin.Settings.impl.Notification;

import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(NotificationServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      String var4 = var1;
      byte var5 = -1;
      var4.hashCode();
      switch (var5) {
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   public JSONArray getTypes() {
      JSONArray var1 = new JSONArray();
      var1.put(new JSONObject().put("key", "email").put("name", "Email"));
      var1.put(new JSONObject().put("key", "popup").put("name", "Show popup"));
      return var1;
   }
}
