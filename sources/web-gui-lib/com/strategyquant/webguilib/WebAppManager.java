package com.strategyquant.webguilib;

import com.jfx.ts.io.PSUtils;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.webguilib.init.PortChecker;
import com.strategyquant.webguilib.server.JettyServer;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAppManager {
   private static final Logger Log = LoggerFactory.getLogger(WebAppManager.class);
   private static Map<String, String> appExecNames = new HashMap<>();
   private static Map<String, String> appNames = new HashMap<>();
   private static Map<String, Integer> appPorts = new HashMap<>();
   private static Map<String, String> traderApps = new LinkedHashMap<>();
   private static Map<String, String> quantApps = new LinkedHashMap<>();
   private static LinkedHashMap<Integer, String> portMap = new LinkedHashMap<>();
   private static WebAppManager instance;
   private WebServer webServer;

   public WebAppManager(WebServer var1) {
      this.webServer = var1;
      appNames.put("QDM", "QuantDataManager");
      appNames.put("SQMANAGER", "Manager");
      appNames.put("SQTRADER", "Trader");
      appNames.put("SQEDITOR", "Editor");
      appNames.put("SQUANT_OLD", "StrategyQuant");
      appNames.put("SQUANT", "StrategyQuant X");
      appNames.put("SQWIZARD", "SQWizard");
      appExecNames.put("SQTRADER", "SQTrader");
      appExecNames.put("SQUANT_OLD", "StrategyQuant");
      appExecNames.put("SQMANAGER", "SQManager");
      appExecNames.put("SQEDITOR", "QuantEditor");
      appExecNames.put("SQWIZARD", "SQWizard");
      traderApps.put("SQMANAGER", "Manager");
      traderApps.put("SQEDITOR", "Editor");
      quantApps.put("SQMANAGER", "Manager");
      quantApps.put("SQEDITOR", "Editor");
      quantApps.put("SQWIZARD", "Wizard");
      quantApps.put("SQTRADER", "Trader");
      instance = this;
   }

   public void startApp(String var1) throws Exception {
      if (!MainApp.isMultiInstance() && this.isAppRunning(var1)) {
         String var6 = getBaseURL(appPorts.get(var1)) + "/main/tofront";
         String var7 = SQUtils.httpGet(var6);
         Log.info(var7);
      } else {
         Log.info("Starting app " + var1);
         String var2 = MainApp.getDataPath() + appExecNames.get(var1);
         String var3;
         if (MainApp.isRelease()) {
            String var4 = "";
            if (MainApp.is32BitVersion()) {
               var4 = "_32";
            }

            var2 = var2 + var4 + ".exe";
            var3 = var2;
         } else {
            var2 = var2 + ".jar";
            var3 = "java -jar " + var2;
         }

         File var8 = new File(var2);
         if (!var8.exists()) {
            throw new Exception(L.t("The application was not found. Path: %s", new Object[]{var2}));
         }

         Log.info("Exec: " + var3);
         PSUtils.getInstance().startProcess(var3, MainApp.getDataPath(), false, false);
         Log.info("App should be started.");
      }
   }

   private void mapPorts() {
      portMap = PortChecker.mapPorts(this.webServer.getPortFrom(), this.webServer.getPortTo());
      appPorts.clear();

      for (int var2 : portMap.keySet()) {
         if (portMap.get(var2) != null) {
            appPorts.put(portMap.get(var2), var2);
         }
      }
   }

   public int getFreePort() {
      for (int var2 : portMap.keySet()) {
         if (portMap.get(var2).equals("Free")) {
            return var2;
         }
      }

      return -1;
   }

   public boolean isAppRunning(String var1) {
      for (int var3 : portMap.keySet()) {
         if (portMap.get(var3).equals(var1)) {
            Log.debug("Application " + var1 + " is already running on port " + var3);
            return true;
         }
      }

      Log.debug("Application " + var1 + " is not running.");
      return false;
   }

   public static String getBaseURL(int var0) {
      String var1 = JettyServer.useHttps ? "https://" : "http://";
      return var1 + "localhost:" + var0;
   }

   public String findActiveWebSocket() {
      return null;
   }

   public Map<String, String> getApps() {
      switch (MainApp.getProduct()) {
         case "SQUANT_OLD":
            return quantApps;
         case "SQTRADER":
            return traderApps;
         default:
            return null;
      }
   }

   public String getAppName() {
      String var1 = MainApp.getProduct();
      return appNames.get(var1);
   }

   public static Map<String, Integer> getAppPorts() {
      return appPorts;
   }
}
