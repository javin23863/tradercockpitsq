package com.strategyquant.datalib.timeframe;

import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.TradingException;
import java.util.Comparator;

public class TimeframesComparator implements Comparator<String> {
   public int compare(String var1, String var2) {
      long var3;
      try {
         var3 = TimeframeManager.getTFHash(var1);
      } catch (TradingException var9) {
         var3 = 2147483647L;
      }

      long var5;
      try {
         var5 = TimeframeManager.getTFHash(var2);
      } catch (TradingException var8) {
         var5 = 2147483647L;
      }

      if (var3 > var5) {
         return 1;
      } else {
         return var3 < var5 ? -1 : 0;
      }
   }

   public static int getSecondsCount(String var0) {
      int var1 = 0;
      if (var0 == null || var0.isEmpty()) {
         return Integer.MAX_VALUE;
      }

      if (var0.equals("TICK")) {
         return 0;
      }

      try {
         var1 = Integer.parseInt(var0.substring(1).trim());
      } catch (Exception var3) {
         return Integer.MAX_VALUE;
      }

      if (var0.startsWith("S")) {
         return var1;
      } else if (var0.startsWith("M")) {
         return 60 * var1;
      } else {
         return var0.startsWith("H") ? 3600 * var1 : 86400 * var1;
      }
   }
}
