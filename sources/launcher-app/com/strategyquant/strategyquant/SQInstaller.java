package com.strategyquant.strategyquant;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQInstaller {
   private static final Logger Log = LoggerFactory.getLogger(SQInstaller.class);
   private static final String ConfigUrl = "https://1859438673.rsc.cdn77.org/ninstall/sqxinstaller/updates.xml";
   private static final String VersionPath = SQStructure.INTERNAL_DIR_PATH + "SQI.dat";

   public void checkForUpdatesAsync() {
      new Thread(new Runnable() {
         @Override
         public void run() {
            SQInstaller.this.checkForUpdates();
         }
      }).start();
   }

   public void checkForUpdates() {
      try {
         Log.debug("Checking for new SQX Installer");
         String var1 = SQUtils.httpGet("https://1859438673.rsc.cdn77.org/ninstall/sqxinstaller/updates.xml");
         Element var2 = XMLUtil.stringToElement(var1);
         int var3 = XMLUtil.getIntAttr(var2, "version", 0);
         String var4 = var2.getAttributeValue("url");
         String var5 = var2.getAttributeValue("checksum");
         int var6 = this.getCurrentVersion();
         Log.debug("Installed version: " + var6);
         Log.debug("Available version: " + var3);
         if (var3 > var6) {
            Log.debug("Downloading new version of StrategyQuantX.exe");
            byte[] var7 = SQUtils.loadDataFromServer(var4);
            if (!var5.equals(SQUtils.md5CheckSum(var7))) {
               throw new Exception(L.t("Checksum doesn't match.", new Object[0]));
            }

            FileUtils.writeByteArrayToFile(new File(MainApp.getDataPath() + "StrategyQuantX.exe"), var7);
            SQUtils.stringToFile(VersionPath, var3 + "");
         }
      } catch (Exception var8) {
         Log.error("Error while checking for updates. Exc.", var8);
      }
   }

   private int getCurrentVersion() {
      int var1 = 1;
      File var2 = new File(VersionPath);
      if (var2.exists()) {
         try {
            String var3 = SQUtils.fileToString(var2);
            var1 = Integer.valueOf(var3);
         } catch (Exception var4) {
         }
      }

      return var1;
   }
}
