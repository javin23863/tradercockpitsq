package com.strategyquant.datalib.darwinex;

import com.strategyquant.datalib.data.io.VersatileData;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DarwinexUtils {
   public static final Logger Log = LoggerFactory.getLogger(DarwinexUtils.class);

   public static void getTickData(File var0, Long2ObjectAVLTreeMap<VersatileData> var1, boolean var2) throws Exception {
      if (var0.exists()) {
         try {
            GZIPInputStream var3 = new GZIPInputStream(new FileInputStream(var0));
            BufferedReader var4 = new BufferedReader(new InputStreamReader(var3, StandardCharsets.UTF_8));
            new VersatileData();

            String var5;
            while ((var5 = var4.readLine()) != null) {
               String[] var6 = var5.split(",");
               if (var6.length == 3) {
                  String var12 = var6[0];

                  long var7;
                  try {
                     var7 = Long.valueOf(var12);
                  } catch (Exception var14) {
                     if (var12.length() > 13) {
                        var12 = var12.substring(0, 13);
                     }

                     var7 = Long.valueOf(var12);
                  }

                  double var9 = Double.parseDouble(var6[1]);
                  VersatileData var11 = (VersatileData)var1.get(var7);
                  if (var11 == null) {
                     var11 = new VersatileData();
                     var11.time = var7;
                     var1.put(var7, var11);
                  }

                  if (var2) {
                     var11.ask = var9;
                  } else {
                     var11.bid = var9;
                     var11.volume = Double.parseDouble(var6[2]);
                  }
               }
            }

            var3.close();
            var4.close();
         } catch (Exception var15) {
            Log.error("Failed to load data from file: " + var0.getAbsolutePath(), var15);
            throw new Exception("Failed to load data from file: " + var0.getAbsolutePath(), var15);
         }
      }
   }
}
