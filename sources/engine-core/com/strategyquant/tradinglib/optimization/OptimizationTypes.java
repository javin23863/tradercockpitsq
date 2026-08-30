package com.strategyquant.tradinglib.optimization;

public class OptimizationTypes {
   public static final int Simple = 1;
   public static final int WalkForward = 2;
   public static final int WalkForwardMatrix = 3;
   public static final int Sequential = 4;

   public static String asString(int var0) {
      switch (var0) {
         case 1:
            return "Simple optimization";
         case 2:
            return "WalkForward optimization";
         case 3:
            return "WalkForwardMatrix (Cluster Analysis)";
         case 4:
            return "Sequential optimization";
         default:
            return "Unknown";
      }
   }
}
