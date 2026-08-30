package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;

public class OrderCloseTypes {
   public static final byte Manual = 1;
   public static final byte SL = 2;
   public static final byte PT = 3;
   public static final byte EndTest = 4;
   public static final byte EOD = 5;
   public static final byte Expired = 6;
   public static final byte Reversed = 7;
   public static final byte Deleted = 8;
   public static final byte Replaced = 9;
   public static final byte OCA = 11;
   public static final byte Commission = 12;
   public static final byte EOD_TIME = 13;
   public static final byte EOF = 14;
   public static final byte EOF_TIME = 16;
   public static final byte EOR = 17;
   public static final byte NETTING_CONTROL_ORDER = 18;
   public static final byte ExitAfterXBars = 19;
   public static final byte MoveSL2BE = 20;
   public static final byte TrailingStop = 21;
   public static final byte ExitSignal = 22;
   public static final byte EOD_NEXT_OPEN = 55;
   public static final byte Delisted = 60;

   public static String toString(byte var0) {
      switch (var0) {
         case 1:
            return L.tsq("Manual");
         case 2:
            return L.tsq("SL");
         case 3:
            return L.tsq("PT");
         case 4:
            return L.tsq("EndTest");
         case 5:
            return L.tsq("End Of Day");
         case 6:
            return L.tsq("Expired");
         case 7:
            return L.tsq("Reversed");
         case 8:
            return L.tsq("Deleted");
         case 9:
            return L.tsq("Replaced");
         case 10:
         case 15:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 32:
         case 33:
         case 34:
         case 35:
         case 36:
         case 37:
         case 38:
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         case 48:
         case 49:
         case 50:
         case 51:
         case 52:
         case 53:
         case 54:
         case 56:
         case 57:
         case 58:
         case 59:
         default:
            return "Unknown";
         case 11:
            return L.tsq("OCA");
         case 12:
            return L.tsq("Commission");
         case 13:
            return L.tsq("End Of Day (Time)");
         case 14:
            return L.tsq("End Of Friday");
         case 16:
            return L.tsq("End Of Friday (Time)");
         case 17:
            return L.tsq("End Of Range");
         case 18:
            return L.tsq("Control order");
         case 19:
            return L.tsq("Exit After X Bars");
         case 20:
            return L.tsq("MoveSL2BE");
         case 21:
            return L.tsq("TrailingStop");
         case 22:
            return L.tsq("Exit Signal");
         case 55:
            return L.tsq("End Of Day (Next Market Open)");
         case 60:
            return L.tsq("Delisted");
      }
   }

   public static boolean wasRealized(byte var0) {
      return var0 != 6;
   }

   public static byte parseMT4(String var0) {
      switch (var0) {
         case "close":
            return 1;
         case "close at stop":
            return 4;
         case "t/p":
            return 3;
         case "s/l":
            return 2;
         case "delete":
            return 1;
         default:
            return 0;
      }
   }

   public static byte parseMT5(String var0) {
      if (var0.startsWith("end of test")) {
         return 4;
      } else if (var0.startsWith("sl")) {
         return 2;
      } else {
         return (byte)(var0.startsWith("tp") ? 3 : 1);
      }
   }
}
