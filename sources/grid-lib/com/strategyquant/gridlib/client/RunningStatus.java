package com.strategyquant.gridlib.client;

public class RunningStatus {
   public static final int BeforeStart = 0;
   public static final int Running = 1;
   public static final int Paused = 2;
   public static final int Finished = 3;
   public static final int Stopped = 4;
   public static final int Pausing = 5;
   public static final int Stopping = 6;
   public static final int Error = 50;
   public static final int Loading = 100;

   public static String printStatus(int var0) {
      switch (var0) {
         case 0:
            return "BeforeStart";
         case 1:
            return "Running";
         case 2:
            return "Paused";
         case 3:
            return "Finished";
         case 4:
            return "Stopped";
         case 5:
            return "Pausing";
         case 6:
            return "Stopping";
         case 50:
            return "Error";
         case 100:
            return "Loading";
         default:
            return "? status: " + var0;
      }
   }

   public static boolean canBeStarted(int var0) {
      return var0 == 0 || var0 == 4 || var0 == 50 || var0 == 3;
   }
}
