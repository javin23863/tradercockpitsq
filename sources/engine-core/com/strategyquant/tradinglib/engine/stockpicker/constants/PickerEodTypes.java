package com.strategyquant.tradinglib.engine.stockpicker.constants;

public class PickerEodTypes {
   public static final byte DayClose = 5;
   public static final byte NotDefined = 10;
   public static final byte[] AvailableOptions = new byte[]{5, 10};

   public static String toString(byte var0) {
      switch (var0) {
         case 5:
            return "End of day";
         case 10:
            return "";
         default:
            return "Unknown";
      }
   }
}
