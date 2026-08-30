package com.strategyquant.datalib.dataseries;

import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongListCache {
   public static final Logger Log = LoggerFactory.getLogger("LongListCache");
   private static final ILongValuesList[] cache = new ILongValuesList[100];
   private static int initedTotal = 0;
   private static int fromCache = 0;
   private static int clearedTotal = 0;
   private static int clearedToCache = 0;
   private static int maxInCache = 0;
   private static int notSavedToCache = 0;

   public LongListCache() {
      TimerTask var1 = new TimerTask() {
         @Override
         public void run() {
            LongListCache.printStats();
         }
      };
      Timer var2 = new Timer("Timer");
      long var3 = 3000L;
      long var5 = 3000L;
      var2.scheduleAtFixedRate(var1, var3, var5);
   }

   protected static void printStats() {
      synchronized (cache) {
         int var1 = computeInCache();
         Log.info(
            "initedTotal: {}, from cache: {}, clearedTotal: {}, clearedToCache: {}, notSavedToCache: {}, in cache: {}, max in cache: {}",
            new Object[]{initedTotal, fromCache, clearedTotal, clearedToCache, notSavedToCache, var1, maxInCache}
         );
      }
   }

   private static int computeInCache() {
      int var0 = 0;

      for (int var1 = 0; var1 < cache.length; var1++) {
         if (cache[var1] != null) {
            var0++;
         }
      }

      return var0;
   }

   public static void resetStats() {
      synchronized (cache) {
         initedTotal = 0;
         fromCache = 0;
         clearedTotal = 0;
         clearedToCache = 0;
         notSavedToCache = 0;
      }
   }

   public static void clear(ILongValuesList var0) {
      var0.clear();
      clearedTotal++;
      synchronized (cache) {
         for (int var2 = 0; var2 < cache.length; var2++) {
            if (cache[var2] == null) {
               clearedToCache++;
               cache[var2] = var0;
               break;
            }
         }
      }
   }

   public static ILongValuesList get(int var0) {
      initedTotal++;
      synchronized (cache) {
         for (int var3 = 0; var3 < cache.length; var3++) {
            if (cache[var3] != null) {
               ILongValuesList var1 = cache[var3];
               cache[var3] = null;
               fromCache++;
               return var1;
            }
         }
      }

      return new HeapLongValuesList(var0);
   }
}
