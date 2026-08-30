package com.strategyquant.webguilib;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.app.webserver.MainAppWebServer;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.webguilib.server.JettyServer;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Electron {
   public static final Logger Log = LoggerFactory.getLogger(Electron.class);
   private static Electron instance;
   private DefaultExecutor executor = null;

   public static synchronized Electron getInstance() {
      if (instance == null) {
         instance = new Electron();
      }

      return instance;
   }

   public void start() {
      Log.info("Starting Electron ...");
      OperatingSystem var1 = new OperatingSystem();

      try {
         String var2 = "electron/StrategyQuantX";
         if (MainApp.checkProduct("QDM")) {
            var2 = "electron/QuantDataManager";
            if (!MainApp.isRelease()) {
               var2 = "electron-qdm/QuantDataManager";
            }
         }

         DefaultExecuteResultHandler var3 = new DefaultExecuteResultHandler();
         this.executor = new DefaultExecutor();
         ExecuteWatchdog var4 = new ExecuteWatchdog(-1L);
         this.executor.setWatchdog(var4);
         int var5 = JettyServer.getBrowserToken();
         String var6 = this.getIcon(var1);
         String var7 = MainApp.getProduct() + " " + MainApp.getProductName(true) + "  " + var6 + " " + MainAppWebServer.port + " " + var5;
         CommandLine var8;
         if (var1.isWindows()) {
            String var9 = MainApp.getDataPath() + "internal/" + var2 + "_ui.exe";
            var8 = new CommandLine("cmd");
            var8.addArgument("/c");
            var8.addArgument("start");
            var8.addArgument("\"\"", false);
            var8.addArgument(var9);
            var8.addArgument(MainApp.getProduct());
            var8.addArgument(MainApp.getProductName(true));
            var8.addArgument(var6);
            var8.addArgument(String.valueOf(MainAppWebServer.port));
            var8.addArgument(String.valueOf(var5));
         } else if (var1.isUnix()) {
            String var12 = MainApp.getDataPath() + "internal/" + var2.toLowerCase() + "_ui";
            var8 = new CommandLine(var12);
            var8.addArgument("--no-sandbox");
            var8.addArgument("--disable-setuid-sandbox");
            var8.addArgument(MainApp.getProduct());
            var8.addArgument(MainApp.getProductName(true));
            var8.addArgument(var6);
            var8.addArgument(String.valueOf(MainAppWebServer.port));
            var8.addArgument(String.valueOf(var5));
         } else {
            if (!var1.isMac()) {
               throw new Exception("OS not recognized!");
            }

            String var13 = MainApp.getDataPath() + "internal/" + var2 + "_ui.app";
            String var10 = "open -na \"" + var13.replace("\"", "\\\"") + "\" --args " + var7;
            var8 = CommandLine.parse(var10);
         }

         Log.info("Running Electron " + var8.toString());
         this.executor.execute(var8, var3);
      } catch (Exception var11) {
         Log.error("Failed to run Electron.", var11);
      }
   }

   private String getIcon(OperatingSystem var1) {
      if (MainApp.checkProduct("SQEDITOR")) {
         return var1.isUnix() ? "ce.png" : "ce.ico";
      } else if (MainApp.checkProduct("QDM")) {
         return var1.isUnix() ? "qdm.png" : "qdm.ico";
      } else {
         return var1.isUnix() ? "sq.png" : "sq.ico";
      }
   }

   public void exit() {
      Log.info("Exit electron");
      if (MainApp.electronWS != null) {
         MainApp.electronWS.sendMessage("exit", new JSONObject());
      }
   }

   public void loadUrl(String var1) {
      Log.info("Loading url: " + var1);
      if (MainApp.electronWS == null) {
         Log.error("Failed to load url, browser is not ready.");
      } else {
         MainApp.electronWS.sendMessage("loadUrl", new JSONObject().put("url", var1));
      }
   }

   public void loadFile(String var1) {
      Log.info("Loading file: " + var1);
      if (MainApp.electronWS == null) {
         Log.error("Failed to load file, browser is not ready.");
      } else {
         MainApp.electronWS.sendMessage("loadFile", new JSONObject().put("file", var1));
      }
   }

   public void setTitle(String var1) {
      Log.info("Set title: " + var1);
      if (MainApp.electronWS == null) {
         Log.error("Failed to set title, browser is not ready.");
      } else {
         MainApp.electronWS.sendMessage("setTitle", new JSONObject().put("title", var1));
      }
   }

   public void openDevTools(boolean var1) {
      if (var1) {
         Log.info("Open dev tools: " + var1);
         if (MainApp.electronWS == null) {
            Log.error("Failed to open dev tools, browser is not ready.");
         } else {
            MainApp.electronWS.sendMessage("openDevTools", new JSONObject().put("openDevTools", var1));
         }
      }
   }

   public void maximize() {
      if (MainApp.electronWS != null) {
         MainApp.electronWS.sendMessage("maximize", new JSONObject());
      }
   }

   public void minimize() {
      if (MainApp.electronWS != null) {
         MainApp.electronWS.sendMessage("minimize", new JSONObject());
      }
   }

   public boolean isReady() {
      return MainApp.electronWS != null;
   }

   public boolean waitingForElectron() {
      try {
         for (int var1 = 0; var1 < 15; var1++) {
            if (getInstance().isReady()) {
               Log.info("Electron is ready");
               return true;
            }

            Thread.sleep(1000L);
            Log.info("Waiting for Electron ...");
         }

         throw new Exception("Electron is not running.");
      } catch (Exception var2) {
         Log.error("Error: " + var2.getMessage());
         MainApp.exitApp();
         return false;
      }
   }
}
