package com.strategyquant.tradinglib.backtest;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestfilesUtils {
   public static final Logger Log = LoggerFactory.getLogger("TestfilesUtils");

   public static String getTestdataFile(int var0) {
      return getTestdataFile(var0, true);
   }

   public static String getTestdataFile(int var0, boolean var1) {
      StringBuilder var2 = new StringBuilder(MainApp.getDataPath());
      var2.append("/internal/testfiles");
      String var3 = Integer.toString(var0);
      int var4 = 0;
      int var5 = 0;

      while (true) {
         String var6 = var3.substring(var4, var4 + 1);
         var4++;
         if (!var6.equals("-")) {
            var5++;
            var2.append("/");
            var2.append(var6);
            if (var5 >= 2) {
               var6 = var2.toString();
               var2.append("/c");
               var2.append(var3);
               String var7 = var2.toString();
               if (var1) {
                  SQUtils.ensureDirExists(var6);
               }

               return var7;
            }
            continue;
         }
      }
   }

   public static void deleteTestingFiles() {
      File var0 = new File(MainApp.getDataPath() + "internal/testfiles");
      var0.mkdirs();
      Collection var1 = FileUtils.listFiles(var0, null, true);
      if (var1 != null) {
         TreeMap var2 = new TreeMap();
         long var3 = 0L;

         for (File var6 : var1) {
            if (var6.isFile()) {
               var3 += var6.length();
               long var7 = var6.lastModified();
               var2.put(var7, var6);
            }
         }

         if (var3 > 2000000000L && var2.size() > 20) {
            int var11 = var2.size() - 20;
            Set var12 = var2.keySet();
            Object[] var13 = var12.toArray();
            if (var11 > 0) {
               for (int var8 = 0; var8 < var11; var8++) {
                  int var9 = var2.size() - var8 - 1;
                  File var10 = (File)var2.get(var13[var9]);
                  if (var10 != null) {
                     if (var10.delete()) {
                        Log.debug("Test file '" + var10.getName() + "' was deleted!");
                     } else {
                        Log.debug("Test file '" + var10.getName() + "' delete failed!");
                     }
                  }
               }
            }
         }
      }
   }

   public static void deleteTempTestfile(String var0) {
      if (var0 != null) {
         StringBuilder var1 = new StringBuilder(MainApp.getDataPath());
         var1.append("/internal/testfiles/tmp/");
         var1.append(var0);
         String var2 = var1.toString();
         File var3 = new File(var2);
         if (var3.exists() && var3.isDirectory()) {
            try {
               FileUtils.deleteDirectory(var3);
            } catch (IOException var5) {
               Log.error("Cannot x delete folder '" + var2 + "'.", var5);
            }
         }
      }
   }
}
