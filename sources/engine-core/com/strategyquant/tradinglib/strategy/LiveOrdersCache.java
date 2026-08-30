package com.strategyquant.tradinglib.strategy;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.tradinglib.Trader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LiveOrdersCache {
   public static final Logger Log = LoggerFactory.getLogger("LiveOrdersCache");
   private LiveOrderObj[] cache = new LiveOrderObj[6];

   public LiveOrderObj createLiveOrder(Trader var1, InstrumentInfo var2, byte var3, String var4) {
      LiveOrderObj var5 = this.findInCache();
      if (var5 != null) {
         var5.init(var1, var2, var3, var4);
         return var5;
      } else {
         return new LiveOrderObj(var1, var2, var3, var4);
      }
   }

   public LiveOrderObj createLiveOrder(Trader var1, InstrumentInfo var2, byte var3, String var4, double var5) {
      LiveOrderObj var7 = this.findInCache();
      if (var7 != null) {
         var7.init(var1, var2, var3, var4, var5);
         return var7;
      } else {
         return new LiveOrderObj(var1, var2, var3, var4, var5);
      }
   }

   private LiveOrderObj findInCache() {
      for (int var1 = 0; var1 < this.cache.length; var1++) {
         if (this.cache[var1] != null) {
            LiveOrderObj var2 = this.cache[var1];
            this.cache[var1] = null;
            return var2;
         }
      }

      return null;
   }

   public void returnObject(LiveOrderObj var1) {
      if (var1 != null) {
         for (int var2 = 0; var2 < this.cache.length; var2++) {
            if (this.cache[var2] == null) {
               this.cache[var2] = var1;
               return;
            }
         }
      }
   }
}
