package com.strategyquant.plugin.App.impl.QuantDataManager;

import com.strategyquant.lib.L;
import com.strategyquant.qdm.QDM;
import com.strategyquant.qdm.banners.Banner;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuantDataManagerServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(QuantDataManagerServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "bannerClicked":
            return this.onBannerClicked(var2);
         default:
            throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
      }
   }

   private synchronized String onBannerClicked(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"id"});
      int var2 = Integer.parseInt(((String[])var1.get("id"))[0]);
      boolean var3 = var1.containsKey("dismiss") && Boolean.parseBoolean(((String[])var1.get("dismiss"))[0]);
      Banner var4 = QDM.getInstance().banners.getBannerById(var2);
      if (var4.type.equals("popup")) {
         QuantDataManagerAppPlugin.timeOfLastDisplayedPopupBanner = System.currentTimeMillis();
      }

      if (!var3) {
         QDM.getInstance().banners.click(var2);
      }

      JSONObject var5 = new JSONObject();
      var5.put("success", "ok");
      return var5.toString();
   }
}
