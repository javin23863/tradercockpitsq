package com.strategyquant.tradinglib;

import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.memory.MemoryUsageChecker;
import com.strategyquant.tradinglib.backtest.BacktestDataCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemoryCleaner {
   public static final Logger Log = LoggerFactory.getLogger(MemoryCleaner.class);
   public static final int NEVER = 0;
   public static final int EVERY_10_SECONDS = 10000;
   public static final int EVERY_20_SECONDS = 20000;
   public static final int EVERY_30_SECONDS = 30000;
   public static final int EVERY_45_SECONDS = 40000;
   public static final int EVERY_MINUTE = 60000;
   public static final int EVERY_2_MINUTES = 120000;
   public static final int EVERY_5_MINUTES = 300000;
   public static final int EVERY_10_MINUTES = 600000;
   public static final int EVERY_15_MINUTES = 900000;
   public static final int EVERY_30_MINUTES = 1800000;
   public static final int EVERY_HOUR = 3600000;
   private static final int memoryUsageActivationLimit = 80;
   private static final int autoCleaningInterval = 60000;
   private static Thread manualCleaningThread = null;
   private static long lastCleanTime = 0L;

   public static void init() {
      boolean var0 = Boolean.parseBoolean(MainApp.settings().get("memoryCleanup", "false"));
      int var1 = Integer.parseInt(MainApp.settings().get("memoryCleanupInterval", "0"));
      startAutomaticCleaningInterval();
      if (var0) {
         try {
            checkType(var1);
            startManualCleaningInterval(var1);
         } catch (Exception var3) {
            Log.error("Cannot setup memory cleanup interval", var3);
         }
      }
   }

   public static String cleanNow() {
      System.gc();
      lastCleanTime = System.currentTimeMillis();
      Log.debug("Memory cleaned");
      long var1 = SQGrid.getGridClient().countAllJobs(null);
      String var0;
      if (var1 == 0L) {
         long var3 = BacktestDataCache.getInstance().freeCache();
         if (var3 == 0L) {
            var0 = "Started Java memory cleanup.<br/>No other offheap cached data found.";
         } else {
            var0 = "Started Java memory cleanup.<br/>Cleaned also " + SQUtils.formatBytesToHumanFormat(var3) + " from cached offheap data.";
         }
      } else {
         var0 = "Started Java memory cleanup.<br/>No cached offheap data memory was freed because there are some jobs running.";
      }

      return var0;
   }

   public static void checkType(int var0) throws Exception {
      switch (var0) {
         case 0:
         case 10000:
         case 20000:
         case 30000:
         case 40000:
         case 60000:
         case 120000:
         case 300000:
         case 600000:
         case 900000:
         case 1800000:
         case 3600000:
            return;
         default:
            throw new Exception(L.t("Unknown cleaning period %d.", new Object[]{var0}));
      }
   }

   private static void startManualCleaningInterval(final int var0) {
      if (manualCleaningThread != null && manualCleaningThread.isAlive()) {
         manualCleaningThread.interrupt();
      }

      if (var0 != 0) {
         manualCleaningThread = new Thread() {
            @Override
            public void run() {
               while (true) {
                  try {
                     Thread.sleep(var0);
                  } catch (InterruptedException var2) {
                     break;
                  }

                  if (MainApp.AppLoaded) {
                     if (!MainApp.isExiting()) {
                        MemoryCleaner.cleanNow();
                        continue;
                     }
                     break;
                  }
               }
            }
         };
         manualCleaningThread.start();
      }
   }

   private static void startAutomaticCleaningInterval() {
      (new Thread() {
         @Override
         public void run() {
            while (true) {
               try {
                  Thread.sleep(60000L);
               } catch (InterruptedException var3) {
                  MemoryCleaner.Log.error("Automatic memory cleaning thread interrupted", var3);
                  break;
               }

               if (MainApp.isExiting()) {
                  break;
               }

               if (!(MemoryUsageChecker.getCurrentUsage() < 80.0)) {
                  long var1 = System.currentTimeMillis();
                  if (var1 - MemoryCleaner.lastCleanTime >= 60000L) {
                     MemoryCleaner.cleanNow();
                  }
               }
            }
         }
      }).start();
   }
}
