package com.strategyquant.plugin.CodeEditor.impl.ImportExport;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionManager {
   private static final Logger Log = LoggerFactory.getLogger(ExtensionManager.class);
   private static final byte STATE_DEFAULT = 0;
   private static final byte STATE_OVERWRITE = 1;
   private static final byte STATE_OVERWRITEALWAYS = 2;
   private static final byte STATE_SKIP = 3;
   private static final byte STATE_SKIPALWAYS = 4;
   private static final byte STATE_CANCEL = 5;
   private static volatile byte state = 0;
   private static Thread waitingThread = null;
   private static JSONObject confirmObj = new JSONObject();
   private static DataToSend dataToSend = new DataToSend("overwriteConfirmation", confirmObj);
   public static HashMap<String, String> notImportedMap = new HashMap<>();

   public static ArrayList<String> install(String[] var0) throws Exception {
      state = 0;
      notImportedMap.clear();
      String var1 = new File(MainApp.getDataPath() + "user").getAbsolutePath();
      ArrayList var2 = new ArrayList();

      for (int var3 = 0; var3 < var0.length; var3++) {
         try {
            String var4 = var0[var3];
            if (!var4.toLowerCase().endsWith(".sxp")) {
               throw new Exception(L.t("Expected file with extension .sxp", new Object[0]));
            }

            ZipInputStream var5 = null;

            try {
               var5 = new ZipInputStream(new FileInputStream(var4));
               ZipEntry var6 = null;

               while ((var6 = var5.getNextEntry()) != null) {
                  if (!var6.isDirectory()) {
                     File var7 = new File(var1 + "/" + var6.getName());
                     File var8 = new File(SQStructure.INTERNAL_DIR_PATH + var6.getName());
                     if (var8.exists()) {
                        notImportedMap.put(var6.getName(), L.t("already exists in Builtin", new Object[0]));
                     } else if (var7.exists()) {
                        switch (state) {
                           case 4:
                              notImportedMap.put(var6.getName(), L.t("skipped by user", new Object[0]));
                              break;
                           default:
                              sendConfirmation(var7.getAbsolutePath());
                              waitForUserAction();
                              switch (state) {
                                 case 3:
                                 case 4:
                                    notImportedMap.put(var6.getName(), L.t("skipped by user", new Object[0]));
                                    continue;
                                 case 5:
                                    throw new Exception(L.t("Canceled", new Object[0]));
                              }
                           case 2:
                              writeToFile(var7, var5);
                              var2.add(var6.getName());
                        }
                     } else {
                        writeToFile(var7, var5);
                        var2.add(var6.getName());
                     }
                  }
               }
            } finally {
               if (var5 != null) {
                  var5.close();
               }
            }
         } catch (Exception var13) {
            Log.error("Installing extension failed", var13);
            throw var13;
         }
      }

      return var2;
   }

   public static void export(String[] var0, String var1) throws Exception {
      File var2 = new File(var1);
      ZipOutputStream var3 = null;

      try {
         var3 = new ZipOutputStream(new FileOutputStream(var2));

         for (int var4 = 0; var4 < var0.length; var4++) {
            String var5 = SQUtils.trimFilePath(var0[var4], MainApp.getDataPath());
            String var6 = MainApp.getDataPath() + var5;
            if (var5.startsWith("user/")) {
               var5 = var5.replaceFirst("user/", "");
            }

            ZipEntry var7 = new ZipEntry(var5);
            var3.putNextEntry(var7);
            byte[] var8 = SQUtils.fileToBytes(new File(var6));
            var3.write(var8, 0, var8.length);
            var3.closeEntry();
         }
      } finally {
         if (var3 != null) {
            var3.close();
         }
      }
   }

   public static void overwrite() {
      state = 1;
   }

   public static void overwriteAlways() {
      state = 2;
   }

   public static void skip() {
      state = 3;
   }

   public static void skipAlways() {
      state = 4;
   }

   public static void cancelImport() {
      state = 5;
   }

   private static void writeToFile(File var0, ZipInputStream var1) throws Exception {
      if (var0.isDirectory()) {
         if (!var0.exists() && !var0.mkdirs()) {
            throw new Exception(L.t("Cannot create dir %s", new Object[]{var0.getAbsolutePath()}));
         }
      } else if (!var0.getParentFile().exists() && !var0.getParentFile().mkdirs()) {
         throw new Exception(L.t("Cannot create dir %s", new Object[]{var0.getParentFile().getAbsolutePath()}));
      }

      FileOutputStream var2 = null;

      try {
         var2 = new FileOutputStream(var0);
         byte[] var3 = new byte[1024];
         int var4 = 0;

         while ((var4 = var1.read(var3)) > 0) {
            var2.write(var3, 0, var4);
         }
      } finally {
         var1.closeEntry();
         if (var2 != null) {
            var2.close();
         }
      }
   }

   private static void sendConfirmation(String var0) {
      state = 0;
      confirmObj.put("filePath", var0);
      SQWebSocketManager.addToDataQueue(dataToSend, new String[]{"SQEDITOR"});
   }

   private static void waitForUserAction() throws Exception {
      if (waitingThread != null && waitingThread.isAlive()) {
         throw new Exception(L.t("Waiting for user action. Cannot continue until submitted", new Object[0]));
      }

      waitingThread = new Thread() {
         @Override
         public void run() {
            long var1 = 0L;

            while (ExtensionManager.state == 0) {
               try {
                  Thread.sleep(200L);
                  var1 += 200L;
               } catch (Exception var4) {
               }

               if (var1 > 60000L) {
                  ExtensionManager.state = 5;
               }
            }
         }
      };
      waitingThread.start();
      waitingThread.join();
   }
}
