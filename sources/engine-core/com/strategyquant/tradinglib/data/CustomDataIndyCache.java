package com.strategyquant.tradinglib.data;

import com.strategyquant.datalib.customData.CustomDataBinReader;
import com.strategyquant.datalib.customData.CustomDataInfo;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomDataIndyCache {
   private static final Logger Log = LoggerFactory.getLogger(CustomDataIndyCache.class);
   private static Long2ObjectOpenHashMap<CustomDataIndyMap> cache = new Long2ObjectOpenHashMap();

   public static CustomDataIndyMap getMapForIndy(Element var0) {
      synchronized (cache) {
         String var2 = var0.getAttributeValue("key");
         if (var2 == null) {
            return null;
         }

         String var3 = CustomDataManager.getCDataIndyId(var2);
         if (var3 == null) {
            return null;
         }

         int var4 = 0;
         Element var5 = XMLUtil.findFirstWithKey(var0, "Param", "#Value#");
         if (var5 != null) {
            String var6 = var5.getText();
            if (var6 != null && !var6.equals("")) {
               var4 = Integer.parseInt(var6);
            }
         }

         long var10 = SQUtils.longHashCode(var3) + var4;
         return !cache.containsKey(var10) ? loadCustomDataIndy(var3, var4) : (CustomDataIndyMap)cache.get(var10);
      }
   }

   private static CustomDataIndyMap loadCustomDataIndy(String var0, int var1) {
      CustomDataInfo var2 = CustomDataManager.getDataInfo(var0);
      if (var2 == null) {
         Log.error("Cannot load data info for {}", var0);
         return null;
      }

      CustomDataIndyMap[] var3 = new CustomDataIndyMap[var2.values];

      for (int var4 = 0; var4 < var2.values; var4++) {
         var3[var4] = new CustomDataIndyMap(var0, var2, var4);
      }

      try {
         CustomDataBinReader var9 = new CustomDataBinReader(var2);
         var9.open();

         while (var9.hasNextData()) {
            var9.loadData();

            for (int var5 = 0; var5 < var2.values; var5++) {
               var3[var5].put(var9.loadedData.time, var9.loadedData.values[var5]);
            }
         }

         for (int var10 = 0; var10 < var2.values; var10++) {
            long var6 = SQUtils.longHashCode(var0) + var10;
            cache.put(var6, var3[var10]);
         }

         if (var1 >= 0 && var1 < var2.values) {
            long var11 = SQUtils.longHashCode(var0) + var1;
            return (CustomDataIndyMap)cache.get(var11);
         } else {
            Log.error("Custom Data Indicator {} has only {} values, index {} requested!", new Object[]{var0, var2.values, var1});
            return null;
         }
      } catch (Exception var8) {
         Log.error("Cannot load custom indy values for {}->Value{}", var0, var1);
         return null;
      }
   }
}
