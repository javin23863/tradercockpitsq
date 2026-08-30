package com.strategyquant.plugin.Servlet.impl.Wizard;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.DefaultServlet;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import com.strategyquant.wizard.desktop.Dispatcher;
import java.io.File;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WizardServlet extends DefaultServlet {
   private static final Logger Log = LoggerFactory.getLogger(WizardServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "loadLicense":
            return this.getLicenceInfo();
         case "getConfig":
            return this.onGetConfig(var2);
         default:
            Dispatcher var9 = new Dispatcher();
            String[] var10 = (String[])var2.get("param");
            String var6 = var10 != null && var10.length != 0 ? var10[0] : "";
            String var7 = var1 + "#;#" + var6;
            String var8 = var9.execute(var7);
            if (var8 == null) {
               var8 = "";
            }

            return var8;
      }
   }

   private String getLicenceInfo() {
      return "{\"password\":\"\",\"type\":\"C\",\"result\":\"success\",\"email\":\"" + MainApp.v571hfnsHw().fSECzwVwpK() + "\"}";
   }

   private String onGetConfig(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "sessionID")[0];
         var2.put("wizardConfig", SQUtils.fileToString(new File(SQPaths.wizardConfigPath)));
         String var4 = SQStructure.INTERNAL_DIR_PATH + "web/" + "AlgoWizard" + "/assets/InitialStrategy.xml";
         var2.put("initialStrategyConfig", SQUtils.fileToString(new File(var4)));
         Dispatcher var5 = new Dispatcher();
         var2.put("favoriteBlocks", var5.loadFavorites());
         var2.put("recentFiles", var5.loadRecentFiles(var3));
         var2.put("symbols", WSDataObjects.getData("", "list").getDataObject().get("data"));
         var2.put("timeframes", WSDataObjects.getTimeframes().getDataArray());
      } catch (Exception var6) {
         return HttpJSONServlet.apiErrorJSON(L.t("Cannot get wizard config.", new Object[0]), var6);
      }

      var2.put("success", L.t("Config loaded.", new Object[0]));
      return var2.toString();
   }
}
