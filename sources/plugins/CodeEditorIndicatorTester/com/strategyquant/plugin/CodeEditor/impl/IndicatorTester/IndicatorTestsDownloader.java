package com.strategyquant.plugin.CodeEditor.impl.IndicatorTester;

import com.strategyquant.indicatorTester.IndicatorTesterConfig;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.utils.IProgressListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jdom2.Element;

public class IndicatorTestsDownloader {
   private static final String downloadURL = "http://autoupdate.strategyquant.com/indicator_data/indicator_tests.zip";
   private static final String targetFilePath = SQPaths.testsDirPath + "/tmp/indicator_tests.zip";
   private static Thread processingThread = null;

   public static void downloadTests(final IProgressListener var0) throws Exception {
      if (processingThread != null && processingThread.isAlive()) {
         throw new Exception("Download is already in progress");
      }

      processingThread = new Thread() {
         @Override
         public void run() {
            try {
               IndicatorTestsDownloader.downloadFile(var0);
               IndicatorTestsDownloader.copyTestFiles(var0);
               IndicatorTestsDownloader.updateTesterConfig(var0);
            } catch (Exception var2) {
               if (var0 != null) {
                  var0.onError(var2.getMessage());
               }
            }
         }
      };
      processingThread.start();
   }

   private static void downloadFile(IProgressListener var0) throws Exception {
      HttpURLConnection var1 = null;
      BufferedInputStream var2 = null;
      BufferedOutputStream var3 = null;
      if (var0 != null) {
         var0.setMessage(L.t("Downloading tests...", new Object[0]));
         var0.onProgress(0.0);
      }

      try {
         URL var4 = new URL("http://autoupdate.strategyquant.com/indicator_data/indicator_tests.zip");
         var1 = (HttpURLConnection)var4.openConnection();
         long var17 = var1.getContentLength();
         var2 = new BufferedInputStream(var1.getInputStream());
         SQUtils.createFile(targetFilePath);
         FileOutputStream var7 = new FileOutputStream(targetFilePath);
         var3 = new BufferedOutputStream(var7, 1024);
         byte[] var8 = new byte[1024];
         long var9 = 0L;
         int var11 = 0;

         while ((var11 = var2.read(var8, 0, 1024)) >= 0) {
            var9 += var11;
            double var12 = (double)var9 / var17 * 100.0;
            var12 = var12 < 100.0 ? var12 : 99.0;
            if (var0 != null) {
               var0.onProgress(var12);
            }

            var3.write(var8, 0, var11);
         }

         var3.close();
         var2.close();
      } catch (Exception var16) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (IOException var15) {
            }
         }

         if (var3 != null) {
            try {
               var3.close();
            } catch (IOException var14) {
            }
         }

         if (var1 != null) {
            var1.disconnect();
         }

         new File(targetFilePath).delete();
         String var5 = var16.getMessage();
         throw new Exception("Download failed. Reason: " + var5 != null ? var5 : "NullPointer exception");
      }
   }

   private static void copyTestFiles(IProgressListener var0) throws Exception {
      if (var0 != null) {
         var0.setMessage(L.t("Copying test files...", new Object[0]));
      }

      ZipFile var1 = null;
      FileOutputStream var2 = null;
      byte[] var3 = new byte[1024];

      try {
         File var4 = new File(SQPaths.indicatorTestsPath);
         if (!var4.exists()) {
            var4.mkdirs();
         }

         var1 = new ZipFile(targetFilePath);
         int var16 = var1.size();
         int var6 = 0;
         Enumeration var7 = var1.entries();

         while (var7.hasMoreElements()) {
            ZipEntry var8 = (ZipEntry)var7.nextElement();
            var6++;
            double var9 = (double)var6 / var16 * 100.0;
            var9 = var9 < 100.0 ? var9 : 99.0;
            if (var0 != null) {
               var0.onProgress(var9);
            }

            if (!var8.isDirectory()) {
               String var11 = var8.getName();
               File var12 = new File(SQPaths.indicatorTestsPath + File.separator + var11);
               new File(var12.getParent()).mkdirs();
               var2 = new FileOutputStream(var12);
               InputStream var13 = var1.getInputStream(var8);

               int var14;
               while ((var14 = var13.read(var3)) > 0) {
                  var2.write(var3, 0, var14);
               }

               var2.close();
            }
         }

         var1.close();
         new File(targetFilePath).delete();
      } catch (Exception var15) {
         if (var2 != null) {
            var2.close();
         }

         if (var1 != null) {
            var1.close();
         }

         String var5 = var15.getMessage();
         throw new Exception("Copying test files failed. Reason: " + var5 != null ? var5 : "NullPointer exception");
      }
   }

   private static void updateTesterConfig(IProgressListener var0) throws Exception {
      if (var0 != null) {
         var0.setMessage(L.t("Updating tester config...", new Object[0]));
         var0.onProgress(1.0);
      }

      String var1 = SQPaths.indicatorTestsPath + File.separator + "IndicatorTester.xml";

      try {
         Element var2 = XMLUtil.fileToXmlElement(new File(var1));
         IndicatorTesterConfig.save(var2, IndicatorTesterConfig.defaultConfigFilePath);
         if (var0 != null) {
            var0.onProgress(100.0);
            var0.onFinish();
         }

         new File(var1).delete();
      } catch (Exception var4) {
         new File(var1).delete();
         String var3 = var4.getMessage();
         throw new Exception("Updating Indicator Tester config failed. Reason: " + var3 != null ? var3 : "NullPointer exception");
      }
   }
}
