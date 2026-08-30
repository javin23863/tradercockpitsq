package com.strategyquant.tradinglib.correlation;

public class CorrelationPeriodTypes {
   public static final int HOUR = 5;
   public static final int DAY = 10;
   public static final int WEEK = 20;
   public static final int MONTH = 30;
   public static final int YEAR = 40;

   public static String toString(int var0) {
      switch (var0) {
         case 5:
            return "Hour";
         case 10:
            return "Day";
         case 20:
            return "Week";
         case 30:
            return "Month";
         case 40:
            return "Year";
         default:
            return "?";
      }
   }
}
