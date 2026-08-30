package com.strategyquant.plugin.DataManager.impl.Home;

import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.webguilib.BrowserGUI;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import com.strategyquant.wizard.desktop.utils.JsonUtils;
import java.io.File;
import java.util.Map;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeServlet extends HttpJSONServlet {
   private static final long serialVersionUID = 1L;
   private static final Logger Log = LoggerFactory.getLogger(HomeServlet.class);
   private static final String LINK = "https://cdn.strategyquant.com/public/qdmhome";

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "link":
            return this.onLink(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onLink(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = "";
      boolean var4 = this.isOnline();
      if (var4) {
         V571hfnsHw var5 = MainApp.v571hfnsHw();
         String var6 = var5.eHvc2JguAd();
         var6 = var6 + "|" + (var5.tsPZHq7yG3() ? "paid" : "free");
         var6 = var6 + "|" + MainApp.getAppVersion();
         var3 = "https://cdn.strategyquant.com/public/qdmhome?d=" + Base64.encodeBase64String(var6.getBytes());
      } else {
         var3 = BrowserGUI.getInstance().getAppUrl() + "/plugins/DataManagerHome/offline/index.html";
      }

      var2.put("link", var3);
      var2.put("success", "Home link");
      return var2.toString();
   }

   private boolean isOnline() {
      if (new File("offline.txt").exists()) {
         return false;
      }

      try {
         JsonUtils.doGet("https://cdn.strategyquant.com/public/qdmhome");
         return true;
      } catch (Exception | Error var2) {
         return false;
      }
   }
}
