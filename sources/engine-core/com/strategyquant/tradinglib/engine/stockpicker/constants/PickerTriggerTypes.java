package com.strategyquant.tradinglib.engine.stockpicker.constants;

import com.strategyquant.lib.L;

public class PickerTriggerTypes {
   public static final byte OnBarOpen = 5;
   public static final byte OnBarClose = 10;
   public static final byte BeforeBarOpen = 15;
   public static final byte[] AvailableOptions = new byte[]{5, 10, 15};

   public static String toString(byte var0) {
      switch (var0) {
         case 5:
            return L.tsq("On Bar Open");
         case 10:
            return L.tsq("On Bar Close");
         case 15:
            return L.tsq("Before Bar Open");
         default:
            return "Unknown";
      }
   }

   public static byte parse(String var0) {
      switch (var0) {
         case "BeforeBarOpen":
         case "AfterBarClose":
            return 15;
         case "OnBarOpen":
            return 5;
         case "OnBarClose":
            return 10;
         default:
            return -1;
      }
   }

   public static String getKey(byte var0) throws Exception {
      switch (var0) {
         case 5:
            return "OnBarOpen";
         case 10:
            return "OnBarClose";
         case 15:
            return "BeforeBarOpen";
         default:
            throw new Exception(L.t("Invalid trigger type %s", new Object[]{var0}));
      }
   }
}
