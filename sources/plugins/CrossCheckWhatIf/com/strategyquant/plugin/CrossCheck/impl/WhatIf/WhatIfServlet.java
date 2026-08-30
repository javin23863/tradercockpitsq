package com.strategyquant.plugin.CrossCheck.impl.WhatIf;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.WhatIf;
import com.strategyquant.tradinglib.whatif.WhatIfMethodsList;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhatIfServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(WhatIfServlet.class);

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
      JSONArray var3 = new JSONArray();
      List var4 = WhatIfMethodsList.get().getAvailableClasses();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         WhatIf var6 = (WhatIf)var4.get(var5);
         JSONObject var7 = new JSONObject();
         JSONArray var8 = new JSONArray();
         var8.put(false);
         var8.put(var6.printFormatedName());
         JSONObject var9 = new JSONObject();
         var9.put("display", var6.getFormatedName());
         var9.put("config", XMLUtil.elementToString(var6.getXML()));
         var7.put("userdata", var9);
         var7.put("id", var6.getClass().getSimpleName());
         var7.put("data", var8);
         var3.put(var7);
      }

      var2.put("rows", var3);
      var2.put("success", L.t("Methods listed.", new Object[0]));
      return var2.toString();
   }
}
