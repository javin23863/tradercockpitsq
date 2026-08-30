package com.strategyquant.plugin.Home.impl.About;

import com.strategyquant.lib.L88OaFjjon.Tu5UdlpFO9;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.historyData.HistoryDataSubscription;
import com.strategyquant.lib.volumeProfile.VolumeProfileSubscription;
import com.strategyquant.webguilib.BrowserGUI;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AboutServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(AboutServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "license":
            return this.onLicense();
         case "updateLicense":
            return this.onUpdateLicense(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onUpdateLicense(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"code"});
      String var2 = ((String[])var1.get("code"))[0];
      JSONObject var3 = new JSONObject();

      try {
         MainApp.rsq3UErJhC(var2);
         V571hfnsHw var4 = MainApp.v571hfnsHw();
         V571hfnsHw.reasonVerifFailed = null;
         var3.put("updateResult", "License OK");
         var3.put("updateSuccess", true);
         if (MainApp.checkProduct("QDM")) {
            Tu5UdlpFO9.pzi7BBRxNg(var4.eHvc2JguAd());
         }

         HistoryDataSubscription.getInstance().update();
         VolumeProfileSubscription.getInstance().update();
         BrowserGUI.notifyUILicenseChanged(false);
      } catch (Exception var5) {
         Log.error("Exc.", var5);
         V571hfnsHw.reasonVerifFailed = var5.getMessage();
         var3.put("updateResult", var5.getMessage());
         var3.put("updateSuccess", false);
      }

      this.getLicenseInfo(var3);
      return var3.toString();
   }

   private String onLicense() {
      JSONObject var1 = new JSONObject();
      this.getLicenseInfo(var1);
      return var1.toString();
   }

   private JSONObject getLicenseInfo(JSONObject var1) {
      V571hfnsHw var2 = MainApp.v571hfnsHw();
      if (MainApp.checkProduct("QDM")) {
         var1.put("product", "QuantDataManager");
         var1.put("version", var2.tsPZHq7yG3() ? " Pro version" : " Free version");
         var1.put("hardwareid", var2.nmFllxIfvN());
         var1.put("code", var2.eHvc2JguAd());
         var1.put("owner", var2.oOkoIeUsMG());
         var1.put("type", var2.printType(var2.eHvc2JguAd()));
      } else {
         var1.put("product", "StrategyQuant");
         var1.put("version", MainApp.printAppVersion(var2.printEdition(), true));
         var1.put("hardwareid", var2.nmFllxIfvN());
         var1.put("code", var2.eHvc2JguAd());
         var1.put("email", var2.fSECzwVwpK());
         String var3 = var2.oOkoIeUsMG();
         if (var2.aVwpAOwrF1()) {
            var3 = var3 + " - " + var2.fSECzwVwpK();
         }

         var1.put("owner", var3);
         var1.put("type", var2.printType(var2.eHvc2JguAd()));
      }

      var1.put("volumeProfile", VolumeProfileSubscription.toJSON());
      var1.put("success", "ok");
      return var1;
   }
}
