package com.strategyquant.datalib.dataseries;

import com.strategyquant.lib.random.MersenneTwisterRng;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoubleListCache {
   public static final Logger Log = LoggerFactory.getLogger("DoubleListCache");
   private static final ObjectArrayList<IDoubleValuesList> cache = new ObjectArrayList();
   protected static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
   protected static final int MEGABYTE = 1048576;
   private static boolean tooBigCacheMemory = false;
   private static boolean tooMuchMemoryUsed = false;
   private static long lastUsedTime = 0L;
   private static long lastClearedTime = 0L;
   private static int maxCacheSize = 0;
   private static long totalGet = 0L;
   private static long totalClear = 0L;
   private static long totalAllocated = 0L;
   private static long totalDeallocated = 0L;
   private static long totalGetFromCache = 0L;
   private static long totalReturnedToCache = 0L;
   private static long maxElementsInCache = 0L;
   private static int totalObjectsCreated = 0;
   private static final AtomicInteger objectsWaitingForAllocation = new AtomicInteger(0);
   private static final MersenneTwisterRng rng = new MersenneTwisterRng();
   private static long maxJavaMemory;
   private static int timerCount = 0;

   public DoubleListCache() {
      TimerTask var1 = new TimerTask() {
         @Override
         public void run() {
            DoubleListCache.printStats();
         }
      };
      Timer var2 = new Timer("Timer");
      long var3 = 20000L;
      long var5 = 5000L;
      var2.scheduleAtFixedRate(var1, var3, var5);
      maxJavaMemory = memoryBean.getHeapMemoryUsage().getMax() / 1048576L;
      maxCacheSize = (int)(maxJavaMemory / 1026L) * 100;
      Log.debug("Max memory detected: {} MB, max cache size set to: {}", maxJavaMemory, maxCacheSize);
   }

   protected static void printStats() {
      synchronized (cache) {
         long var1 = 0L;
         if (lastUsedTime > 0L) {
            long var3 = (System.currentTimeMillis() - lastUsedTime) / 1000L;
            long var5 = (System.currentTimeMillis() - lastClearedTime) / 1000L;
            if (cache.size() > 0) {
               int var7 = 0;
               if (var3 > 300L) {
                  var7 = (int)(cache.size() * 0.2);
               } else if (var5 > 300L && tooMuchMemoryUsed) {
                  var7 = (int)(cache.size() * 0.1);
               }

               if (var7 > 5) {
                  for (int var8 = 0; var8 < var7; var8++) {
                     IDoubleValuesList var9 = (IDoubleValuesList)cache.pop();
                     var9.release();
                  }
               }
            }
         }

         tooMuchMemoryUsed = false;
         MemoryUsage var14 = memoryBean.getHeapMemoryUsage();
         double var4 = var14.getUsed() / 1048576L;
         double var6 = var14.getCommitted() / 1048576L;
         double var15 = var4 / var6;
         double var10 = var6 / maxJavaMemory;
         if (var15 > 0.75 && var10 > 0.8) {
            tooMuchMemoryUsed = true;
         }

         tooBigCacheMemory = false;
      }
   }

   public static void resetStats() {
      synchronized (cache) {
         ;
      }
   }

   public static void clear(IDoubleValuesList var0) {
      synchronized (cache) {
         totalClear++;
         boolean var2 = false;
         String var3 = "";
         if (cache.size() < maxCacheSize && !tooBigCacheMemory && !tooMuchMemoryUsed) {
            var2 = true;
         } else {
            if (cache.size() >= maxCacheSize) {
               var3 = "max cache size";
            }

            if (tooBigCacheMemory) {
               var3 = "tooBigCacheMemory";
            }

            if (tooMuchMemoryUsed) {
               var3 = "tooMuchMemoryUsed";
            }
         }

         String var4 = "";
         if (var2) {
            var0.clear();
            totalReturnedToCache++;
            cache.push(var0);
            maxElementsInCache = Math.max(cache.size(), maxElementsInCache);
         } else {
            totalDeallocated++;
            var0.release();
         }
      }
   }

   public static IDoubleValuesList get(int var0) {
      totalGet++;
      String var2 = "";
      IDoubleValuesList var1 = getFromCache();
      if (var1 != null) {
         totalGetFromCache++;
         return var1;
      } else {
         totalAllocated++;
         var2 = "";
         return new HeapDoubleValuesList(var0);
      }
   }

   private static IDoubleValuesList getFromCache() {
      synchronized (cache) {
         if (!cache.isEmpty()) {
            lastUsedTime = System.currentTimeMillis();
            return (IDoubleValuesList)cache.pop();
         } else {
            return null;
         }
      }
   }

   public static int getWaitingForAllocationObjects() {
      return objectsWaitingForAllocation.get();
   }
}
