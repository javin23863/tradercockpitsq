package com.strategyquant.gridlib.utils;

import com.strategyquant.lib.memory.CpuInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreUsagesEvaluator {
   private static final Logger LOGGER = LoggerFactory.getLogger(CoreUsagesEvaluator.class);

   public static int getCores(String var0) {
      int var1 = CpuInfo.getAvailableProcessors();
      var0 = var0.trim();

      try {
         int var10 = Integer.parseInt(var0);
         if (var10 < 0) {
            LOGGER.debug("Using all cores but reserving:" + -var10 + " cores");
            var10 += var1;
         }

         var10 = getSafeCountOFCores(var10, var1);
         LOGGER.debug("Total cores available: {}, using: {}", var1, var10);
         return var10;
      } catch (Exception var7) {
         if (var0.endsWith("%")) {
            var0 = var0.substring(0, var0.length() - 1);

            try {
               int var9 = Integer.parseInt(var0);
               double var3 = (float)var1 * var9 / 100.0;
               int var5 = (int)var3;
               LOGGER.debug("Using percentage of cores:" + var9 + "%. Total cores:" + var1 + ". Cores for usage:" + var5);
               var5 = getSafeCountOFCores(var5, var1);
               LOGGER.debug("Total cores available: {}, using: {}", var1, var5);
               return var5;
            } catch (Exception var6) {
            }
         }

         LOGGER.debug("Invalid core usage pattern:" + var0 + ". Using all available cores-1");
         int var2 = getSafeCountOFCores(var1 - 1, var1);
         LOGGER.debug("Total cores available: {}, using: {}", var1, var2);
         return var2;
      }
   }

   private static int getSafeCountOFCores(int var0, int var1) {
      if (var0 < 1) {
         LOGGER.debug("Number of cores is too small. Using 1 core.");
         var0 = 1;
      }

      if (var0 > var1) {
         LOGGER.debug("Number of cores is higher than available cores. Using " + var1 + " core.");
         var0 = var1;
      }

      return var0;
   }
}
