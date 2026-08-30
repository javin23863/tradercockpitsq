package com.strategyquant.tradinglib.talib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.app.MainApp;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class TALibIndicatorsCache {
   private Long2ObjectOpenHashMap<float[][]> cache = new Long2ObjectOpenHashMap(10);
   private Long2ObjectOpenHashMap<Object[]> hashToIdentMap = new Long2ObjectOpenHashMap();

   public boolean containsKey(long var1) {
      return this.cache.containsKey(var1);
   }

   public long getHash(String var1, String var2, SettingsMap var3) {
      int var4 = SQUtils.betterHashCode(var1);
      var4 += SQUtils.betterHashCode(var2);
      var4 += System.identityHashCode(var3);
      return var4;
   }

   public float[][] get(long var1) {
      return (float[][])this.cache.get(var1);
   }

   public void put(String var1, String var2, long var3, float[][] var5, int var6, int var7, String var8, SettingsMap var9) {
      this.cache.put(var3, var5);
      if (!MainApp.isBacktestNode()) {
         this.hashToIdentMap.put(var3, new Object[]{var1, var2, var6, var7, var8, var3, var9});
      }
   }

   public Long2ObjectOpenHashMap<Object[]> getIndicatorHashes() {
      return this.hashToIdentMap;
   }

   public void clear() {
      this.cache.clear();
      this.hashToIdentMap.clear();
   }
}
