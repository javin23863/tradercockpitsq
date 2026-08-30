package com.strategyquant.webguilib.license;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.webguilib.Electron;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LicenseDialog {
   public static final Logger Log = LoggerFactory.getLogger(LicenseDialog.class);
   private static LicenseDialog instance;
   private boolean isBrowserReady = true;

   public static synchronized LicenseDialog getInstance() {
      if (instance == null) {
         instance = new LicenseDialog();
      }

      return instance;
   }

   public void loadLicensePage() {
      String var1;
      if (MainApp.checkProduct("QDM")) {
         var1 = "file:///" + MainApp.getDataPath() + "/internal/web/app/licenseDialogQDM.html";
      } else if (MainApp.isBrazilianEdition()) {
         var1 = "file:///" + MainApp.getDataPath() + "/internal/web/app/licenseDialogBr.html";
      } else {
         var1 = "file:///" + MainApp.getDataPath() + "/internal/web/app/licenseDialog.html";
      }

      try {
         Electron.getInstance().loadFile(var1);
      } catch (Exception var3) {
         Log.error("Cannot load license page", var3);
      }
   }

   public void showLoadingScreen() {
      try {
         File var1 = new File(this.getLoadingScreenHTMLPath());
         Log.info("Path: " + var1.getAbsolutePath());
         String var2 = SQUtils.fileToString(var1);
         OperatingSystem var3 = new OperatingSystem();
         if (MainApp.isFirstRun() && var2.contains("sq-firewall-note hide")) {
            if (var3.isWindows()) {
               var2 = var2.replaceAll("sq-firewall-note hide", "sq-firewall-note");
            } else if (var3.isMac()) {
               var2 = var2.replaceAll("sq-firewall-note hide", "sq-firewall-note sq-firewall-note-mac");
            } else {
               var2 = var2.replaceAll("sq-firewall-note hide", "sq-firewall-note sq-firewall-note-linux");
            }
         }

         Log.info("Loading HTML in licencedialog");

         while (!this.isBrowserReady) {
            Thread.sleep(200L);
         }

         String var5 = "file:///" + var1.getAbsolutePath();
         Electron.getInstance().loadFile(var5);
      } catch (Exception var6) {
         Log.error("Cannot load loading screen html", var6);
      }
   }

   public void updateLoadingInfo(String var1) {
      if (!MainApp.runInConsole()) {
         if (MainApp.electronWS != null) {
            MainApp.sendMessage("loadingInfo", var1);
         }
      }
   }

   public void waitUntilChecked() {
      Thread var1 = new Thread() {
         @Override
         public void run() {
            while (!MainApp.license.licenseChecked) {
               try {
                  Thread.sleep(100L);
               } catch (Exception var2) {
               }
            }
         }
      };
      var1.start();

      try {
         var1.join();
      } catch (Exception var3) {
      }
   }

   private String getLoadingScreenHTMLPath() {
      String var1 = "loadingScreen.html";
      if (MainApp.checkProduct("SQEDITOR")) {
         var1 = "loadingScreenCE.html";
      } else if (MainApp.checkProduct("AlgoWizard")) {
         var1 = "loadingScreenAW.html";
      } else if (MainApp.checkProduct("QDM")) {
         var1 = "loadingScreenQDM.html";
      } else if (MainApp.isBrazilianEdition()) {
         var1 = "loadingScreenBr.html";
      }

      String var2 = SQStructure.INTERNAL_DIR_PATH + "web/app/layout/views/";
      return var2 + var1;
   }
}
