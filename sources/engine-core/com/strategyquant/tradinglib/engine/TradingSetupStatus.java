package com.strategyquant.tradinglib.engine;

public class TradingSetupStatus {
   public static final int Paused = 0;
   public static final int Running = 1;
   public static final int Stopped = 2;

   public static String toString(int var0) {
      switch (var0) {
         case 0:
            return "Paused";
         case 1:
            return "Running";
         case 2:
            return "Stopped";
         default:
            return "Undefined";
      }
   }
}
