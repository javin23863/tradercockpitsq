package com.strategyquant.tradinglib.indicator;

import com.strategyquant.tradinglib.ChartData;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorsCache {
   private static final Logger Log = LoggerFactory.getLogger(IndicatorsCache.class);
   private Long2ObjectOpenHashMap<IndicatorBase> cache = new Long2ObjectOpenHashMap(10);

   public boolean containsKey(long var1) {
      return this.cache.containsKey(var1);
   }

   public IndicatorBase put(long var1, IndicatorBase var3) {
      return (IndicatorBase)this.cache.put(var1, var3);
   }

   public IndicatorBase get(long var1) {
      return (IndicatorBase)this.cache.get(var1);
   }

   public void destroy() {
      ObjectIterator var1 = this.cache.values().iterator();

      while (var1.hasNext()) {
         IndicatorBase var2 = (IndicatorBase)var1.next();

         try {
            var2.Deinitialize();
         } catch (Exception var4) {
            Log.error("Error during Deinitialization of indicator", var4);
         }

         var2.destroy();
      }

      this.cache.clear();
   }

   public ArrayList<IndicatorBase> getIndicatorsForThisData(ChartData var1) {
      ArrayList var2 = new ArrayList();
      LongIterator var3 = this.cache.keySet().iterator();

      while (var3.hasNext()) {
         long var4 = (Long)var3.next();
         IndicatorBase var6 = (IndicatorBase)this.cache.get(var4);
         if (var6.checkUsesData(var1)) {
            var6.setName(var6.getPseudoName());
            var2.add(var6);
         }
      }

      return var2;
   }
}
