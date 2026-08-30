package com.strategyquant.datalib.data;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataFolderSweeper {
   public static final Logger Log = LoggerFactory.getLogger(DataFolderSweeper.class);
   protected static final long DELAY_TIME = 180000L;
   private static DataFolderSweeper instance = new DataFolderSweeper();
   private Thread cleanerThread;
   private boolean skip = false;

   public static DataFolderSweeper get() {
      return instance;
   }

   private DataFolderSweeper() {
   }

   public synchronized void init() {
      if (!MainApp.runInConsole()) {
         if (this.cleanerThread != null) {
            throw new IllegalStateException("DataFolderSweeper was already initialized");
         }

         this.cleanerThread = new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  ArrayList var1 = DataManager.list();
                  Thread.sleep(180000L);
                  long var2 = System.currentTimeMillis();
                  DataFolderSweeper.Log.info("Data folder sweep started");
                  DataFolderSweeper.this.removeUnusedData(var1);
                  DataFolderSweeper.this.removeCopyFiles("History", var1);
                  long var4 = System.currentTimeMillis() - var2;
                  DataFolderSweeper.Log.info("Data folder sweep finished in {}ms", var4);
               } catch (Exception var6) {
                  DataFolderSweeper.Log.error("Data folder sweep failed", var6);
               }
            }
         }, "DataFolderCleaner-thread");
         this.cleanerThread.start();
      }
   }

   public void setSkip(boolean var1) {
      this.skip = var1;
   }

   private void removeUnusedData(ArrayList<DataInfo> var1) {
      try {
         HashMap var2 = new HashMap();

         for (int var3 = 0; var3 < var1.size(); var3++) {
            DataInfo var4 = (DataInfo)var1.get(var3);
            List var5 = (List)var2.get(var4.connection);
            if (var5 == null) {
               var5 = new LinkedList();
               var2.put(var4.connection, var5);
            }

            var5.add(var4);
         }

         for (String var13 : var2.keySet()) {
            List var6 = (List)var2.get(var13);
            Map var7 = var6.stream()
               .filter(var0 -> var0.source == 6)
               .collect(Collectors.toMap(var0 -> DataManager.fixFilename(var0.symbol), var0 -> (DataInfo)var0));
            Map var8 = var6.stream()
               .filter(var0 -> var0.source == 5)
               .collect(Collectors.toMap(var0 -> DataManager.fixFilename(var0.symbol), var0 -> (DataInfo)var0));
            Map var9 = var6.stream()
               .filter(var0 -> var0.source != 5 && var0.source != 6)
               .collect(Collectors.toMap(var0 -> DataManager.fixFilename(var0.symbol), var0 -> (DataInfo)var0));
            this.removeUnusedInFolder(new File(DataManager.get()._getDirectory(), var13), var9, var8, var7);
         }
      } catch (Exception var10) {
         Log.error("Cannot remove unused symbol files. ", var10);
      }
   }

   private void removeUnusedInFolder(File var1, Map<String, DataInfo> var2, Map<String, DataInfo> var3, Map<String, DataInfo> var4) {
      this.removeUnusedInFolder(var1, var2);

      for (char var5 = 'A'; var5 <= 'Z'; var5++) {
         if (this.skip) {
            return;
         }

         File var6 = new File(var1, "sq_futures/" + var5);
         File var7 = new File(var1, "sq_equity/" + var5);
         if (!this.removeUnusedInFolder(var6, var4)) {
            this.deleteFolder(var6);
         }

         if (!this.removeUnusedInFolder(var7, var3)) {
            this.deleteFolder(var7);
         }
      }
   }

   private boolean removeUnusedInFolder(File var1, Map<String, DataInfo> var2) {
      File[] var3 = var1.listFiles(new FileFilter() {
         @Override
         public boolean accept(File var1) {
            return var1.isDirectory();
         }
      });
      if (var3 == null) {
         return false;
      }

      boolean var4 = false;

      for (File var8 : var3) {
         if (this.skip) {
            return true;
         }

         String var9 = var8.getName();
         if (!var9.equals("sq_equity") && !var9.equals("sq_futures")) {
            DataInfo var10 = (DataInfo)var2.get(var9);
            if (var10 == null) {
               this.deleteFolder(var8);
            } else {
               var4 = true;
            }
         }
      }

      return var4;
   }

   private void deleteFolder(File var1) {
      if (Files.exists(var1.toPath())) {
         File[] var2 = var1.listFiles();
         if (var2 != null) {
            for (File var6 : var2) {
               var6.delete();
            }
         }

         var1.delete();
      }
   }

   private void removeCopyFiles(String var1, ArrayList<DataInfo> var2) throws IOException {
      for (DataInfo var4 : var2) {
         if (this.skip) {
            return;
         }

         String var5 = DataManager.getDataFileName(var1, var4.symbol, var4.timeframe, "No Session");
         String var6 = var5 + ".copy";
         File var7 = new File(var6);
         Files.deleteIfExists(var7.toPath());
      }
   }
}
