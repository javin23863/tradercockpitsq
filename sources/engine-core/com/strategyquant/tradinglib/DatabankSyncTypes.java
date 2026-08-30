package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.task.settings.buildmode.JSONAble;

public class DatabankSyncTypes extends JSONAble {
   public static final String IMMEDIATELY = "Immediately (not recommended, very slow)";
   public static final String NEVER = "Auto-sync never";
   public static final String EVERY_10_MINUTES = "Auto-sync every 10 minutes";
   public static final String EVERY_20_MINUTES = "Auto-sync every 20 minutes";
   public static final String EVERY_30_MINUTES = "Auto-sync every 30 minutes";
   public static final String EVERY_1_HOUR = "Auto-sync every 1 hour";
   public static final String EVERY_2_HOURS = "Auto-sync every 2 hours";
   public static final String EVERY_4_HOURS = "Auto-sync every 4 hours";

   public static int getTimeout(String var0) throws Exception {
      switch (var0) {
         case "Immediately (not recommended, very slow)":
            return 0;
         case "Auto-sync never":
            return -1;
         case "Auto-sync every 10 minutes":
            return 600000;
         case "Auto-sync every 20 minutes":
            return 1200000;
         case "Auto-sync every 30 minutes":
            return 1800000;
         case "Auto-sync every 1 hour":
            return 3600000;
         case "Auto-sync every 2 hours":
            return 7200000;
         case "Auto-sync every 4 hours":
            return 14400000;
         default:
            throw new Exception(L.t("Uknown type '%s'", new Object[]{var0}));
      }
   }
}
