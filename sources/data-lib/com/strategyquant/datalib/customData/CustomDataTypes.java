package com.strategyquant.datalib.customData;

import java.util.Arrays;
import java.util.List;

public class CustomDataTypes {
   public static final byte IndicatorValuePrice = 1;
   public static final byte IndicatorValueNumber = 2;
   public static final byte IndicatorValuePriceRange = 3;
   public static final byte SignalValueBoolean = 10;
   public static final byte SignalValueAction = 11;
   public static final String IndicatorValuePriceKey = "IndicatorValuePrice";
   public static final String IndicatorValueNumberKey = "IndicatorValueNumber";
   public static final String IndicatorValuePriceRangeKey = "IndicatorValuePriceRange";
   public static final String SignalValueBooleanKey = "SignalValueBoolean";
   public static final String SignalValueActionKey = "SignalValueAction";
   public static final String IndicatorValuePriceName = "Indicator value - price";
   public static final String IndicatorValueNumberName = "Indicator value - number";
   public static final String IndicatorValuePriceRangeName = "Indicator value - price range";
   public static final String SignalValueBooleanName = "Signal - 0 means false, anything else means true";

   public static String toString(byte var0) {
      switch (var0) {
         case 1:
            return "Indicator value - price";
         case 2:
            return "Indicator value - number";
         case 3:
            return "Indicator value - price range";
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         default:
            return "CustomDataType" + var0;
         case 10:
            return "Signal - 0 means false, anything else means true";
      }
   }

   public static String translateDataType(byte var0) throws Exception {
      switch (var0) {
         case 1:
            return "price";
         case 2:
            return "number";
         case 3:
            return "pricerange";
         case 4:
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         default:
            throw new Exception("Cannot recognize custom data indoicartor type: " + var0);
         case 10:
            return "boolean";
      }
   }

   public static List<Byte> availableDataTypes() {
      return Arrays.asList((byte)1, (byte)2, (byte)3, (byte)10, (byte)11);
   }
}
