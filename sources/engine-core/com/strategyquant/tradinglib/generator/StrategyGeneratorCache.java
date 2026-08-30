package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.util.ArrayList;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyGeneratorCache {
   public static final Logger Log = LoggerFactory.getLogger("StrategyGeneratorCache");
   private static Int2ObjectOpenHashMap<ArrayList<StrategyGenerator>> mapGenerators = new Int2ObjectOpenHashMap();

   public static StrategyGenerator getGenerator(int var0, IRandomGenerator var1, Element var2, Element var3, SettingsMap var4, ATMGenerateConfig var5) throws JDOMException, IOException, Exception {
      synchronized (mapGenerators) {
         StrategyGenerator var7 = null;
         boolean var9 = false;
         ArrayList var8;
         if (mapGenerators.containsKey(var0)) {
            var8 = (ArrayList)mapGenerators.get(var0);
            int var10 = 0;

            for (int var11 = 0; var11 < var8.size(); var11++) {
               StrategyGenerator var12 = (StrategyGenerator)var8.get(var11);
               if (var12.isFree()) {
                  var7 = var12;
                  var12.setRandomGenerator(var1);
                  if (Log.isDebugEnabled()) {
                     Log.debug(String.format("Found existing StrategyGenerator with hash %d - %s", var0, var12.toString()));
                  }
                  break;
               }

               var10++;
            }
         } else {
            var8 = new ArrayList();
            var9 = true;
         }

         if (var7 == null) {
            var7 = new StrategyGenerator(var2, var3, var1, var4, var5);
            var8.add(var7);
            if (var9) {
               mapGenerators.put(var0, var8);
            }

            if (Log.isDebugEnabled()) {
               Log.debug(String.format("New StrategyGenerator created with hash: %d -%s", var0, var7.toString()));
            }
         }

         var7.setUsed();
         return var7;
      }
   }

   public static int getHash(Element var0, Element var1, SettingsMap var2, ATMGenerateConfig var3) {
      String var4 = XMLUtil.xmlToStringRaw(var0);
      String var5 = XMLUtil.xmlToStringRaw(var1);
      int var6 = var4.hashCode();
      int var7 = var5.hashCode();
      int var8 = var2.hashCode();
      int var9 = var6 + var7 + var8;
      if (var3 != null) {
         var9 += var3.getHash();
      }

      return var9;
   }

   public static void clearTooFullCache() {
      synchronized (mapGenerators) {
         if (mapGenerators.size() > 50) {
            mapGenerators.clear();
         }
      }
   }

   public static void clear() {
      synchronized (mapGenerators) {
         mapGenerators.clear();
      }
   }
}
