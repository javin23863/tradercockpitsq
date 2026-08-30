package com.strategyquant.datalib.bartype;

import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.bartype.impl.FuturesIntradayBar;
import com.strategyquant.datalib.bartype.impl.FuturesTimeBar;
import com.strategyquant.datalib.bartype.impl.FxIntradayBar;
import com.strategyquant.datalib.bartype.impl.FxTimeBar;
import com.strategyquant.datalib.data.DataException;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BarTypeFactory {
   public static final Logger Log = LoggerFactory.getLogger("BarTypeFactory");
   private static final String PREFIX_SEPARATOR = ":";
   private static BarTypeFactory instance = null;
   private ArrayList<BarType> registeredBarTypes = new ArrayList<>();
   private Long2ObjectOpenHashMap<BarType> createdBarTypes = new Long2ObjectOpenHashMap();

   private BarTypeFactory() {
      try {
         this.addBarType(new FxTimeBar("M1"));
         this.addBarType(new FuturesTimeBar("M1"));
         this.addBarType(new FxIntradayBar());
         this.addBarType(new FuturesIntradayBar());
      } catch (TimeframeNotSupportedException var2) {
         Log.error("Error initializing BarTypes", var2);
      }
   }

   private void addBarType(BarType var1) {
      this.registeredBarTypes.add(var1);
   }

   private static BarTypeFactory get() {
      if (instance == null) {
         instance = new BarTypeFactory();
      }

      return instance;
   }

   public static BarType getBarType(String var0, int var1) throws DataException {
      return get()._getBarType(var0, var1);
   }

   private BarType _getBarType(String var1, int var2) throws DataException {
      long var3 = this.getUniqueHash(var1, var2);
      if (!this.createdBarTypes.containsKey(var3)) {
         BarType var5 = null;

         for (BarType var7 : this.registeredBarTypes) {
            if (var7.getBarTimeType() == var2 && var7.checkTimeframeIsSupported(var1)) {
               var5 = var7;
               break;
            }
         }

         if (var5 == null) {
            throw new DataException(4, "No BarType was found for timeframe '" + var1 + "' and barTimeType '" + var2 + "' !");
         }

         BarType var8 = var5.clone(var1);
         this.createdBarTypes.put(var3, var8);
      }

      return (BarType)this.createdBarTypes.get(var3);
   }

   private long getUniqueHash(String var1, int var2) throws DataException {
      try {
         return TimeframeManager.getTFHash(var1) + var2;
      } catch (Exception var4) {
         throw new DataException(6, var4.getMessage());
      }
   }

   private static boolean isTimePeriod(String var0) {
      return true;
   }

   public static void checkTimeframeIsValid(String var0) throws DataException {
      if (var0 != null && !var0.isEmpty()) {
         var0 = var0.toUpperCase();
         if (!var0.equalsIgnoreCase("TICK")) {
            if (!var0.equalsIgnoreCase("Intraday")) {
               if (!var0.equalsIgnoreCase("Weekly")) {
                  if (!var0.equalsIgnoreCase("Monthly")) {
                     String var1 = var0.substring(0, 1);
                     if (!var1.equals("S") && !var1.equals("M") && !var1.equals("H") && !var1.equals("D")) {
                        throw new DataException(6, "Timeframe '" + var0 + "' is not supported!");
                     }

                     try {
                        String var2 = var0.substring(1, var0.length());
                        int var3 = Integer.parseInt(var2);
                     } catch (NumberFormatException var4) {
                        throw new DataException(6, "Timeframe '" + var0 + "' is not supported!");
                     }
                  }
               }
            }
         }
      } else {
         throw new DataException(6, "Timeframe wasn't recognized.");
      }
   }
}
