package com.strategyquant.tradinglib.atm;

import com.strategyquant.tradinglib.task.settings.buildmode.JSONAble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ATMTypes extends JSONAble {
   public static final Logger Log = LoggerFactory.getLogger("ATMTypes");
   public static final int DONT_USE = 0;
   public static final int FROM_STRATEGY = 1;
   public static final int FROM_CONFIG = 2;
   public static final int GENERATE = 3;
   public static final int TYPE_FIXED_PIPS = 1;
   public static final int TYPE_ATR_BASED = 2;

   public static int recognize(String var0, int var1) {
      if (var0 != null && !var0.equals("")) {
         try {
            int var2 = Integer.parseInt(var0);
            switch (var2) {
               case 0:
               case 1:
               case 2:
               case 3:
                  return var2;
               default:
                  Log.error("Unrecognized atm type: {}. Using default value", var0);
            }
         } catch (Exception var3) {
            Log.info("Cannot convert value'{}' to int!", var0);
         }

         return var1;
      } else {
         return var1;
      }
   }
}
