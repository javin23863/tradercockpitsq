package com.strategyquant.webguilib;

import com.strategyquant.lib.L;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.app.SQSplashScreen;
import com.strategyquant.lib.volumeProfile.VolumeProfileSubscription;
import com.strategyquant.tradinglib.project.StrategiesSaver;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.webguilib.config.RemoteAccessConfig;
import com.strategyquant.webguilib.license.LicenseDialog;
import com.strategyquant.webguilib.server.AbstractUIWebServer;
import java.io.File;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserGUI {
   public static final Logger Log = LoggerFactory.getLogger(BrowserGUI.class);
   public static final Logger LogBrowserConsole = LoggerFactory.getLogger("BrowserConsole");
   private boolean appLoaded = false;
   private boolean fullscreen;
   private static BrowserGUI instance;
   private boolean debugconsole;

   public static BrowserGUI getInstance() {
      return instance;
   }

   public BrowserGUI(String var1) {
      Log.info("Initializing browser ...");
      instance = this;
      this.debugconsole = new File(MainApp.getDataPath() + "/debugconsole.txt").exists();
      Electron.getInstance().start();
      boolean var2 = Electron.getInstance().waitingForElectron();
      if (var2) {
         this.fullscreen = Boolean.parseBoolean(MainApp.settings().get("fullscreen", "true"));
         if (!this.fullscreen) {
            this.setBounds();
         }

         this.fixFullScreenSize();
         Electron.getInstance().openDevTools(this.debugconsole);
         Log.info("UI visible");
      }
   }

   private void fixFullScreenSize() {
      if (this.fullscreen) {
         Log.error("going FULLSCREEN");
      } else {
         Log.error("going NORMAL");
         this.setBounds();
      }
   }

   public void showLicenseLoadingScreen() {
      SQSplashScreen var1 = MainApp.getSplashScreen();
      if (var1 != null) {
         var1.hide();
      }

      Log.info("Showing loading dialog");
      LicenseDialog.getInstance().showLoadingScreen();
   }

   public void loadLicenseDialogForm() {
      SQSplashScreen var1 = MainApp.getSplashScreen();
      if (var1 != null) {
         var1.hide();
      }

      Log.info("Showing license form");
      LicenseDialog.getInstance().loadLicensePage();
      LicenseDialog.getInstance().waitUntilChecked();
      LicenseDialog.getInstance().showLoadingScreen();
   }

   public static void notifyUILicenseChanged(Boolean var0) {
      V571hfnsHw var1 = MainApp.v571hfnsHw();
      boolean var2;
      boolean var3;
      if (MainApp.checkProduct("QDM")) {
         var2 = var1.tsPZHq7yG3();
         var3 = false;
      } else {
         var2 = var1.gWfGtoRYJG() && var1.gDmtOfRJBr();
         var3 = MainApp.v571hfnsHw().aDm88fRJB2();
      }

      JSONObject var4 = new JSONObject()
         .put("email", var1.fSECzwVwpK())
         .put("code", var1.eHvc2JguAd())
         .put("isProVersion", var2)
         .put("isUltimateVersion", var3)
         .put("isTrial", var1.yIifbIrWD3())
         .put("isSpecialTrial", var1.bDm88fRJA3())
         .put("sq4bAllowed", !var1.a1wUchdumV())
         .put("volumeProfile", VolumeProfileSubscription.toJSON());
      if (var0 && MainApp.checkProduct("QDM") && V571hfnsHw.reasonVerifFailed != null) {
         var4.put("reasonLicVerifFailed", V571hfnsHw.reasonVerifFailed);
         var4.put("codeVerifFailed", V571hfnsHw.codeVerifFailed);
      }

      SQWebSocketManager.addToDataQueue(new DataToSend("LicenseInfo", var4), new String[]{"SQUANT", "QDM", "HOME"});
   }

   public void init(String var1) {
      this.appLoaded = false;
      this.setTitle(var1);
      String var2 = this.getAppUrl() + "/?nocache=" + System.currentTimeMillis();
      Electron.getInstance().loadUrl(var2);
   }

   private void onSaveAction() {
      Log.info("Save action triggered");
      int var1 = MainApp.awaitUserConfirmation(
         L.t("Saving strategies", new Object[0]), L.t("Save action triggered (CTRL+SHIFT+S). Do you want to save strategies into files?", new Object[0])
      );
      if (var1 == 1) {
         StrategiesSaver.saveAll(false, "Save action triggered (CTRL+SHIFT+S)");
         Log.info("Strategies saved");
      }
   }

   public void reload() {
      Log.info("Reloading browser...");
      String var1 = this.getAppUrl() + "/?nocache=" + System.currentTimeMillis();
      Log.debug("Opening url:" + var1);
      Electron.getInstance().loadUrl(var1);
      this.appLoaded = true;
   }

   public void onAppLoaded() {
      this.appLoaded = true;
      Log.info("APP LOADED");
      RemoteAccessConfig.getInstance().enableRemoteAccess();
   }

   public boolean isAppLoaded() {
      return this.appLoaded;
   }

   public static void clearCache() {
      if (getInstance() != null) {
         ;
      }
   }

   public String getAppUrl() {
      String var1;
      if (AbstractUIWebServer.useHttps) {
         var1 = "https://";
      } else {
         var1 = "http://";
      }

      return var1 + "localhost:" + AbstractUIWebServer.port;
   }

   private void setBounds() {
   }

   public void destroy() {
      Electron.getInstance().exit();
   }

   public static void showErrorDialog(String var0, String var1) {
      MainApp.showErrorDialog(var0, var1);
   }

   public boolean isFullscreen() {
      return this.fullscreen;
   }

   public void setTitle(String var1) {
      Electron.getInstance().setTitle(var1);
   }
}
