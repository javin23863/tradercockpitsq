package com.strategyquant.plugin.Servlet.impl.Builder;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.build.BuildTemplatesManager;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuilderServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(BuilderServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "getTemplate":
            return this.onGetBuildTemplate(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Incorrect url arguments '%s'.", new Object[]{var1}), null);
      }
   }

   private String onGetBuildTemplate(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("template"))[0];
         var2.put("template", XMLUtil.elementToString(BuildTemplatesManager.getTemplateXML(var3)));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot get build template.", new Object[0]), var4);
      }

      var2.put("success", L.t("Builder template returned.", new Object[0]));
      return var2.toString();
   }
}
