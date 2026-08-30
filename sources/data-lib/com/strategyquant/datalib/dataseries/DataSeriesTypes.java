package com.strategyquant.datalib.dataseries;

public class DataSeriesTypes extends DataSeriesBase {
   public static final int Close = 0;
   public static final int Open = 1;
   public static final int High = 2;
   public static final int Low = 3;
   public static final int Median = 4;
   public static final int Typical = 5;
   public static final int Weighted = 6;
   public static final int Volume = 7;

   public static String toString(int var0) {
      switch (var0) {
         case 0:
            return "Close";
         case 1:
            return "Open";
         case 2:
            return "High";
         case 3:
            return "Low";
         case 4:
            return "Median";
         case 5:
            return "Typical";
         case 6:
            return "Weighted";
         case 7:
            return "Volume";
         default:
            return "Unknown";
      }
   }
}
