package com.strategyquant.indicatorTester;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.concurrent.locks.StampedLock;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorTesterConfig {
   private static final Logger Log = LoggerFactory.getLogger(IndicatorTesterConfig.class);
   public static final String defaultConfigFileName = "IndicatorTester.xml";
   public static final String defaultConfigFilePath = SQPaths.settingsDirPath + "/IndicatorTesterConfigs/" + "IndicatorTester.xml";
   private static Element lastConfig;
   private static String lastConfigPath;
   private static StampedLock lock = new StampedLock();

   public static Element getConfig() {
      long var0 = lock.readLock();

      try {
         return lastConfig != null ? lastConfig : load(null);
      } finally {
         lock.unlock(var0);
      }
   }

   public static String getConfigPath() {
      return lastConfigPath;
   }

   public static void resetConfigPath() {
      long var0 = lock.writeLock();

      try {
         lastConfigPath = null;
      } finally {
         lock.unlock(var0);
      }
   }

   public static Element load(String var0) {
      if (var0 != null) {
         MainApp.settings().set("IndicatorTesterPath", var0);
         MainApp.settings().save();
      } else {
         var0 = MainApp.settings().get("IndicatorTesterPath");
      }

      if (var0 == null) {
         var0 = defaultConfigFilePath;
      }

      lastConfigPath = var0;
      File var1 = new File(var0);
      if (var1.exists()) {
         long var2 = lock.writeLock();

         try {
            lastConfig = XMLUtil.fileToXmlElement(var1);
            return lastConfig;
         } catch (Exception var8) {
            Log.error("Error while reading indicator tester config. ", var8);
         } finally {
            lock.unlock(var2);
         }
      }

      return createDefaultConfig("MetaTrader4", "300");
   }

   public static Element createDefaultConfig(String var0, String var1) {
      Element var2 = new Element("IndicatorTester");
      Element var3 = new Element("Engine");
      var3.setText(var0);
      Element var4 = new Element("BarsToReserve");
      var4.setText(var1);
      Element var5 = new Element("Tests");
      var2.addContent(var3);
      var2.addContent(var4);
      var2.addContent(var5);
      long var6 = lock.writeLock();

      try {
         lastConfig = var2;
      } finally {
         lock.unlock(var6);
      }

      lastConfigPath = null;
      return var2;
   }

   public static void save(Element var0, String var1) {
      if (var1 != null) {
         MainApp.settings().set("IndicatorTesterPath", var1);
         MainApp.settings().save();
         lastConfigPath = var1;
      } else {
         var1 = lastConfigPath;
      }

      if (var1 != null) {
         try {
            File var2 = new File(var1);
            XMLUtil.xmlToFile(var0 != null ? var0 : getConfig(), var2);
         } catch (Exception var8) {
            Log.error("Exc.", var8);
         }

         if (var0 != null) {
            long var9 = lock.writeLock();

            try {
               lastConfig = var0;
            } finally {
               lock.unlock(var9);
            }
         }
      }
   }

   public static Element createNewConfig(String[] var0) throws Exception {
      long var1 = lock.writeLock();

      try {
         Element var3 = XMLUtil.getChildElem(lastConfig, "Tests");

         for (String var7 : var0) {
            IndicatorInfo var8 = IndicatorsLoader.getIndyByName(var7);
            var3.addContent(var8.getXML(var7));
         }

         return lastConfig;
      } finally {
         lock.unlock(var1);
      }
   }

   public static void updateConfig(Element var0) {
      long var1 = lock.writeLock();

      try {
         lastConfig = var0;
      } finally {
         lock.unlock(var1);
      }
   }
}
