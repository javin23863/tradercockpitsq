package com.strategyquant.tradinglib;

public class Directions {
   public static final byte Both = 0;
   public static final byte Long = 1;
   public static final byte Short = -1;

   public static String typeToString(byte var0) {
      switch (var0) {
         case -1:
            return "Short";
         case 0:
            return "Both";
         case 1:
            return "Long";
         default:
            return "?";
      }
   }
}
