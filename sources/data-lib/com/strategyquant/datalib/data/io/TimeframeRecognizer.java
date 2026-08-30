package com.strategyquant.datalib.data.io;

import com.strategyquant.datalib.TimeframeManager;
import java.util.HashMap;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeframeRecognizer {
   public static final Logger Log = LoggerFactory.getLogger("TimeframeRecognizer");
   private long previousTime;
   private HashMap<Long, Integer> gapCounts = new HashMap<>();

   public void reset() {
      this.previousTime = 0L;
      this.gapCounts.clear();
   }

   public void processTime(long var1) {
      if (this.previousTime != 0L) {
         Long var3 = Math.abs(var1 - this.previousTime);
         if (var3 > 80000000L && var3 < 92000000L) {
            var3 = new Long(86400000L);
         }

         if (var3 > 160000000L && var3 < 184000000L) {
            var3 = new Long(172800000L);
         }

         if (this.gapCounts.containsKey(var3)) {
            this.gapCounts.put(var3, new Integer(this.gapCounts.get(var3) + 1));
         } else {
            this.gapCounts.put(var3, new Integer(1));
         }
      }

      this.previousTime = var1;
   }

   public String getTimeframe() {
      int var1 = (int)(this.getAverageGapBetweenLines() / 60000L);
      return TimeframeManager.getTFName(var1 * 60000);
   }

   private long getAverageGapBetweenLines() {
      if (this.gapCounts.size() == 0) {
         return -1L;
      }

      long var1 = 0L;
      long var3 = 0L;
      int var5 = 0;
      int var6 = 0;

      for (Entry var8 : this.gapCounts.entrySet()) {
         long var9 = (Long)var8.getKey();
         int var11 = (Integer)var8.getValue();
         var5++;
         var6 += var11;
         Log.debug("Gap: {} , Count: {}", var9, var11);
         if (var11 > var1) {
            var1 = var11;
            var3 = var9;
         }
      }

      Log.info("Total gaps: {}, most frequent gap: {} - in {} cases", new Object[]{var5, var3, var1});
      if (var1 > var5 / 2) {
         Log.info("Gap {} is in more than 50% of data, it is probably the one", var3);
         return var3;
      }

      if (var5 > 200) {
         return -60000L;
      }

      var6 /= 100;
      return var5 > 8 * var6 ? -60000L : var3;
   }
}
