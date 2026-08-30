package com.strategyquant.tradinglib.data;

import com.strategyquant.datalib.customData.CustomDataInfo;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDataIndyMap {
   private static final Logger Log = LoggerFactory.getLogger(CustomDataIndyMap.class);
   private Long2DoubleOpenHashMap map = new Long2DoubleOpenHashMap();
   private String indyId;
   private CustomDataInfo dataInfo;
   private int value;

   CustomDataIndyMap(String var1, CustomDataInfo var2, int var3) {
      this.indyId = var1;
      this.dataInfo = var2;
      this.value = var3;
   }

   public double getTime(long var1) {
      return this.map.getOrDefault(var1, 0.0);
   }

   public void put(long var1, double var3) {
      this.map.put(var1, var3);
   }

   public String getName() {
      return this.indyId;
   }

   public int getValueIndex() {
      return this.value;
   }
}
