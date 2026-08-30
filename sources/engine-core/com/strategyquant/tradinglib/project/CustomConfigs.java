package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.file.SQFileManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomConfigs {
   private static final Logger Log = LoggerFactory.getLogger(CustomConfigs.class);
   private static final String NameAttr = "name=\"";

   public static HashMap<String, String> list() {
      HashMap var0 = new HashMap();

      try {
         File var1 = new File(SQPaths.configsDirPath);
         if (!var1.exists()) {
            var1.mkdirs();
         } else {
            loadFiles(var1, var0);
         }
      } catch (Exception var2) {
         Log.error("Error while listing custom configs", var2);
      }

      return var0;
   }

   private static void loadFiles(File var0, HashMap<String, String> var1) {
      char[] var2 = new char[500];

      for (File var6 : var0.listFiles()) {
         if (var6.isDirectory()) {
            loadFiles(var6, var1);
         } else {
            String var7 = var6.getAbsolutePath().replace("\\", "/");
            var7 = var7.replace(SQPaths.configsDirPath.replace("\\", "/"), "").substring(1);

            try {
               if (var7.endsWith("cfx")) {
                  String var22 = SQFileManager.loadFile(var6.getAbsolutePath());
                  String var23 = getNameFromConfig(var22);
                  if (var23 == null) {
                     Log.error("Cannot get name from CFX config");
                  } else if (!var23.isEmpty()) {
                     var1.put(var7, var23);
                  } else {
                     Log.info("Config file " + var6.getName() + " skipped. Name is empty");
                  }
               } else if (var7.endsWith(".xml")) {
                  BufferedReader var8 = null;

                  try {
                     var8 = new BufferedReader(new FileReader(var6));

                     for (int var9 = 0; var9 < 5; var9++) {
                        if (var8.read(var2) > 0) {
                           String var10 = new String(var2);
                           String var11 = getNameFromConfig(var10);
                           if (var11 != null) {
                              if (!var11.isEmpty()) {
                                 var1.put(var7, var11);
                              } else {
                                 Log.info("File " + var6.getName() + " skipped. Name is empty");
                              }
                              break;
                           }
                        }
                     }
                  } finally {
                     if (var8 != null) {
                        try {
                           var8.close();
                        } catch (Exception var18) {
                           Log.error("Unable to close reader", var18);
                        }
                     }
                  }
               }
            } catch (Exception var20) {
               Log.error("Cannot load config file '" + var6.getAbsolutePath() + "'. ", var20);
            }
         }
      }
   }

   private static String getNameFromConfig(String var0) {
      int var1 = var0.indexOf("name=\"");
      if (var1 <= 0) {
         return null;
      }

      int var2 = var1 + "name=\"".length();
      int var3 = var0.indexOf("\"", var2);
      return var0.substring(var2, var3);
   }
}
