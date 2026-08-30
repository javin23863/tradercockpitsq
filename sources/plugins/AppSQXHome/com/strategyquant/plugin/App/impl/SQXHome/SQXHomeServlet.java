package com.strategyquant.plugin.App.impl.SQXHome;

import com.strategyquant.lib.L;
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

public class SQXHomeServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SQXHomeServlet.class);
   private static final String LINK = "https://cdn.strategyquant.com/public/sqxhome/";

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "link":
            return this.onLink(var2);
         default:
            throw new Exception(L.t("Unknown command '%s'.", new Object[]{var1}));
      }
   }

   private String onLink(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = "";
      V571hfnsHw var4 = MainApp.v571hfnsHw();
      JSONObject var5 = new JSONObject();
      var5.put("license", var4.eHvc2JguAd());
      var5.put("version", MainApp.getAppVersion());
      var5.put("hardwareid", var4.nmFllxIfvN());
      var5.put("isTrial", var4.yIifbIrWD3());
      var5.put("trialLeftSec", var4.aIpB57erT2());
      var5.put("isSpecialTrial", var4.isSpecialTrial());
      var5.put("type", var4.ljYePnIhcR());
      var5.put("edition", var4.printEdition());
      var5.put("futlab", var4.a1wUchdumV());
      var5.put("isSQBR", MainApp.isBrazilianEdition());
      String var6 = Base64.encodeBase64String(var5.toString().getBytes());
      long var7 = System.currentTimeMillis();
      boolean var9 = this.isOnline();
      if (var9) {
         var3 = "https://cdn.strategyquant.com/public/sqxhome/?d=" + var6 + "&t=" + var7;
      } else {
         var3 = BrowserGUI.getInstance().getAppUrl() + "/plugins/AppSQXHome/offline/offline/index.html?d=" + var6 + "&t=" + var7;
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
         JsonUtils.doGet("https://cdn.strategyquant.com/public/sqxhome/");
         return true;
      } catch (Exception | Error var2) {
         return false;
      }
   }
}
